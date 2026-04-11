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

package onyx.entities.api.request.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import onyx.entities.api.request.OnyxApiRequestEntity;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@JsonDeserialize(builder = CompleteMultipartUploadRequest.Builder.class)
public interface CompleteMultipartUploadRequest extends OnyxApiRequestEntity {

    @JsonProperty("parts")
    List<Part> getParts();

    @JsonDeserialize(builder = CompleteMultipartUploadRequest.Part.Builder.class)
    interface Part {

        @JsonProperty("partNumber")
        int getPartNumber();

        @JsonProperty("eTag")
        String getETag();

        final class Builder {

            private int partNumber_;
            private String eTag_;

            @JsonProperty("partNumber")
            public Builder setPartNumber(
                    final int partNumber) {
                partNumber_ = partNumber;
                return this;
            }

            @JsonProperty("eTag")
            public Builder setETag(
                    final String eTag) {
                eTag_ = eTag;
                return this;
            }

            public Part build() {
                checkNotNull(eTag_, "ETag cannot be null.");

                return new Part() {
                    @Override
                    public int getPartNumber() {
                        return partNumber_;
                    }

                    @Override
                    public String getETag() {
                        return eTag_;
                    }
                };
            }

        }

    }

    final class Builder {

        private List<Part> parts_;

        @JsonProperty("parts")
        public Builder setParts(
                final List<Part> parts) {
            parts_ = parts;
            return this;
        }

        public CompleteMultipartUploadRequest build() {
            checkNotNull(parts_, "Parts cannot be null.");
            checkState(!parts_.isEmpty(), "Parts cannot be empty.");

            return new CompleteMultipartUploadRequest() {
                @Override
                public List<Part> getParts() {
                    return parts_;
                }
            };
        }

    }

}
