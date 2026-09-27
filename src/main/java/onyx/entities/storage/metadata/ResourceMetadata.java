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

package onyx.entities.storage.metadata;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a single derived metadata object discovered in S3 under a resource's
 * {@code .onyx/metadata/<resource-key>/<type>/} prefix (e.g., a generated video
 * frame sprite sheet). Intentionally carries only the key, not a presigned URL -
 * presigned URLs expire, so one is only generated on demand for a specific key,
 * at the moment a caller actually wants to fetch it. Also deliberately doesn't carry
 * S3's last-modified time - that's used internally by {@code S3MetadataManager} to sort
 * results before this entity is ever built, not something a caller needs to see.
 */
public interface ResourceMetadata {

    String getKey();

    final class Builder {

        private String key_;

        public Builder setKey(
                final String key) {
            key_ = key;
            return this;
        }

        public ResourceMetadata build() {
            checkNotNull(key_, "Metadata key cannot be null.");

            return new ResourceMetadata() {
                @Override
                public String getKey() {
                    return key_;
                }
            };
        }

    }

}
