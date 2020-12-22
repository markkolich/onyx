/*
 * Copyright (c) 2021 Mark S. Kolich
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

package onyx.entities.storage.cache;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonDeserialize(builder = CachedResourceToken.Builder.class)
public interface CachedResourceToken {

    @JsonProperty("path")
    String getPath();

    @JsonProperty("expiry")
    Instant getExpiry();

    @JsonIgnore
    default Builder toBuilder() {
        return new Builder()
                .setPath(getPath())
                .setExpiry(getExpiry());
    }

    final class Builder {

        private String path_;

        private Instant expiry_;

        @JsonProperty("path")
        public Builder setPath(
                final String path) {
            path_ = path;
            return this;
        }

        @JsonProperty("expiry")
        public Builder setExpiry(
                final Instant expiry) {
            expiry_ = expiry;
            return this;
        }

        public CachedResourceToken build() {
            checkNotNull(path_, "Cached resource path cannot be null.");
            checkNotNull(expiry_, "Cached resource expiry cannot be null.");

            return new CachedResourceToken() {
                @Override
                public String getPath() {
                    return path_;
                }

                @Override
                public Instant getExpiry() {
                    return expiry_;
                }
            };
        }

    }

}
