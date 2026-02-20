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

package onyx.components.config.authentication;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.OnyxConfig;
import onyx.entities.authentication.User;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public final class OnyxTypesafeSessionConfig implements SessionConfig {

    private final Config config_;

    @Injectable
    public OnyxTypesafeSessionConfig(
            final OnyxConfig onyxConfig) {
        config_ = onyxConfig.getOnyxConfig().getConfig(SESSION_CONFIG_PATH);
    }

    @Override
    public Map<String, User> getUsers() {
        return config_.getObjectList(SESSION_USERS_PROP).stream()
                .map(ConfigObject::toConfig)
                .map(userConfig -> new User.Builder()
                        .setUsername(userConfig.getString(USERS_USERNAME_PROP))
                        .setPassword(userConfig.getString(USERS_PASSWORD_PROP))
                        .setMobileNumber(userConfig.getString(USERS_MOBILE_NUMBER_PROP))
                        .build())
                .collect(ImmutableMap.toImmutableMap(User::getUsername, user -> user));
    }

    @Override
    public Map<String, String> getApiKeys() {
        return config_.getObjectList(SESSION_API_KEYS_PROP).stream()
                .map(ConfigObject::toConfig)
                .collect(ImmutableMap.toImmutableMap(
                        apiKeyConfig -> apiKeyConfig.getString(API_KEY_KEY_PROP),
                        apiKeyConfig -> apiKeyConfig.getString(API_KEY_USERNAME_PROP)));
    }

    @Override
    public String getSessionDomain() {
        return config_.getString(SESSION_DOMAIN_PROP);
    }

    @Override
    public long getSessionDuration(
            final TimeUnit timeUnit) {
        return config_.getDuration(SESSION_DURATION_PROP, timeUnit);
    }

    @Override
    public boolean isSessionUsingHttps() {
        return config_.getBoolean(SESSION_HTTPS_PROP);
    }

    @Override
    public boolean shouldRefreshSessionAutomatically() {
        return config_.getBoolean(SESSION_REFRESH_AUTOMATICALLY_PROP);
    }

    @Override
    public long getRefreshSessionAfter(
            final TimeUnit timeUnit) {
        return config_.getDuration(SESSION_REFRESH_AFTER_PROP, timeUnit);
    }

}
