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
import onyx.entities.storage.metadata.ResourceMetadata;

import static com.google.common.base.Preconditions.checkNotNull;

public interface ResourceMetadataResponse extends OnyxApiResponseEntity {

    @JsonProperty("key")
    String getKey();

    final class Builder extends AbstractOnyxApiResponseEntityBuilder {

        private String key_;

        public Builder(
                final ObjectMapper objectMapper) {
            super(objectMapper);
        }

        public Builder setKey(
                final String key) {
            key_ = key;
            return this;
        }

        public ResourceMetadataResponse build() {
            checkNotNull(key_, "Key cannot be null.");

            return new ResourceMetadataResponse() {
                @Override
                public String getKey() {
                    return key_;
                }

                @Override
                public ObjectMapper getMapper() {
                    return objectMapper_;
                }
            };
        }

        public static ResourceMetadataResponse.Builder fromResourceMetadata(
                final ObjectMapper objectMapper,
                final ResourceMetadata metadata) {
            checkNotNull(objectMapper, "Object mapper cannot be null.");
            checkNotNull(metadata, "Resource metadata cannot be null.");

            return new ResourceMetadataResponse.Builder(objectMapper)
                    .setKey(metadata.getKey());
        }

    }

}
