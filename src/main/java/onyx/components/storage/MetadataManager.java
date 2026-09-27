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

package onyx.components.storage;

import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.entities.storage.metadata.ResourceMetadata;

import javax.annotation.Nullable;
import java.net.URL;
import java.util.List;

/**
 * Resolves derived, generic metadata blobs stored in S3 alongside a resource's
 * primary asset, under {@code .onyx/metadata/<resource-key>/<type>/}. The "type"
 * is a caller-supplied sub-path (e.g., "screencaps") identifying which kind of
 * derived metadata is being requested; new kinds can be added later without
 * any change to this contract.
 */
public interface MetadataManager {

    String ONYX_METADATA_PATH_PREFIX = AssetManager.DOT_ONYX_PATH_PREFIX + "/metadata";

    /**
     * Lists the keys of every metadata blob under the given resource's metadata root,
     * optionally scoped to one type/kind. If type is null or blank, everything under the
     * resource's metadata root is listed, across every kind. If given, the listing is
     * scoped to just that type's prefix (e.g., "screencaps"). Returns an empty list if
     * nothing matches (e.g., not yet processed). Deliberately does not presign anything -
     * presigned URLs expire, so generating one for every listed item up front is wasteful
     * (and may go stale) when only a fraction will ever actually be fetched; see
     * {@link #getMetadataDownloadUrlForResource(Resource, String)}. Results are sorted
     * oldest-to-newest by S3's last-modified time, so a caller that wants "the latest" of a
     * kind can just take the last element - no client-side sorting needed, and no reliance
     * on key naming order to signal recency.
     */
    List<ResourceMetadata> listMetadataForResource(
            Resource resource,
            @Nullable String type);

    /**
     * Returns a presigned URL for the metadata blob at the given exact key (as returned by
     * {@link #listMetadataForResource(Resource, String)}), generated fresh at call time so
     * it's never stale by the time the caller uses it. Returns null if no object exists at
     * that key.
     */
    @Nullable
    URL getMetadataDownloadUrlForResource(
            Resource resource,
            String key);

    /**
     * Returns a presigned URL that can be used to upload a metadata blob for the given
     * resource, at the given exact key relative to the resource's metadata root (e.g.,
     * "screencaps/sheet_10x10_340x191_97.jpg") - not a prefix to discover, the caller already
     * knows the exact name of what it's about to write.
     */
    URL getMetadataUploadUrlForResource(
            Resource resource,
            String key);

    /**
     * Deletes the metadata blob at the given exact key (as returned by
     * {@link #listMetadataForResource(Resource, String)}) for the given resource.
     * Deliberately requires the exact key rather than a type/prefix - deletion is
     * permanent, so a caller has to be specific about what it's removing rather than
     * being able to wipe out an entire kind in one call. No-op if nothing exists at
     * that key.
     */
    void deleteMetadataForResource(
            Resource resource,
            String key);

    /**
     * Async variant of {@link #deleteMetadataForResource(Resource, String)}.
     */
    void deleteMetadataForResourceAsync(
            Resource resource,
            String key);

    /**
     * Permanently deletes every metadata object under the given resource's metadata root,
     * across every kind. Unlike {@link #deleteMetadataForResource(Resource, String)}, which
     * requires an exact key so a caller has to be deliberate about removing one object at a
     * time, this sweeps everything - appropriate when the resource itself is being deleted
     * entirely (or, for a directory resource, when any of its descendants are), not when a
     * caller is clearing out one kind on purpose. No-op if nothing exists under the root.
     */
    void deleteAllMetadataForResource(
            Resource resource);

    /**
     * Async variant of {@link #deleteAllMetadataForResource(Resource)}.
     */
    void deleteAllMetadataForResourceAsync(
            Resource resource);

}
