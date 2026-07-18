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

package onyx.entities.api.response.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import onyx.entities.api.response.OnyxApiResponseEntity;
import onyx.entities.authentication.ApiKeyCredential;

import javax.annotation.Nullable;
import java.time.Instant;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A single API key summary, as returned by the list-keys endpoint. Deliberately does
 * not, and cannot, carry the raw key value &mdash; that is only ever available once,
 * in {@link CreateApiKeyResponse}, at the moment the key is created.
 */
public interface ApiKeyResponse extends OnyxApiResponseEntity {

    @JsonProperty("keyHash")
    String getKeyHash();

    @Nullable
    @JsonProperty("description")
    String getDescription();

    @JsonProperty("createdAt")
    Instant getCreatedAt();

    final class Builder extends AbstractOnyxApiResponseEntityBuilder {

        private String keyHash_;
        private String description_;
        private Instant createdAt_;

        public Builder(
                final ObjectMapper objectMapper) {
            super(objectMapper);
        }

        public Builder setKeyHash(
                final String keyHash) {
            keyHash_ = keyHash;
            return this;
        }

        public Builder setDescription(
                final String description) {
            description_ = description;
            return this;
        }

        public Builder setCreatedAt(
                final Instant createdAt) {
            createdAt_ = createdAt;
            return this;
        }

        public ApiKeyResponse build() {
            checkNotNull(keyHash_, "Key hash cannot be null.");
            checkNotNull(createdAt_, "Created at instant cannot be null.");

            return new ApiKeyResponse() {
                @Override
                public String getKeyHash() {
                    return keyHash_;
                }

                @Override
                public String getDescription() {
                    return description_;
                }

                @Override
                public Instant getCreatedAt() {
                    return createdAt_;
                }

                @Override
                public ObjectMapper getMapper() {
                    return objectMapper_;
                }
            };
        }

        public static Builder fromCredential(
                final ObjectMapper objectMapper,
                final ApiKeyCredential credential) {
            checkNotNull(objectMapper, "Object mapper cannot be null.");
            checkNotNull(credential, "Credential cannot be null.");

            return new Builder(objectMapper)
                    .setKeyHash(credential.getKeyHash())
                    .setDescription(credential.getDescription())
                    .setCreatedAt(credential.getCreatedAt());
        }

    }

}
