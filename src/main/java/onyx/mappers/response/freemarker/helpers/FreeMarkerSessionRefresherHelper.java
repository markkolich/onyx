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

package onyx.mappers.response.freemarker.helpers;

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.core.servlet.HttpResponse;
import onyx.components.authentication.SessionManager;
import onyx.components.authentication.UserAuthenticator;
import onyx.components.config.OnyxConfig;
import onyx.components.config.authentication.SessionConfig;
import onyx.entities.authentication.Session;
import onyx.entities.freemarker.FreeMarkerContent;
import onyx.util.CookieBaker;

import javax.annotation.Nonnull;
import java.time.Instant;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public final class FreeMarkerSessionRefresherHelper {

    private final OnyxConfig onyxConfig_;

    private final UserAuthenticator userAuthenticator_;

    private final SessionConfig sessionConfig_;
    private final SessionManager sessionManager_;

    private final boolean shouldRefreshSessionAutomatically_;

    @Injectable
    public FreeMarkerSessionRefresherHelper(
            @Nonnull final OnyxConfig onyxConfig,
            @Nonnull final UserAuthenticator userAuthenticator,
            @Nonnull final SessionConfig sessionConfig,
            @Nonnull final SessionManager sessionManager) {
        onyxConfig_ = onyxConfig;
        userAuthenticator_ = userAuthenticator;
        sessionConfig_ = sessionConfig;
        sessionManager_ = sessionManager;
        shouldRefreshSessionAutomatically_ = sessionConfig_.shouldRefreshSessionAutomatically();
    }

    public void refreshSession(
            final HttpResponse response,
            @Nonnull final FreeMarkerContent content) {
        final Session session = content.getAttribute(FreeMarkerContent.DATA_MAP_SESSION_ATTR);
        if (shouldRefreshSessionAutomatically_ && session != null) {
            refreshSessionIfNeeded(session, response);
        }
    }

    private void refreshSessionIfNeeded(
            final Session session,
            final HttpResponse response) {
        checkNotNull(session, "Session to refresh cannot be null.");
        checkNotNull(response, "HTTP servlet response cannot be null.");

        final Instant refreshAfter = session.getRefreshAfter();
        if (Instant.now().isAfter(refreshAfter)) {
            final Session refreshed = userAuthenticator_.refreshSession(session);
            final String signedRefreshedSession = sessionManager_.signSession(refreshed);

            final CookieBaker refreshedSessionCookieBaker = new CookieBaker.Builder()
                    .setName(SessionManager.SESSION_COOKIE_NAME)
                    .setValue(signedRefreshedSession)
                    .setDomain(sessionConfig_.getSessionDomain())
                    .setContextPath(onyxConfig_.getContextPath())
                    .setSecure(sessionConfig_.isSessionUsingHttps())
                    .build();
            refreshedSessionCookieBaker.bake(response);
        }
    }

}
