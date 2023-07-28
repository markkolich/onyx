/*
 * Copyright (c) 2023 Mark S. Kolich
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.util.http.ContentTypes;
import onyx.components.config.aws.AwsConfig;
import onyx.components.storage.AssetManager;
import onyx.components.storage.async.AsyncAssetThreadPool;
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

    private final ExecutorService asyncAssetExecutorService_;

    @Injectable
    public S3Manager(
            final AwsConfig awsConfig,
            final S3Client s3Client,
            final AsyncAssetThreadPool asyncAssetThreadPool) {
        awsConfig_ = awsConfig;
        s3_ = s3Client.getS3Client();
        asyncAssetExecutorService_ = asyncAssetThreadPool.getExecutorService();
    }

    @Override
    public URL getPresignedInfoUrlForResource(
            final Resource resource) {
        return getPresignedUrlForResource(resource, HttpMethod.HEAD, null);
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
        final String contentType = ContentTypes.getContentTypeForExtension(extension,
                DEFAULT_CONTENT_TYPE);

        final Date expiration =
                new Date(Instant.now().plusSeconds(linkValidityDurationInSeconds).toEpochMilli());

        final ResponseHeaderOverrides responseHeaderOverrides = new ResponseHeaderOverrides()
                .withContentType(contentType)
                .withContentDisposition(String.format("inline; filename=\"%s\"", name));

        final GeneratePresignedUrlRequest presignedUrlRequest =
                new GeneratePresignedUrlRequest(s3Link.getBucketName(), s3Link.getKey())
                .withExpiration(expiration)
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
    public long getResourceObjectSize(
            final Resource resource) {
        final S3Link s3Link = resource.getS3Link();

        try {
            final ObjectMetadata metadata =
                    s3_.getObjectMetadata(s3Link.getBucketName(), s3Link.getKey());
            if (metadata == null) {
                return -1L;
            }

            return metadata.getContentLength();
        } catch (final Exception e) {
            LOG.warn("Failed to load resource object size from S3 for key: {}",
                    s3Link.getKey(), e);
            return -1L;
        }
    }

    @Override
    public void deleteResource(
            final Resource resource,
            final boolean permanent) {
        final Resource.Type resourceType = resource.getType();

        if (Resource.Type.FILE.equals(resourceType)) {
            final S3Link s3Link = resource.getS3Link();

            deleteObject(s3Link.getBucketName(), s3Link.getKey(), permanent);
        } else if (Resource.Type.DIRECTORY.equals(resourceType)) {
            final S3Link s3Link = resource.getS3Link();

            // IMPORTANT: note the trailing slash on the key, which is to catch all "children"
            // of the directory (including the directory itself).
            final ObjectListing objectList = s3_.listObjects(s3Link.getBucketName(),
                    s3Link.getKey() + SLASH_STRING);
            final List<S3ObjectSummary> objectsToDelete = objectList.getObjectSummaries();
            objectsToDelete.stream()
                    .map(S3ObjectSummary::getKey)
                    .forEach(key -> deleteObject(s3Link.getBucketName(), key, permanent));
        }
    }

    @Override
    public void deleteResourceAsync(
            final Resource resource,
            final boolean permanent) {
        asyncAssetExecutorService_.submit(() -> deleteResource(resource, permanent));
    }

    private void deleteObject(
            final String bucketName,
            final String key,
            final boolean permanent) {
        final boolean versioningEnabled = awsConfig_.getAwsS3VersioningEnabled();

        if (versioningEnabled && permanent) {
            // Permanent deletion of an object in a versioning enabled S3 bucket requires us to
            // fetch each version of the object at the given key and then explicitly delete each version.
            // This is the only way to permanently delete objects in a versioning enabled S3 bucket.
            final List<S3VersionSummary> versions = getAllVersionSummariesForObject(bucketName, key);
            for (final S3VersionSummary version : versions) {
                final DeleteVersionRequest dvr = new DeleteVersionRequest(
                        bucketName,
                        version.getKey(),
                        version.getVersionId());

                s3_.deleteVersion(dvr);
            }
        } else {
            // Standard deletion; if done in a versioning enabled bucket this operation
            // creates a delete marker in S3 and the object appears as it has been deleted.
            s3_.deleteObject(bucketName, key);
        }
    }

    /**
     * Fetches a complete list of all object version summaries for the given key,
     * iterating over the fetched object versions in batches as needed.
     */
    private List<S3VersionSummary> getAllVersionSummariesForObject(
            final String bucketName,
            final String key) {
        final ImmutableList.Builder<S3VersionSummary> builder = ImmutableList.builder();

        final ListVersionsRequest lvr = new ListVersionsRequest();
        lvr.setBucketName(bucketName);
        lvr.setPrefix(key);
        lvr.setMaxResults(5);

        VersionListing verList = s3_.listVersions(lvr);
        for (boolean truncated = true; truncated; verList = s3_.listNextBatchOfVersions(verList)) {
            for (final S3VersionSummary summary : verList.getVersionSummaries()) {
                builder.add(summary);
            }
            truncated = verList.isTruncated();
        }

        return builder.build();
    }

}
