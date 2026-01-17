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

package onyx.entities.authentication.twofactor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import onyx.entities.authentication.Session;

import java.time.Instant;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonDeserialize(builder = TwoFactorAuthToken.Builder.class)
public interface TwoFactorAuthToken {

    @JsonProperty("id")
    String getId();

    @JsonProperty("hash")
    String getHash();

    @JsonProperty("session")
    Session getSession();

    @JsonProperty("expiry")
    Instant getExpiry();

    @JsonIgnore
    default Builder toBuilder() {
        return new Builder()
                .setId(getId())
                .setHash(getHash())
                .setSession(getSession())
                .setExpiry(getExpiry());
    }

    final class Builder {

        private String id_;
        private String hash_;

        private Session session_;

        private Instant expiry_;

        @JsonProperty("id")
        public Builder setId(
                final String id) {
            id_ = id;
            return this;
        }

        @JsonProperty("hash")
        public Builder setHash(
                final String hash) {
            hash_ = hash;
            return this;
        }

        @JsonProperty("session")
        public Builder setSession(
                final Session session) {
            session_ = session;
            return this;
        }

        @JsonProperty("expiry")
        public Builder setExpiry(
                final Instant expiry) {
            expiry_ = expiry;
            return this;
        }

        public TwoFactorAuthToken build() {
            checkNotNull(id_, "2FA token ID cannot be null.");
            checkNotNull(hash_, "2FA token hash cannot be null.");
            checkNotNull(session_, "2FA token session cannot be null.");
            checkNotNull(expiry_, "2FA token expiry cannot be null.");

            return new TwoFactorAuthToken() {
                @Override
                public String getId() {
                    return id_;
                }

                @Override
                public String getHash() {
                    return hash_;
                }

                @Override
                public Session getSession() {
                    return session_;
                }

                @Override
                public Instant getExpiry() {
                    return expiry_;
                }
            };
        }

    }

}
