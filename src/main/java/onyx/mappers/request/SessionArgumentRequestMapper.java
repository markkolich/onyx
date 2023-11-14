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

package onyx.mappers.request;

import com.google.common.net.HttpHeaders;
import curacao.annotations.Injectable;
import curacao.annotations.Mapper;
import curacao.context.CuracaoContext;
import curacao.core.servlet.HttpCookie;
import curacao.core.servlet.HttpRequest;
import curacao.mappers.request.AbstractControllerArgumentMapper;
import onyx.components.authentication.SessionManager;
import onyx.components.authentication.api.ApiKeyAuthenticator;
import onyx.entities.authentication.Session;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.List;

import static onyx.components.authentication.api.ApiKeyAuthenticator.API_KEY_AUTH_HEADER_PREFIX;
import static onyx.util.CookieBaker.getFirstCookieByName;

@Mapper
public final class SessionArgumentRequestMapper
        extends AbstractControllerArgumentMapper<Session> {

    private final ApiKeyAuthenticator apiKeySessionManager_;
    private final SessionManager sessionManager_;

    @Injectable
    public SessionArgumentRequestMapper(
            final ApiKeyAuthenticator apiKeySessionManager,
            final SessionManager sessionManager) {
        apiKeySessionManager_ = apiKeySessionManager;
        sessionManager_ = sessionManager;
    }

    @Override
    public Session resolve(
            @Nullable final Annotation annotation,
            @Nonnull final CuracaoContext context) throws Exception {
        final HttpRequest request = context.getRequest();

        // 1. If there's an API key on the request, validate it first.
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String apiKey = StringUtils.removeStart(authHeader, API_KEY_AUTH_HEADER_PREFIX);
        if (StringUtils.isNotBlank(apiKey)) {
            final Session session = apiKeySessionManager_.getSessionForApiKey(apiKey);
            if (session != null) {
                context.setProperty(SessionManager.SESSION_COOKIE_NAME, session);
                return session;
            }
        }

        // 2. If no API key is present, fallback to extracting the user session from a
        // signed session cookie.
        final List<HttpCookie> cookies = request.getCookies();
        if (CollectionUtils.isNotEmpty(cookies)) {
            final HttpCookie sessionCookie = getFirstCookieByName(cookies, SessionManager.SESSION_COOKIE_NAME);
            if (sessionCookie != null) {
                final Session session = sessionManager_.extractSignedSession(sessionCookie.getValue());
                if (session != null) {
                    context.setProperty(SessionManager.SESSION_COOKIE_NAME, session);
                    return session;
                }
            }
        }

        return null;
    }

}
