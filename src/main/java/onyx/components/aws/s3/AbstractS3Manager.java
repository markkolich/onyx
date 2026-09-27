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
import curacao.util.http.ContentTypes;
import onyx.components.config.aws.AwsConfig;
import org.apache.commons.io.FilenameUtils;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Shared base for S3-backed manager implementations in this package.
 */
public abstract class AbstractS3Manager {

    protected static final String DEFAULT_CONTENT_TYPE = MediaType.OCTET_STREAM.toString();

    // https://github.com/aws/aws-sdk-java-v2/issues/1849#issuecomment-642919219
    protected static final String X_AMZ_STORAGE_CLASS = "x-amz-storage-class";

    protected final AwsConfig awsConfig_;

    protected final S3Client s3_;
    protected final S3Presigner presigner_;

    protected AbstractS3Manager(
            final AwsConfig awsConfig,
            final OnyxS3Client onyxS3Client) {
        awsConfig_ = awsConfig;
        s3_ = onyxS3Client.getS3Client();
        presigner_ = onyxS3Client.getS3Presigner();
    }

    /**
     * Deletes the object at the given key without touching version history. On a
     * versioning-enabled bucket this only adds a delete marker; it does not remove
     * any existing version or free its storage.
     */
    protected final void deleteObject(
            final String bucketName,
            final String key) {
        s3_.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }

    /**
     * Permanently deletes every version of the object at the given key, freeing all
     * of its storage. Only meaningful on a versioning-enabled bucket; callers are
     * responsible for checking {@code AwsConfig.getAwsS3VersioningEnabled()} first.
     */
    protected final void deleteAllObjectVersions(
            final String bucketName,
            final String key) {
        final List<ObjectVersion> versions = listAllObjectVersions(bucketName, key);
        for (final ObjectVersion version : versions) {
            s3_.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(version.key())
                    .versionId(version.versionId())
                    .build());
        }
    }

    /**
     * Fetches a complete list of all object versions for the given key, paginating
     * as needed.
     */
    protected final List<ObjectVersion> listAllObjectVersions(
            final String bucketName,
            final String key) {
        final ImmutableList.Builder<ObjectVersion> versionsBuilder = ImmutableList.builder();

        ListObjectVersionsRequest request = ListObjectVersionsRequest.builder()
                .bucket(bucketName)
                .prefix(key)
                .build();

        ListObjectVersionsResponse response = s3_.listObjectVersions(request);
        versionsBuilder.addAll(response.versions());

        while (Boolean.TRUE.equals(response.isTruncated())) {
            request = ListObjectVersionsRequest.builder()
                    .bucket(bucketName)
                    .prefix(key)
                    .keyMarker(response.nextKeyMarker())
                    .versionIdMarker(response.nextVersionIdMarker())
                    .build();

            response = s3_.listObjectVersions(request);
            versionsBuilder.addAll(response.versions());
        }

        return versionsBuilder.build();
    }

    /**
     * Presigns a GET URL for the given key, with an inline {@code Content-Disposition}
     * carrying the given display name and a {@code Content-Type} resolved from the key's
     * file extension (falling back to {@link #DEFAULT_CONTENT_TYPE}) - so a browser
     * renders the object directly rather than prompting a raw-bytes download. Shared by
     * both the primary asset download URL and the metadata download URL, which differ
     * only in which key/name they resolve beforehand.
     */
    protected final URL presignDownloadUrl(
            final String bucketName,
            final String key,
            final String name) {
        final String extension = FilenameUtils.getExtension(key).toLowerCase();
        final String contentType = ContentTypes.getContentTypeForExtension(extension,
                DEFAULT_CONTENT_TYPE);

        final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .responseContentType(contentType)
                .responseContentDisposition(String.format("inline; filename=\"%s\"", name))
                .build();

        final long linkValidityDurationInSeconds =
                awsConfig_.getAwsS3PresignedAssetUrlValidityDuration(TimeUnit.SECONDS);

        final GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(linkValidityDurationInSeconds))
                .getObjectRequest(getObjectRequest)
                .build();

        return presigner_.presignGetObject(presignRequest).url();
    }

    /**
     * Presigns a PUT URL for the given key, tagged with the app's default storage class
     * (e.g., Intelligent-Tiering) via a raw {@code x-amz-storage-class} query parameter
     * override - {@code PutObjectRequest.storageClass()} alone is not honored on a
     * presigned URL, see the link on {@link #X_AMZ_STORAGE_CLASS}. Shared by both the
     * primary asset upload URL and the metadata upload URL, which differ only in which
     * key they resolve beforehand.
     */
    protected final URL presignUploadUrl(
            final String bucketName,
            final String key) {
        final String defaultStorageClass = awsConfig_.getAwsS3DefaultStorageClass();

        final AwsRequestOverrideConfiguration overrideConfig = AwsRequestOverrideConfiguration.builder()
                .putRawQueryParameter(X_AMZ_STORAGE_CLASS, defaultStorageClass)
                .build();

        final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .overrideConfiguration(overrideConfig)
                .build();

        final long linkValidityDurationInSeconds =
                awsConfig_.getAwsS3PresignedAssetUrlValidityDuration(TimeUnit.SECONDS);

        final PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(linkValidityDurationInSeconds))
                .putObjectRequest(putObjectRequest)
                .build();

        return presigner_.presignPutObject(presignRequest).url();
    }

}
