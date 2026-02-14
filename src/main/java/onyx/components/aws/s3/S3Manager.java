/*
 * Copyright (c) 2026 Mark S. Kolich
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

import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.util.http.ContentTypes;
import onyx.components.config.aws.AwsConfig;
import onyx.components.storage.AssetManager;
import onyx.components.storage.async.AsyncAssetThreadPool;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.HeadObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public final class S3Manager implements AssetManager {

    private static final Logger LOG = LoggerFactory.getLogger(S3Manager.class);

    private static final String DEFAULT_CONTENT_TYPE = MediaType.OCTET_STREAM.toString();

    private static final String X_AMZ_STORAGE_CLASS = "x-amz-storage-class";

    private static final String SLASH_STRING = "/";

    private final AwsConfig awsConfig_;

    private final S3Client s3_;
    private final S3Presigner presigner_;

    private final ExecutorService asyncAssetExecutorService_;

    @Injectable
    public S3Manager(
            final AwsConfig awsConfig,
            final OnyxS3Client onyxS3Client,
            final AsyncAssetThreadPool asyncAssetThreadPool) {
        awsConfig_ = awsConfig;
        s3_ = onyxS3Client.getS3Client();
        presigner_ = onyxS3Client.getPresigner();
        asyncAssetExecutorService_ = asyncAssetThreadPool.getExecutorService();
    }

    @Override
    public URL getPresignedInfoUrlForResource(
            final Resource resource) {
        final long linkValidityDurationInSeconds =
                awsConfig_.getAwsS3PresignedAssetUrlValidityDuration(TimeUnit.SECONDS);

        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String key = resource.getS3Key();

        final HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        final HeadObjectPresignRequest presignRequest = HeadObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(linkValidityDurationInSeconds))
                .headObjectRequest(headObjectRequest)
                .build();

        return presigner_.presignHeadObject(presignRequest).url();
    }

    @Override
    public URL getPresignedDownloadUrlForResource(
            final Resource resource) {
        final long linkValidityDurationInSeconds =
                awsConfig_.getAwsS3PresignedAssetUrlValidityDuration(TimeUnit.SECONDS);

        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String key = resource.getS3Key();

        final String name = resource.getName();
        final String extension = FilenameUtils.getExtension(key).toLowerCase();
        final String contentType = ContentTypes.getContentTypeForExtension(extension,
                DEFAULT_CONTENT_TYPE);

        final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .responseContentType(contentType)
                .responseContentDisposition(String.format("inline; filename=\"%s\"", name))
                .build();

        final GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(linkValidityDurationInSeconds))
                .getObjectRequest(getObjectRequest)
                .build();

        return presigner_.presignGetObject(presignRequest).url();
    }

    @Override
    public URL getPresignedUploadUrlForResource(
            final Resource resource) {
        final long linkValidityDurationInSeconds =
                awsConfig_.getAwsS3PresignedAssetUrlValidityDuration(TimeUnit.SECONDS);

        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String key = resource.getS3Key();

        final String defaultStorageClass = awsConfig_.getAwsS3DefaultStorageClass();

        // https://github.com/aws/aws-sdk-java-v2/issues/1849#issuecomment-642919219
        final AwsRequestOverrideConfiguration overrideConfig = AwsRequestOverrideConfiguration.builder()
                .putRawQueryParameter(X_AMZ_STORAGE_CLASS, defaultStorageClass)
                .build();

        final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .overrideConfiguration(overrideConfig)
                .build();

        final PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(linkValidityDurationInSeconds))
                .putObjectRequest(putObjectRequest)
                .build();

        return presigner_.presignPutObject(presignRequest).url();
    }

    @Override
    public long getResourceObjectSize(
            final Resource resource) {
        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String key = resource.getS3Key();

        try {
            final HeadObjectResponse headResponse = s3_.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());

            return headResponse.contentLength();
        } catch (final Exception e) {
            LOG.warn("Failed to load resource object size from S3 for key: {}",
                    key, e);
            return -1L;
        }
    }

    @Override
    public void deleteResource(
            final Resource resource,
            final boolean permanent) {
        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String key = resource.getS3Key();
        final Resource.Type resourceType = resource.getType();

        if (Resource.Type.FILE.equals(resourceType)) {
            deleteObject(bucketName, key, permanent);
        } else if (Resource.Type.DIRECTORY.equals(resourceType)) {
            // IMPORTANT: note the trailing slash on the key, which is to catch all "children"
            // of the directory (including the directory itself).
            final ListObjectsV2Response listResponse = s3_.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(key + SLASH_STRING)
                    .build());
            listResponse.contents().stream()
                    .map(S3Object::key)
                    .forEach(objKey -> deleteObject(bucketName, objKey, permanent));
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
            final List<ObjectVersion> versions = getAllVersionsForObject(bucketName, key);
            for (final ObjectVersion version : versions) {
                final DeleteObjectRequest dor = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(version.key())
                        .versionId(version.versionId())
                        .build();

                s3_.deleteObject(dor);
            }
        } else {
            // Standard deletion; if done in a versioning enabled bucket this operation
            // creates a delete marker in S3 and the object appears as it has been deleted.
            final DeleteObjectRequest dor = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3_.deleteObject(dor);
        }
    }

    /**
     * Fetches a complete list of all object versions for the given key,
     * iterating over the fetched object versions in batches as needed.
     */
    private List<ObjectVersion> getAllVersionsForObject(
            final String bucketName,
            final String key) {
        final ImmutableList.Builder<ObjectVersion> builder = ImmutableList.builder();

        ListObjectVersionsRequest request = ListObjectVersionsRequest.builder()
                .bucket(bucketName)
                .prefix(key)
                .maxKeys(5)
                .build();

        ListObjectVersionsResponse response = s3_.listObjectVersions(request);
        builder.addAll(response.versions());

        while (Boolean.TRUE.equals(response.isTruncated())) {
            request = ListObjectVersionsRequest.builder()
                    .bucket(bucketName)
                    .prefix(key)
                    .maxKeys(5)
                    .keyMarker(response.nextKeyMarker())
                    .versionIdMarker(response.nextVersionIdMarker())
                    .build();

            response = s3_.listObjectVersions(request);
            builder.addAll(response.versions());
        }

        return builder.build();
    }

}
