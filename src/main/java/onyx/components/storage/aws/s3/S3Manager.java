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

package onyx.components.storage.aws.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.dynamodbv2.datamodeling.S3Link;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.net.MediaType;
import curacao.CuracaoConfigLoader;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.OnyxConfig;
import onyx.components.storage.AssetManager;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public final class S3Manager implements AssetManager {

    private static final Logger LOG = LoggerFactory.getLogger(S3Manager.class);

    private static final String DEFAULT_CONTENT_TYPE = MediaType.OCTET_STREAM.toString();

    private static final String SLASH_STRING = "/";
    private static final String EMPTY_STRING = "";

    private final OnyxConfig onyxConfig_;

    private final AmazonS3 s3_;

    @Injectable
    public S3Manager(
            final OnyxConfig onyxConfig,
            final S3Client onyxS3Client) {
        onyxConfig_ = onyxConfig;
        s3_ = onyxS3Client.getS3Client();
    }

    @Override
    public URL getPresignedDownloadUrlForResource(
            final Resource resource) {
        return getPresignedUrlForResource(resource, HttpMethod.GET);
    }

    @Override
    public URL getPresignedUploadUrlForResource(
            final Resource resource) {
        return getPresignedUrlForResource(resource, HttpMethod.PUT);
    }

    @Override
    public URL getPresignedUrlForResource(
            final Resource resource,
            final HttpMethod httpMethod) {
        final long linkValidityDurationInSeconds =
                onyxConfig_.getAwsS3PresignedAssetUrlValidityDuration(TimeUnit.SECONDS);

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

        return s3_.generatePresignedUrl(presignedUrlRequest);
    }

    @Override
    public void deleteResource(
            final Resource resource) {
        if (Resource.Type.FILE.equals(resource.getType())) {
            final S3Link s3Link = resource.getS3Link();
            s3_.deleteObject(s3Link.getBucketName(), s3Link.getKey());
        } else if (Resource.Type.DIRECTORY.equals(resource.getType())) {
            final S3Link s3Link = resource.getS3Link();

            // IMPORTANT: note the trailing slash on the key, which is to catch all "children"
            // of the directory (including the directory itself).
            final ObjectListing objectList = s3_.listObjects(s3Link.getBucketName(),
                    s3Link.getKey() + SLASH_STRING);
            final List<S3ObjectSummary> objectsToDelete = objectList.getObjectSummaries();
            for (final S3ObjectSummary objectToDelete : objectsToDelete) {
                s3_.deleteObject(objectToDelete.getBucketName(), objectToDelete.getKey());
            }
        }
    }

    @Override
    public void deleteResourceAsync(
            final Resource resource,
            final ExecutorService executorService) {
        executorService.submit(() -> deleteResource(resource));
    }

}