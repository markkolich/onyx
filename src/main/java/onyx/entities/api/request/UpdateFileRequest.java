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

package onyx.entities.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import onyx.entities.storage.aws.dynamodb.Resource;

import javax.annotation.Nullable;

@JsonDeserialize(builder = UpdateFileRequest.Builder.class)
public interface UpdateFileRequest {

    @Nullable
    @JsonProperty("description")
    String getDescription();

    @Nullable
    @JsonProperty("visibility")
    Resource.Visibility getVisibility();

    @Nullable
    @JsonProperty("favorite")
    Boolean getFavorite();

    @JsonIgnore
    default Builder toBuilder() {
        return new Builder()
                .setDescription(getDescription())
                .setVisibility(getVisibility())
                .setFavorite(getFavorite());
    }

    final class Builder {

        private String description_;

        private Resource.Visibility visibility_;
        private Boolean favorite_;

        @JsonProperty("description")
        public Builder setDescription(
                final String description) {
            description_ = description;
            return this;
        }

        @JsonProperty("visibility")
        public Builder setVisibility(
                @Nullable final Resource.Visibility visibility) {
            visibility_ = visibility;
            return this;
        }

        @JsonProperty("favorite")
        public Builder setFavorite(
                @Nullable final Boolean favorite) {
            favorite_ = favorite;
            return this;
        }

        public UpdateFileRequest build() {
            return new UpdateFileRequest() {
                @Nullable
                @Override
                public String getDescription() {
                    return description_;
                }

                @Nullable
                @Override
                public Resource.Visibility getVisibility() {
                    return visibility_;
                }

                @Nullable
                @Override
                public Boolean getFavorite() {
                    return favorite_;
                }
            };
        }

    }

}
