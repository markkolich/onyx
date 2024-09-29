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

package onyx.components.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.security.StringSigner;
import onyx.entities.authentication.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Instant;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public final class OnyxSignedSessionManager implements SessionManager {

    private static final Logger LOG = LoggerFactory.getLogger(OnyxSignedSessionManager.class);

    private final StringSigner stringSigner_;

    private final ObjectMapper objectMapper_;

    @Injectable
    public OnyxSignedSessionManager(
            final StringSigner stringSigner,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper) {
        stringSigner_ = stringSigner;
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();
    }

    @Nullable
    @Override
    public String signSession(
            final Session session) {
        checkNotNull(session, "Session to sign cannot be null.");

        try {
            final String serializedSession = objectMapper_.writeValueAsString(session);
            return stringSigner_.sign(serializedSession);
        } catch (final Exception e) {
            LOG.warn("Failed to sign session: {}", session.getId(), e);
            return null;
        }
    }

    @Nullable
    @Override
    public Session extractSignedSession(
            final String signedSession) {
        checkNotNull(signedSession, "Signed session string cannot be null.");

        try {
            final String sessionString = stringSigner_.verifyAndGet(signedSession);
            if (sessionString == null) {
                LOG.warn("Failed to verify session from signed session string: {}", signedSession);
                return null;
            }

            final Session session = objectMapper_.readValue(sessionString, Session.class);

            final Instant now = Instant.now();
            if (now.isAfter(session.getExpiry())) {
                LOG.debug("Session expired: {}", session.getId());
                return null;
            }

            return session;
        } catch (final Exception e) {
            LOG.warn("Failed to get session from signed session string: {}", signedSession, e);
            return null;
        }
    }

}
