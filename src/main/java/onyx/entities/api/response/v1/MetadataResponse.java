/*
 * Copyright (c) 2024 Mark S. Kolich
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
import onyx.entities.authentication.Session;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.openapitools.jackson.nullable.JsonNullable;

import javax.annotation.Nullable;
import java.time.Instant;

import static com.google.common.base.Preconditions.checkNotNull;
import static onyx.util.UserUtils.userIsOwner;

public interface MetadataResponse extends OnyxApiResponseEntity {

    @JsonProperty("type")
    Resource.Type getType();

    @JsonProperty("visibility")
    Resource.Visibility getVisibility();

    @JsonProperty("owner")
    String getOwner();

    @JsonProperty("createdAt")
    Instant getCreatedAt();

    @JsonProperty("description")
    String getDescription();

    @JsonProperty("size")
    JsonNullable<Long> getSize();

    @JsonProperty("sizeReadable")
    JsonNullable<String> getSizeReadable();

    @JsonProperty("favorite")
    JsonNullable<Boolean> getFavorite();

    final class Builder extends AbstractOnyxApiResponseEntityBuilder {

        private Resource.Type type_;
        private Resource.Visibility visibility_;
        private String owner_;
        private Instant createdAt_;
        private String description_;
        private JsonNullable<Long> size_;
        private JsonNullable<String> sizeReadable_;
        private JsonNullable<Boolean> favorite_;

        public Builder(
                final ObjectMapper objectMapper) {
            super(objectMapper);
        }

        public Builder setType(
                final Resource.Type type) {
            type_ = type;
            return this;
        }

        public Builder setVisibility(
                final Resource.Visibility visibility) {
            visibility_ = visibility;
            return this;
        }

        public Builder setOwner(
                final String owner) {
            owner_ = owner;
            return this;
        }

        public Builder setCreatedAt(
                final Instant createdAt) {
            createdAt_ = createdAt;
            return this;
        }

        public Builder setDescription(
                final String description) {
            description_ = description;
            return this;
        }

        public Builder setSize(
                final JsonNullable<Long> size) {
            size_ = size;
            return this;
        }

        public Builder setSizeReadable(
                final JsonNullable<String> sizeReadable) {
            sizeReadable_ = sizeReadable;
            return this;
        }

        public Builder setFavorite(
                final JsonNullable<Boolean> favorite) {
            favorite_ = favorite;
            return this;
        }

        public MetadataResponse build() {
            return new MetadataResponse() {
                @Override
                public Resource.Type getType() {
                    return type_;
                }

                @Override
                public Resource.Visibility getVisibility() {
                    return visibility_;
                }

                @Override
                public String getOwner() {
                    return owner_;
                }

                @Override
                public Instant getCreatedAt() {
                    return createdAt_;
                }

                @Override
                public String getDescription() {
                    return description_;
                }

                @Override
                public JsonNullable<Long> getSize() {
                    return size_;
                }

                @Override
                public JsonNullable<String> getSizeReadable() {
                    return sizeReadable_;
                }

                @Override
                public JsonNullable<Boolean> getFavorite() {
                    return favorite_;
                }

                @Override
                public ObjectMapper getMapper() {
                    return objectMapper_;
                }
            };
        }

        public static MetadataResponse.Builder fromResource(
                final ObjectMapper objectMapper,
                final Resource resource,
                @Nullable final Session session) {
            checkNotNull(objectMapper, "Object mapper cannot be null.");
            checkNotNull(resource, "Resource cannot be null.");

            final boolean userIsOwner = userIsOwner(resource, session);
            final boolean resourceIsFile = Resource.Type.FILE.equals(resource.getType());

            return new MetadataResponse.Builder(objectMapper)
                    .setType(resource.getType())
                    .setVisibility(resource.getVisibility())
                    .setOwner(resource.getOwner())
                    .setCreatedAt(resource.getCreatedAt())
                    .setDescription(resource.getHtmlDescription())
                    .setSize(userIsOwner || resourceIsFile
                            ? JsonNullable.of(resource.getSize()) : JsonNullable.undefined())
                    .setSizeReadable(userIsOwner || resourceIsFile
                            ? JsonNullable.of(resource.getHtmlSize()) : JsonNullable.undefined())
                    .setFavorite(userIsOwner
                            ? JsonNullable.of(resource.getFavorite()) : JsonNullable.undefined());
        }

    }

}
