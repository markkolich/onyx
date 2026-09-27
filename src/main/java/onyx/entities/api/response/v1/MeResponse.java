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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import onyx.entities.api.response.OnyxApiResponseEntity;
import onyx.entities.authentication.Session;

import java.time.Instant;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identity of the caller behind the current session, as returned by the "who am I"
 * endpoint.
 *
 * <p>Deliberately does not carry {@link Session#getId()}. For a cookie-backed session
 * the session identifier is effectively bearer material, and an endpoint whose whole
 * purpose is validating a credential must not hand back something that could itself
 * be replayed. Nothing from {@link onyx.entities.authentication.User} belongs here
 * either &mdash; that interface exposes the password hash and mobile number.
 */
public interface MeResponse extends OnyxApiResponseEntity {

    @JsonProperty("username")
    String getUsername();

    /**
     * How this caller authenticated &mdash; {@link Session.Type#USER} for a browser
     * cookie session, {@link Session.Type#API} for an API key. Useful when a script or
     * batch job needs to confirm which credential it actually presented.
     */
    @JsonProperty("type")
    Session.Type getType();

    @JsonProperty("expiry")
    Instant getExpiry();

    final class Builder extends AbstractOnyxApiResponseEntityBuilder {

        private String username_;
        private Session.Type type_;
        private Instant expiry_;

        public Builder(
                final ObjectMapper objectMapper) {
            super(objectMapper);
        }

        public Builder setUsername(
                final String username) {
            username_ = username;
            return this;
        }

        public Builder setType(
                final Session.Type type) {
            type_ = type;
            return this;
        }

        public Builder setExpiry(
                final Instant expiry) {
            expiry_ = expiry;
            return this;
        }

        public MeResponse build() {
            checkNotNull(username_, "Username cannot be null.");
            checkNotNull(type_, "Session type cannot be null.");
            checkNotNull(expiry_, "Expiry instant cannot be null.");

            return new MeResponse() {
                @Override
                public String getUsername() {
                    return username_;
                }

                @Override
                public Session.Type getType() {
                    return type_;
                }

                @Override
                public Instant getExpiry() {
                    return expiry_;
                }

                @Override
                public ObjectMapper getMapper() {
                    return objectMapper_;
                }
            };
        }

        public static Builder fromSession(
                final ObjectMapper objectMapper,
                final Session session) {
            checkNotNull(objectMapper, "Object mapper cannot be null.");
            checkNotNull(session, "Session cannot be null.");

            return new Builder(objectMapper)
                    .setUsername(session.getUsername())
                    .setType(session.getType())
                    .setExpiry(session.getExpiry());
        }

    }

}
