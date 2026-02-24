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

package onyx.entities.api.request.v1.webauthn;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import onyx.entities.api.request.OnyxApiRequestEntity;

import java.time.Instant;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonDeserialize(builder = WebAuthnAuthenticationRequest.Builder.class)
public interface WebAuthnAuthenticationRequest extends OnyxApiRequestEntity {

    @JsonProperty("id")
    String getId();

    @JsonProperty("username")
    String getUsername();

    @JsonProperty("challenge")
    String getChallenge();

    @JsonProperty("assertionRequestJson")
    String getAssertionRequestJson();

    @JsonProperty("expiry")
    Instant getExpiry();

    final class Builder {

        private String id_;

        private String username_;

        private String challenge_;

        private String assertionRequestJson_;

        private Instant expiry_;

        @JsonProperty("id")
        public Builder setId(
                final String id) {
            id_ = id;
            return this;
        }

        @JsonProperty("username")
        public Builder setUsername(
                final String username) {
            username_ = username;
            return this;
        }

        @JsonProperty("challenge")
        public Builder setChallenge(
                final String challenge) {
            challenge_ = challenge;
            return this;
        }

        @JsonProperty("assertionRequestJson")
        public Builder setAssertionRequestJson(
                final String assertionRequestJson) {
            assertionRequestJson_ = assertionRequestJson;
            return this;
        }

        @JsonProperty("expiry")
        public Builder setExpiry(
                final Instant expiry) {
            expiry_ = expiry;
            return this;
        }

        public WebAuthnAuthenticationRequest build() {
            checkNotNull(id_, "WebAuthn authentication request ID cannot be null.");
            checkNotNull(challenge_, "WebAuthn authentication request challenge cannot be null.");
            checkNotNull(assertionRequestJson_, "WebAuthn authentication request assertion request cannot be null.");
            checkNotNull(expiry_, "WebAuthn authentication request expiry cannot be null.");

            return new WebAuthnAuthenticationRequest() {
                @Override
                public String getId() {
                    return id_;
                }

                @Override
                public String getUsername() {
                    return username_;
                }

                @Override
                public String getChallenge() {
                    return challenge_;
                }

                @Override
                public String getAssertionRequestJson() {
                    return assertionRequestJson_;
                }

                @Override
                public Instant getExpiry() {
                    return expiry_;
                }
            };
        }

    }

}
