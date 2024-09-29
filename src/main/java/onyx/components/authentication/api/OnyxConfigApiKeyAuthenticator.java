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

package onyx.components.authentication.api;

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.authentication.UserAuthenticator;
import onyx.components.config.authentication.SessionConfig;
import onyx.entities.authentication.Session;
import onyx.entities.authentication.Session.Type;
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

    private final SessionConfig sessionConfig_;

    private final UserAuthenticator userAuthenticator_;

    private final Map<String, String> apiKeys_;

    @Injectable
    public OnyxConfigApiKeyAuthenticator(
            final SessionConfig sessionConfig,
            final UserAuthenticator userAuthenticator) {
        sessionConfig_ = sessionConfig;
        userAuthenticator_ = userAuthenticator;

        apiKeys_ = sessionConfig_.getApiKeys();
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

}
