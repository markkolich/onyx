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

package onyx.entities.authentication.webauthn;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonDeserialize(builder = WebAuthnCredential.Builder.class)
public interface WebAuthnCredential {

    @JsonProperty("credentialId")
    String getCredentialId();

    @JsonProperty("publicKeyCose")
    String getPublicKeyCose();

    @JsonProperty("signatureCount")
    long getSignatureCount();

    final class Builder {

        private String credentialId_;
        private String publicKeyCose_;

        private long signatureCount_;

        @JsonProperty("credentialId")
        public Builder setCredentialId(
                final String credentialId) {
            credentialId_ = credentialId;
            return this;
        }

        @JsonProperty("publicKeyCose")
        public Builder setPublicKeyCose(
                final String publicKeyCose) {
            publicKeyCose_ = publicKeyCose;
            return this;
        }

        @JsonProperty("signatureCount")
        public Builder setSignatureCount(
                final long signatureCount) {
            signatureCount_ = signatureCount;
            return this;
        }

        public WebAuthnCredential build() {
            checkNotNull(credentialId_, "Credential ID cannot be null.");
            checkNotNull(publicKeyCose_, "Public key COSE cannot be null.");

            return new WebAuthnCredential() {
                @Override
                public String getCredentialId() {
                    return credentialId_;
                }

                @Override
                public String getPublicKeyCose() {
                    return publicKeyCose_;
                }

                @Override
                public long getSignatureCount() {
                    return signatureCount_;
                }
            };
        }

    }

}
