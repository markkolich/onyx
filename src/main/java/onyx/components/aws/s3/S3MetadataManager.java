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
import onyx.components.storage.MetadataManager;
import onyx.components.storage.async.AsyncMetadataThreadPool;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.entities.storage.metadata.ResourceMetadata;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.Nullable;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;
import static onyx.util.PathUtils.SLASH_STRING;

@Component
public final class S3MetadataManager extends AbstractS3Manager implements MetadataManager {

    private final ExecutorService asyncMetadataExecutorService_;

    @Injectable
    public S3MetadataManager(
            final AwsConfig awsConfig,
            final OnyxS3Client onyxS3Client,
            final AsyncMetadataThreadPool asyncMetadataThreadPool) {
        super(awsConfig, onyxS3Client);
        asyncMetadataExecutorService_ = asyncMetadataThreadPool.getExecutorService();
    }

    @Override
    public List<ResourceMetadata> listMetadataForResource(
            final Resource resource,
            @Nullable final String type) {
        checkNotNull(resource, "Resource cannot be null.");

        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String resourceMetadataRoot = buildResourceMetadataRoot(resource);
        final String prefix = StringUtils.isBlank(type) ?
                resourceMetadataRoot : buildMetadataPrefix(resourceMetadataRoot, type);

        final ImmutableList.Builder<ResourceMetadata> resultBuilder = ImmutableList.builder();

        // Sorted oldest-to-newest by S3's own last-modified time, so a caller that wants "the
        // latest" (e.g. the screencaps hover previewer) can just take the last element - no
        // client-side sorting needed, and no reliance on key naming order, which isn't a
        // reliable proxy for recency (a lexicographic sort of "v2"/"v10" gets it backwards).
        final List<S3Object> objects = listMetadataObjects(bucketName, prefix).stream()
                .sorted(Comparator.comparing(S3Object::lastModified))
                .collect(ImmutableList.toImmutableList());
        for (final S3Object object : objects) {
            // Return the key relative to the resource's metadata root rather than the full absolute
            // S3 key - callers already know the resource they asked about, so the
            // ".onyx/metadata/<resource-key>/" portion is internal storage layout, not
            // something worth exposing.
            final String relativeKey = Strings.CS.removeStart(object.key(), resourceMetadataRoot);

            resultBuilder.add(new ResourceMetadata.Builder()
                    .setKey(relativeKey)
                    .build());
        }

        return resultBuilder.build();
    }

    @Nullable
    @Override
    public URL getMetadataDownloadUrlForResource(
            final Resource resource,
            final String key) {
        checkNotNull(resource, "Resource cannot be null.");
        checkNotNull(key, "Key cannot be null.");

        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String absoluteKey = buildMetadataKey(buildResourceMetadataRoot(resource), key);

        try {
            s3_.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(absoluteKey)
                    .build());
        } catch (final NoSuchKeyException e) {
            return null;
        }

        final String name = FilenameUtils.getName(key);

        return presignDownloadUrl(bucketName, absoluteKey, name);
    }

    @Override
    public URL getMetadataUploadUrlForResource(
            final Resource resource,
            final String key) {
        checkNotNull(resource, "Resource cannot be null.");
        checkNotNull(key, "Key cannot be null.");

        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String absoluteKey = buildMetadataKey(buildResourceMetadataRoot(resource), key);

        return presignUploadUrl(bucketName, absoluteKey);
    }

    @Override
    public void deleteMetadataForResource(
            final Resource resource,
            final String key) {
        checkNotNull(resource, "Resource cannot be null.");
        checkNotNull(key, "Key cannot be null.");

        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String absoluteKey = buildMetadataKey(buildResourceMetadataRoot(resource), key);

        deleteMetadataObjectPermanently(bucketName, absoluteKey);
    }

    @Override
    public void deleteMetadataForResourceAsync(
            final Resource resource,
            final String key) {
        asyncMetadataExecutorService_.submit(() -> deleteMetadataForResource(resource, key));
    }

    @Override
    public void deleteAllMetadataForResource(
            final Resource resource) {
        checkNotNull(resource, "Resource cannot be null.");

        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String resourceMetadataRoot = buildResourceMetadataRoot(resource);

        // Works identically for a FILE or a DIRECTORY resource - a directory's own key is
        // already a prefix of every descendant file's key, so this same root also catches
        // every descendant's metadata.
        final List<S3Object> objects = listMetadataObjects(bucketName, resourceMetadataRoot);
        for (final S3Object object : objects) {
            deleteMetadataObjectPermanently(bucketName, object.key());
        }
    }

    @Override
    public void deleteAllMetadataForResourceAsync(
            final Resource resource) {
        asyncMetadataExecutorService_.submit(() -> deleteAllMetadataForResource(resource));
    }

    // Metadata blobs are disposable, regenerable derived data - not precious user content -
    // so unlike the primary asset delete flow there is no "soft delete" option here: deletion
    // always permanently removes every version on a versioning-enabled bucket, rather than
    // leaving a recoverable delete marker.
    private void deleteMetadataObjectPermanently(
            final String bucketName,
            final String key) {
        if (awsConfig_.getAwsS3VersioningEnabled()) {
            deleteAllObjectVersions(bucketName, key);
        } else {
            deleteObject(bucketName, key);
        }
    }

    /**
     * Lists every object under the given prefix, unbounded (S3's default page size of up to
     * 1000 keys per call is far beyond what a single resource's metadata directory should
     * ever hold, so pagination is intentionally not handled here).
     */
    private List<S3Object> listMetadataObjects(
            final String bucketName,
            final String prefix) {
        final ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        return s3_.listObjectsV2(listRequest).contents();
    }

    /**
     * Builds the S3 key prefix under which all metadata for the given resource lives,
     * regardless of type: {@code .onyx/metadata/<resource-key>/}.
     */
    private static String buildResourceMetadataRoot(
            final Resource resource) {
        return String.format("%s/%s/",
                MetadataManager.ONYX_METADATA_PATH_PREFIX,
                resource.getS3Key());
    }

    /**
     * Builds the S3 prefix under which metadata of the given type lives, given a
     * resource's metadata root: {@code <resourceMetadataRoot><type>/}. Leading/trailing
     * slashes on the caller-supplied type are normalized so callers can pass "screencaps",
     * "screencaps/", or "/screencaps" interchangeably, and a trailing slash is always appended
     * so this prefix can never accidentally match a differently-named sibling type
     * (e.g., "screencaps" vs. "screencaps-extra").
     */
    private static String buildMetadataPrefix(
            final String resourceMetadataRoot,
            final String type) {
        final String normalizedType = StringUtils.strip(type, SLASH_STRING);
        return String.format("%s%s/",
                resourceMetadataRoot,
                normalizedType);
    }

    /**
     * Builds the exact S3 key given a resource's metadata root and a caller-supplied
     * relative key: {@code <resourceMetadataRoot><key>} (e.g.,
     * "screencaps/sheet_10x10_340x191_97.jpg"). Used wherever the caller already knows the
     * exact object it's addressing - uploading, fetching, or deleting one specific
     * blob - as opposed to {@link #buildMetadataPrefix(String, String)}, which is for
     * discovering matches under a kind.
     */
    private static String buildMetadataKey(
            final String resourceMetadataRoot,
            final String key) {
        final String normalizedKey = StringUtils.strip(key, SLASH_STRING);
        return String.format("%s%s",
                resourceMetadataRoot,
                normalizedKey);
    }

}
