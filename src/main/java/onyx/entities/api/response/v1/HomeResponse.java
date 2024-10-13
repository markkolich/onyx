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

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public interface HomeResponse extends OnyxApiResponseEntity {

    @JsonProperty("path")
    String getPath();

    @JsonProperty("children")
    List<ResourceResponse> getChildren();

    final class Builder extends AbstractOnyxApiResponseEntityBuilder {

        private String path_;

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

        public Builder setChildren(
                final List<ResourceResponse> children) {
            children_ = children;
            return this;
        }

        public HomeResponse build() {
            checkNotNull(path_, "Path cannot be null.");

            checkNotNull(children_, "Children cannot be null.");

            return new HomeResponse() {
                @Override
                public String getPath() {
                    return path_;
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

        public static HomeResponse.Builder fromResource(
                final ObjectMapper objectMapper) {
            checkNotNull(objectMapper, "Object mapper cannot be null.");

            return new HomeResponse.Builder(objectMapper);
        }

    }

}
