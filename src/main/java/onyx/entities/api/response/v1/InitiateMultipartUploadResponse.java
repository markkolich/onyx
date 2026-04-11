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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import onyx.entities.api.response.OnyxApiResponseEntity;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.core.servlet.HttpStatus.SC_CREATED;

public interface InitiateMultipartUploadResponse extends OnyxApiResponseEntity {

    @JsonProperty("uploadId")
    String getUploadId();

    @JsonProperty("partSize")
    long getPartSize();

    @JsonProperty("parts")
    List<Part> getParts();

    @JsonIgnore
    @Override
    default int getStatus() {
        return SC_CREATED;
    }

    interface Part {

        @JsonProperty("partNumber")
        int getPartNumber();

        @JsonProperty("presignedUrl")
        String getPresignedUrl();

        final class Builder {

            private int partNumber_;
            private String presignedUrl_;

            public Builder setPartNumber(
                    final int partNumber) {
                partNumber_ = partNumber;
                return this;
            }

            public Builder setPresignedUrl(
                    final String presignedUrl) {
                presignedUrl_ = presignedUrl;
                return this;
            }

            public Part build() {
                checkNotNull(presignedUrl_, "Presigned URL cannot be null.");

                return new Part() {
                    @Override
                    public int getPartNumber() {
                        return partNumber_;
                    }

                    @Override
                    public String getPresignedUrl() {
                        return presignedUrl_;
                    }
                };
            }

        }

    }

    final class Builder extends AbstractOnyxApiResponseEntityBuilder {

        private String uploadId_;
        private long partSize_;
        private List<Part> parts_;

        public Builder(
                final ObjectMapper objectMapper) {
            super(objectMapper);
        }

        public Builder setUploadId(
                final String uploadId) {
            uploadId_ = uploadId;
            return this;
        }

        public Builder setPartSize(
                final long partSize) {
            partSize_ = partSize;
            return this;
        }

        public Builder setParts(
                final List<Part> parts) {
            parts_ = parts;
            return this;
        }

        public InitiateMultipartUploadResponse build() {
            checkNotNull(uploadId_, "Upload ID cannot be null.");
            checkNotNull(parts_, "Parts cannot be null.");

            return new InitiateMultipartUploadResponse() {
                @Override
                public String getUploadId() {
                    return uploadId_;
                }

                @Override
                public long getPartSize() {
                    return partSize_;
                }

                @Override
                public List<Part> getParts() {
                    return parts_;
                }

                @Override
                public ObjectMapper getMapper() {
                    return objectMapper_;
                }
            };
        }

    }

}
