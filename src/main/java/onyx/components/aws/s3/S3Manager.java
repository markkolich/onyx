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
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.aws.AwsConfig;
import onyx.components.storage.AssetManager;
import onyx.components.storage.async.AsyncAssetThreadPool;
import onyx.entities.api.request.v1.CompleteMultipartUploadRequest;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.model.HeadObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static onyx.util.PathUtils.SLASH_STRING;

@Component
public final class S3Manager extends AbstractS3Manager implements AssetManager {

    private static final Logger LOG = LoggerFactory.getLogger(S3Manager.class);

    private final ExecutorService asyncAssetExecutorService_;

    @Injectable
    public S3Manager(
            final AwsConfig awsConfig,
            final OnyxS3Client onyxS3Client,
            final AsyncAssetThreadPool asyncAssetThreadPool) {
        super(awsConfig, onyxS3Client);
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
        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String key = resource.getS3Key();
        final String name = resource.getName();

        return presignDownloadUrl(bucketName, key, name);
    }

    @Override
    public URL getPresignedUploadUrlForResource(
            final Resource resource) {
        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String key = resource.getS3Key();

        return presignUploadUrl(bucketName, key);
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

    @Override
    public String initiateMultipartUpload(
            final Resource resource) {
        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String key = resource.getS3Key();
        final String defaultStorageClass = awsConfig_.getAwsS3DefaultStorageClass();

        final CreateMultipartUploadRequest req = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .storageClass(defaultStorageClass)
                .build();

        return s3_.createMultipartUpload(req).uploadId();
    }

    @Override
    public URL getPresignedUploadUrlForPart(
            final Resource resource,
            final String uploadId,
            final int partNumber,
            final long partSize) {
        final long linkValidityDurationInSeconds =
                awsConfig_.getAwsS3PresignedAssetUrlValidityDuration(TimeUnit.SECONDS);

        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String key = resource.getS3Key();

        final String defaultStorageClass = awsConfig_.getAwsS3DefaultStorageClass();

        // https://github.com/aws/aws-sdk-java-v2/issues/1849#issuecomment-642919219
        final AwsRequestOverrideConfiguration overrideConfig = AwsRequestOverrideConfiguration.builder()
                .putRawQueryParameter(X_AMZ_STORAGE_CLASS, defaultStorageClass)
                .build();

        final UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .contentLength(partSize)
                .overrideConfiguration(overrideConfig)
                .build();

        final UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(linkValidityDurationInSeconds))
                .uploadPartRequest(uploadPartRequest)
                .build();

        return presigner_.presignUploadPart(presignRequest).url();
    }

    @Override
    public void completeMultipartUpload(
            final Resource resource,
            final String uploadId,
            final List<CompleteMultipartUploadRequest.Part> parts) {
        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String key = resource.getS3Key();

        final ImmutableList.Builder<CompletedPart> completedPartsBuilder = ImmutableList.builder();
        for (final CompleteMultipartUploadRequest.Part p : parts) {
            final CompletedPart completedPart = CompletedPart.builder()
                    .partNumber(p.getPartNumber())
                    .eTag(p.getETag())
                    .build();
            completedPartsBuilder.add(completedPart);
        }

        final List<CompletedPart> completedParts = completedPartsBuilder.build();

        final CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                .parts(completedParts)
                .build();

        final software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest completeRequest =
                software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(completedMultipartUpload)
                .build();

        s3_.completeMultipartUpload(completeRequest);
    }

    @Override
    public void abortMultipartUpload(
            final Resource resource,
            final String uploadId) {
        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String key = resource.getS3Key();

        final AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .build();

        s3_.abortMultipartUpload(abortRequest);
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
            deleteAllObjectVersions(bucketName, key);
        } else {
            // Standard deletion; if done in a versioning enabled bucket this operation
            // creates a delete marker in S3 and the object appears as it has been deleted.
            deleteObject(bucketName, key);
        }
    }

}
