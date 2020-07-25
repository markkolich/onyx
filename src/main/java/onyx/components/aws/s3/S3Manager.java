/*
 * Copyright (c) 2020 Mark S. Kolich
 * https://mark.koli.ch
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package onyx.components.aws.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.dynamodbv2.datamodeling.S3Link;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;
import curacao.CuracaoConfigLoader;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.aws.AwsConfig;
import onyx.components.storage.AssetManager;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public final class S3Manager implements AssetManager {

    private static final Logger LOG = LoggerFactory.getLogger(S3Manager.class);

    private static final String DEFAULT_CONTENT_TYPE = MediaType.OCTET_STREAM.toString();

    private static final String SLASH_STRING = "/";
    private static final String EMPTY_STRING = "";

    private final AwsConfig awsConfig_;

    private final AmazonS3 s3_;

    @Injectable
    public S3Manager(
            final AwsConfig awsConfig,
            final S3Client s3Client) {
        awsConfig_ = awsConfig;
        s3_ = s3Client.getS3Client();
    }

    @Override
    public URL getPresignedDownloadUrlForResource(
            final Resource resource) {
        return getPresignedUrlForResource(resource, HttpMethod.GET, null);
    }

    @Override
    public URL getPresignedUploadUrlForResource(
            final Resource resource) {
        final StorageClass defaultStorageClass = awsConfig_.getAwsS3DefaultStorageClass();
        final Map<String, String> requestParameters = ImmutableMap.of(
                Headers.STORAGE_CLASS, defaultStorageClass.toString());

        return getPresignedUrlForResource(resource, HttpMethod.PUT, requestParameters);
    }

    @Override
    public URL getPresignedUrlForResource(
            final Resource resource,
            final HttpMethod httpMethod,
            @Nullable final Map<String, String> requestParameters) {
        final long linkValidityDurationInSeconds =
                awsConfig_.getAwsS3PresignedAssetUrlValidityDuration(TimeUnit.SECONDS);

        final S3Link s3Link = resource.getS3Link();

        final String name = resource.getName();
        final String extension = FilenameUtils.getExtension(s3Link.getKey()).toLowerCase();
        final String contentType = CuracaoConfigLoader.getContentTypeForExtension(extension,
                DEFAULT_CONTENT_TYPE);

        final ResponseHeaderOverrides responseHeaderOverrides = new ResponseHeaderOverrides()
                .withContentType(contentType)
                .withContentDisposition(String.format("inline; filename=\"%s\"", name));

        final GeneratePresignedUrlRequest presignedUrlRequest =
                new GeneratePresignedUrlRequest(s3Link.getBucketName(), s3Link.getKey())
                .withExpiration(new Date(Instant.now().plusSeconds(linkValidityDurationInSeconds).toEpochMilli()))
                .withResponseHeaders(responseHeaderOverrides)
                .withMethod(httpMethod);

        // Attach custom request parameters, if any.
        if (MapUtils.isNotEmpty(requestParameters)) {
            for (final Map.Entry<String, String> parameter : requestParameters.entrySet()) {
                presignedUrlRequest.addRequestParameter(parameter.getKey(), parameter.getValue());
            }
        }

        return s3_.generatePresignedUrl(presignedUrlRequest);
    }

    @Override
    public void deleteResource(
            final Resource resource) {
        final Resource.Type resourceType = resource.getType();

        if (Resource.Type.FILE.equals(resourceType)) {
            final S3Link s3Link = resource.getS3Link();
            s3_.deleteObject(s3Link.getBucketName(), s3Link.getKey());
        } else if (Resource.Type.DIRECTORY.equals(resourceType)) {
            final S3Link s3Link = resource.getS3Link();

            // IMPORTANT: note the trailing slash on the key, which is to catch all "children"
            // of the directory (including the directory itself).
            final ObjectListing objectList = s3_.listObjects(s3Link.getBucketName(),
                    s3Link.getKey() + SLASH_STRING);
            final List<S3ObjectSummary> objectsToDelete = objectList.getObjectSummaries();
            final Set<String> keys = objectsToDelete.stream()
                    .map(S3ObjectSummary::getKey)
                    .collect(ImmutableSet.toImmutableSet());

            // Attempt to recursively delete all of the keys within the
            // directory in a single shot.
            final DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(s3Link.getBucketName())
                    .withKeys(keys.toArray(new String[0]))
                    .withQuiet(true);

            s3_.deleteObjects(deleteObjectsRequest);
        }
    }

    @Override
    public void deleteResourceAsync(
            final Resource resource,
            final ExecutorService executorService) {
        executorService.submit(() -> deleteResource(resource));
    }

}
