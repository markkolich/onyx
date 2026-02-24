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

package onyx.entities.api.response.v1.webauthn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import onyx.entities.api.response.OnyxApiResponseEntity;

import static com.google.common.base.Preconditions.checkNotNull;

public interface WebAuthnRegisterBeginResponse extends OnyxApiResponseEntity {

    @JsonProperty("requestId")
    String getRequestId();

    @JsonProperty("publicKeyCredentialCreationOptions")
    JsonNode getPublicKeyCredentialCreationOptions();

    final class Builder extends AbstractOnyxApiResponseEntityBuilder {

        private String requestId_;

        private JsonNode publicKeyCredentialCreationOptions_;

        public Builder(
                final ObjectMapper objectMapper) {
            super(objectMapper);
        }

        public Builder setRequestId(
                final String requestId) {
            requestId_ = requestId;
            return this;
        }

        public Builder setPublicKeyCredentialCreationOptions(
                final JsonNode publicKeyCredentialCreationOptions) {
            publicKeyCredentialCreationOptions_ = publicKeyCredentialCreationOptions;
            return this;
        }

        public WebAuthnRegisterBeginResponse build() {
            checkNotNull(requestId_, "Request ID cannot be null.");
            checkNotNull(publicKeyCredentialCreationOptions_,
                    "Public key credential creation options cannot be null.");

            return new WebAuthnRegisterBeginResponse() {
                @Override
                public String getRequestId() {
                    return requestId_;
                }

                @Override
                public JsonNode getPublicKeyCredentialCreationOptions() {
                    return publicKeyCredentialCreationOptions_;
                }

                @JsonIgnore
                @Override
                public ObjectMapper getMapper() {
                    return objectMapper_;
                }
            };
        }

    }

}
