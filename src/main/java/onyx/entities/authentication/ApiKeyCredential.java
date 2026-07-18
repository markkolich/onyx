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

package onyx.entities.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a single API key credential as persisted in S3, keyed by a hash of the
 * raw API key value. Deliberately does not carry the raw API key itself anywhere in
 * this object; the raw key is only ever available to the caller at the moment it is
 * generated. The {@link #getKeyHash()} value is redundant with the S3 object key this
 * credential is stored under (which is itself the same hash), but carrying it in the
 * body too makes the object self-describing for debugging without needing to
 * cross-reference the S3 key path.
 */
@JsonDeserialize(builder = ApiKeyCredential.Builder.class)
public interface ApiKeyCredential {

    @JsonProperty("keyHash")
    String getKeyHash();

    @JsonProperty("username")
    String getUsername();

    @JsonProperty("createdAt")
    Instant getCreatedAt();

    @JsonProperty("description")
    String getDescription();

    final class Builder {

        private String keyHash_;
        private String username_;
        private Instant createdAt_;
        private String description_;

        @JsonProperty("keyHash")
        public Builder setKeyHash(
                final String keyHash) {
            keyHash_ = keyHash;
            return this;
        }

        @JsonProperty("username")
        public Builder setUsername(
                final String username) {
            username_ = username;
            return this;
        }

        @JsonProperty("createdAt")
        public Builder setCreatedAt(
                final Instant createdAt) {
            createdAt_ = createdAt;
            return this;
        }

        @JsonProperty("description")
        public Builder setDescription(
                final String description) {
            description_ = description;
            return this;
        }

        public ApiKeyCredential build() {
            checkNotNull(keyHash_, "Key hash cannot be null.");
            checkNotNull(username_, "Username cannot be null.");
            checkNotNull(createdAt_, "Created at instant cannot be null.");

            return new ApiKeyCredential() {
                @Override
                public String getKeyHash() {
                    return keyHash_;
                }

                @Override
                public String getUsername() {
                    return username_;
                }

                @Override
                public Instant getCreatedAt() {
                    return createdAt_;
                }

                @Override
                public String getDescription() {
                    return description_;
                }
            };
        }

    }

}
