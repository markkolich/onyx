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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import onyx.entities.api.response.OnyxApiResponseEntity;

public interface ApiErrorResponse extends OnyxApiResponseEntity {

    @JsonProperty("statusCode")
    int getStatusCode();

    @JsonIgnore
    @Override
    default int getStatus() {
        return getStatusCode();
    }

    @JsonProperty("message")
    String getMessage();

    @JsonProperty("stackTrace")
    String getStackTrace();

    final class Builder extends AbstractOnyxApiResponseEntityBuilder {

        private int statusCode_;
        private String message_;
        private String stackTrace_;

        public Builder(
                final ObjectMapper objectMapper) {
            super(objectMapper);
        }

        public Builder setStatusCode(
                final int statusCode) {
            statusCode_ = statusCode;
            return this;
        }

        public Builder setMessage(
                final String message) {
            message_ = message;
            return this;
        }

        public Builder setStackTrace(
                final String stackTrace) {
            stackTrace_ = stackTrace;
            return this;
        }

        public ApiErrorResponse build() {
            return new ApiErrorResponse() {
                @Override
                public int getStatusCode() {
                    return statusCode_;
                }

                @Override
                public String getMessage() {
                    return message_;
                }

                @Override
                public String getStackTrace() {
                    return stackTrace_;
                }

                @Override
                public ObjectMapper getMapper() {
                    return objectMapper_;
                }
            };
        }

    }

}
