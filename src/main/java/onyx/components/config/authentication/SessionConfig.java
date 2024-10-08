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

package onyx.components.config.authentication;

import onyx.entities.authentication.User;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface SessionConfig {

    String SESSION_CONFIG_PATH = "session";

    String SESSION_USERS_PROP = "users";
    String SESSION_API_KEYS_PROP = "api-keys";
    String SESSION_DOMAIN_PROP = "domain";
    String SESSION_DURATION_PROP = "duration";
    String SESSION_HTTPS_PROP = "https";

    String SESSION_REFRESH_AUTOMATICALLY_PROP = "refresh-automatically";
    String SESSION_REFRESH_AFTER_PROP = "refresh-after";

    String USERS_USERNAME_PROP = "username";
    String USERS_PASSWORD_PROP = "password";
    String USERS_MOBILE_NUMBER_PROP = "mobileNumber";

    String API_KEY_USERNAME_PROP = "username";
    String API_KEY_KEY_PROP = "key";

    Map<String, User> getUsers();

    Map<String, String> getApiKeys();

    String getSessionDomain();

    /**
     * Once generated, a user-session will remain valid for this long.
     */
    long getSessionDuration(
            final TimeUnit timeUnit);

    boolean isSessionUsingHttps();

    boolean shouldRefreshSessionAutomatically();

    long getRefreshSessionAfter(
            final TimeUnit timeUnit);

}
