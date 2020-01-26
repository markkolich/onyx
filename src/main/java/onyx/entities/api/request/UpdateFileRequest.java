/*
 * Copyright (c) 2020 Mark S. Kolich
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import onyx.entities.storage.aws.dynamodb.Resource;

import javax.annotation.Nullable;

@JsonDeserialize(builder = UpdateFileRequest.Builder.class)
public final class UpdateFileRequest {

    private final Resource.Visibility visibility_;
    private final Boolean favorite_;

    private UpdateFileRequest(
            final Resource.Visibility visibility,
            final Boolean favorite) {
        visibility_ = visibility;
        favorite_ = favorite;
    }

    @Nullable
    @JsonProperty("visibility")
    public Resource.Visibility getVisibility() {
        return visibility_;
    }

    @Nullable
    @JsonProperty("favorite")
    public Boolean getFavorite() {
        return favorite_;
    }

    public static final class Builder {

        private Resource.Visibility visibility_;
        private Boolean favorite_;

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
            return new UpdateFileRequest(visibility_, favorite_);
        }

    }

}
