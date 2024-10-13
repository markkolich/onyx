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
import onyx.entities.authentication.Session;
import onyx.entities.storage.aws.dynamodb.Resource;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public interface BrowseResponse extends ResourceResponse {

    @JsonProperty("children")
    List<ResourceResponse> getChildren();

    final class Builder extends AbstractOnyxApiResponseEntityBuilder {

        private String path_;
        private String parent_;
        private String name_;

        private MetadataResponse metadata_;
        private List<ResourceResponse> children_;

        public Builder(
                final ObjectMapper objectMapper) {
            super(objectMapper);
        }

        public Builder setPath(
                final String path) {
            path_ = path;
            return this;
        }

        public Builder setParent(
                final String parent) {
            parent_ = parent;
            return this;
        }

        public Builder setName(
                final String name) {
            name_ = name;
            return this;
        }

        public Builder setMetadata(
                final MetadataResponse metadata) {
            metadata_ = metadata;
            return this;
        }

        public Builder setChildren(
                final List<ResourceResponse> children) {
            children_ = children;
            return this;
        }

        public BrowseResponse build() {
            checkNotNull(path_, "Path cannot be null.");
            checkNotNull(parent_, "Parent cannot be null.");
            checkNotNull(name_, "Name cannot be null.");

            checkNotNull(children_, "Children cannot be null.");

            return new BrowseResponse() {
                @Override
                public String getPath() {
                    return path_;
                }

                @Override
                public String getParent() {
                    return parent_;
                }

                @Override
                public String getName() {
                    return name_;
                }

                @Override
                public MetadataResponse getMetadata() {
                    return metadata_;
                }

                @Override
                public List<ResourceResponse> getChildren() {
                    return children_;
                }

                @Override
                public ObjectMapper getMapper() {
                    return objectMapper_;
                }
            };
        }

        public static BrowseResponse.Builder fromResource(
                final ObjectMapper objectMapper,
                final Resource resource,
                @Nullable final Session session) {
            checkNotNull(objectMapper, "Object mapper cannot be null.");
            checkNotNull(resource, "Resource cannot be null.");

            return new BrowseResponse.Builder(objectMapper)
                    .setPath(resource.getPath())
                    .setParent(resource.getParent())
                    .setName(resource.getHtmlName())
                    .setMetadata(MetadataResponse.Builder.fromResource(objectMapper, resource, session).build());
        }

    }

}
