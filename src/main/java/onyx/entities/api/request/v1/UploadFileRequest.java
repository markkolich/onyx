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

package onyx.entities.api.request.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import onyx.entities.api.request.OnyxApiRequestEntity;
import onyx.entities.storage.aws.dynamodb.Resource;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@JsonDeserialize(builder = UploadFileRequest.Builder.class)
public interface UploadFileRequest extends OnyxApiRequestEntity {

    @JsonProperty("size")
    long getSize();

    @JsonProperty("description")
    String getDescription();

    @JsonProperty("visibility")
    Resource.Visibility getVisibility();

    @JsonIgnore
    default Builder toBuilder() {
        return new Builder()
                .setSize(getSize())
                .setDescription(getDescription())
                .setVisibility(getVisibility());
    }

    final class Builder {

        private long size_;
        private String description_;
        private Resource.Visibility visibility_;

        @JsonProperty("size")
        public Builder setSize(
                final long size) {
            size_ = size;
            return this;
        }

        @JsonProperty("description")
        public Builder setDescription(
                final String description) {
            description_ = description;
            return this;
        }

        @JsonProperty("visibility")
        public Builder setVisibility(
                final Resource.Visibility visibility) {
            visibility_ = visibility;
            return this;
        }

        public UploadFileRequest build() {
            checkState(size_ >= 0L, "Size must be >= 0");
            checkNotNull(description_, "Description cannot be null.");
            checkNotNull(visibility_, "Visibility cannot be null.");

            return new UploadFileRequest() {
                @Override
                public long getSize() {
                    return size_;
                }

                @Override
                public String getDescription() {
                    return description_;
                }

                @Override
                public Resource.Visibility getVisibility() {
                    return visibility_;
                }
            };
        }

    }

}
