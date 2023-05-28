/*
 * Copyright (c) 2023 Mark S. Kolich
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

package onyx.components.authentication.api;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.authentication.UserAuthenticator;
import onyx.components.config.authentication.SessionConfig;
import onyx.entities.authentication.Session;
import onyx.entities.authentication.Session.Type;
import onyx.exceptions.OnyxException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * An {@link ApiKeyAuthenticator} implementation backed by an in-memory map loaded
 * at startup from immutable app configuration.
 */
@Component
public final class OnyxConfigApiKeyAuthenticator implements ApiKeyAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(OnyxConfigApiKeyAuthenticator.class);

    private static final String API_KEY_USERNAME_PROP = "username";
    private static final String API_KEY_KEY_PROP = "key";

    private final SessionConfig sessionConfig_;

    private final UserAuthenticator userAuthenticator_;

    private final Map<String, String> apiKeys_;

    @Injectable
    public OnyxConfigApiKeyAuthenticator(
            final SessionConfig sessionConfig,
            final UserAuthenticator userAuthenticator) {
        sessionConfig_ = sessionConfig;
        userAuthenticator_ = userAuthenticator;

        apiKeys_ = buildApiKeysFromConfig();
    }

    private Map<String, String> buildApiKeysFromConfig() {
        final ImmutableMap.Builder<String, String> apiKeysBuilder =
                ImmutableMap.builder();

        final ConfigList apiKeysInConfig = sessionConfig_.getApiKeys();
        for (final ConfigValue configValue : apiKeysInConfig) {
            if (!ConfigValueType.OBJECT.equals(configValue.valueType())) {
                continue;
            }

            @SuppressWarnings("unchecked") // intentional, and safe
            final Map<String, String> configUser = (Map<String, String>) configValue.unwrapped();

            final String username = configUser.get(API_KEY_USERNAME_PROP);
            if (StringUtils.isBlank(username)) {
                throw missingConfigKey(API_KEY_USERNAME_PROP);
            }

            final String apiKey = configUser.get(API_KEY_KEY_PROP);
            if (StringUtils.isBlank(apiKey)) {
                throw missingConfigKey(API_KEY_KEY_PROP);
            }

            apiKeysBuilder.put(apiKey, username);
        }

        return apiKeysBuilder.build();
    }

    @Nullable
    @Override
    public Session getSessionForApiKey(
            final String apiKey) {
        if (StringUtils.isBlank(apiKey)) {
            return null;
        }

        final String usernameForKey = apiKeys_.get(apiKey);
        if (usernameForKey == null) {
            return null;
        }

        return userAuthenticator_.getSessionForUsername(Type.API, usernameForKey);
    }

    private static OnyxException missingConfigKey(
            final String key) {
        return new OnyxException("Blank or null API key config key: " + key);
    }

}
