/*
 * Copyright (c) 2020 Mark S. Kolich
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

package onyx.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonDeserialize(builder = Session.Builder.class)
public final class Session {

    private final String id_;
    private final String username_;

    private final Date expiry_;

    private Session(
            final String id,
            final String username,
            final Date expiry) {
        id_ = id;
        username_ = username;
        expiry_ = expiry;
    }

    @JsonProperty("id")
    public String getId() {
        return id_;
    }

    @JsonProperty("username")
    public String getUsername() {
        return username_;
    }

    @JsonProperty("expiry")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    public Date getExpiry() {
        return expiry_;
    }

    public static final class Builder {

        private String id_;
        private String username_;

        private Date expiry_;

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

        @JsonProperty("expiry")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        public Builder setExpiry(
                final Date expiry) {
            expiry_ = expiry;
            return this;
        }

        public Session build() {
            checkNotNull(id_, "Session ID cannot be null.");
            checkNotNull(username_, "Session username cannot be null.");
            checkNotNull(expiry_, "Session expiry cannot be null.");

            return new Session(id_, username_, expiry_);
        }

    }

}
