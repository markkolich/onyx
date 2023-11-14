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

package onyx.entities.authentication;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonDeserialize(builder = User.Builder.class)
public interface User {

    @JsonProperty("username")
    String getUsername();

    @JsonProperty("password")
    String getPassword();

    @JsonProperty("mobileNumber")
    String getMobileNumber();

    @JsonIgnore
    default Builder toBuilder() {
        return new Builder()
                .setUsername(getUsername())
                .setPassword(getPassword())
                .setMobileNumber(getMobileNumber());
    }

    final class Builder {

        private String username_;
        private String password_;

        private String mobileNumber_;

        @JsonProperty("username")
        public Builder setUsername(
                final String username) {
            username_ = username;
            return this;
        }

        @JsonProperty("password")
        public Builder setPassword(
                final String password) {
            password_ = password;
            return this;
        }

        @JsonProperty("mobileNumber")
        public Builder setMobileNumber(
                final String mobileNumber) {
            mobileNumber_ = mobileNumber;
            return this;
        }

        public User build() {
            checkNotNull(username_, "Username cannot be null.");
            checkNotNull(password_, "Password cannot be null.");
            checkNotNull(mobileNumber_, "Mobile phone number cannot be null.");

            return new User() {
                @Override
                public String getUsername() {
                    return username_;
                }

                @Override
                public String getPassword() {
                    return password_;
                }

                @Override
                public String getMobileNumber() {
                    return mobileNumber_;
                }
            };
        }

    }

}
