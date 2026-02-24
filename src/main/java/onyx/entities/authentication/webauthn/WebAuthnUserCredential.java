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

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonDeserialize(builder = WebAuthnUserCredential.Builder.class)
public interface WebAuthnUserCredential {

    @JsonProperty("username")
    String getUsername();

    @JsonProperty("userHandle")
    String getUserHandle();

    @JsonProperty("credentials")
    List<WebAuthnCredential> getCredentials();

    final class Builder {

        private String username_;
        private String userHandle_;

        private List<WebAuthnCredential> credentials_;

        @JsonProperty("username")
        public Builder setUsername(
                final String username) {
            username_ = username;
            return this;
        }

        @JsonProperty("userHandle")
        public Builder setUserHandle(
                final String userHandle) {
            userHandle_ = userHandle;
            return this;
        }

        @JsonProperty("credentials")
        public Builder setCredentials(
                final List<WebAuthnCredential> credentials) {
            credentials_ = credentials;
            return this;
        }

        public WebAuthnUserCredential build() {
            checkNotNull(username_, "Username cannot be null.");
            checkNotNull(userHandle_, "User handle cannot be null.");
            checkNotNull(credentials_, "Credentials list cannot be null.");

            return new WebAuthnUserCredential() {
                @Override
                public String getUsername() {
                    return username_;
                }

                @Override
                public String getUserHandle() {
                    return userHandle_;
                }

                @Override
                public List<WebAuthnCredential> getCredentials() {
                    return credentials_;
                }
            };
        }

    }

}
