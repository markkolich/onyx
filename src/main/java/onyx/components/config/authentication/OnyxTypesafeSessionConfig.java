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

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.OnyxConfig;
import onyx.entities.authentication.User;
import onyx.exceptions.OnyxException;
import org.apache.commons.lang3.StringUtils;

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
        final ConfigList userCredentialsInConfig = config_.getList(SESSION_USERS_PROP);

        final ImmutableMap.Builder<String, User> userCredentialsBuilder =
                ImmutableMap.builder();
        for (final ConfigValue configValue : userCredentialsInConfig) {
            if (!ConfigValueType.OBJECT.equals(configValue.valueType())) {
                continue;
            }

            @SuppressWarnings("unchecked") // intentional, and safe
            final Map<String, String> configUser = (Map<String, String>) configValue.unwrapped();

            final String username = configUser.get(USERS_USERNAME_PROP);
            if (StringUtils.isBlank(username)) {
                throw missingUserConfigKey(USERS_USERNAME_PROP);
            }

            final String password = configUser.get(USERS_PASSWORD_PROP);
            if (StringUtils.isBlank(password)) {
                throw missingUserConfigKey(USERS_PASSWORD_PROP);
            }

            final String mobileNumber = configUser.get(USERS_MOBILE_NUMBER_PROP);
            if (StringUtils.isBlank(mobileNumber)) {
                throw missingUserConfigKey(USERS_MOBILE_NUMBER_PROP);
            }

            final User user = new User.Builder()
                    .setUsername(username)
                    .setPassword(password)
                    .setMobileNumber(mobileNumber)
                    .build();

            userCredentialsBuilder.put(username, user);
        }

        return userCredentialsBuilder.build();
    }

    @Override
    public Map<String, String> getApiKeys() {
        final ConfigList apiKeysInConfig = config_.getList(SESSION_API_KEYS_PROP);

        final ImmutableMap.Builder<String, String> apiKeysBuilder =
                ImmutableMap.builder();
        for (final ConfigValue configValue : apiKeysInConfig) {
            if (!ConfigValueType.OBJECT.equals(configValue.valueType())) {
                continue;
            }

            @SuppressWarnings("unchecked") // intentional, and safe
            final Map<String, String> configUser = (Map<String, String>) configValue.unwrapped();

            final String username = configUser.get(API_KEY_USERNAME_PROP);
            if (StringUtils.isBlank(username)) {
                throw missingApiKeyConfigKey(API_KEY_USERNAME_PROP);
            }

            final String apiKey = configUser.get(API_KEY_KEY_PROP);
            if (StringUtils.isBlank(apiKey)) {
                throw missingApiKeyConfigKey(API_KEY_KEY_PROP);
            }

            apiKeysBuilder.put(apiKey, username);
        }

        return apiKeysBuilder.build();
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

    private static OnyxException missingUserConfigKey(
            final String key) {
        return new OnyxException("Blank or null user-authenticator config key: " + key);
    }

    private static OnyxException missingApiKeyConfigKey(
            final String key) {
        return new OnyxException("Blank or null API key config key: " + key);
    }

}
