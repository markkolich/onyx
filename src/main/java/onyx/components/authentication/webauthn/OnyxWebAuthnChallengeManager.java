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

package onyx.components.authentication.webauthn;

import com.fasterxml.jackson.databind.ObjectMapper;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.security.StringSigner;
import onyx.entities.api.request.v1.webauthn.WebAuthnAuthenticationRequest;
import onyx.entities.api.request.v1.webauthn.WebAuthnRegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Instant;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public final class OnyxWebAuthnChallengeManager implements WebAuthnChallengeManager {

    private static final Logger LOG = LoggerFactory.getLogger(OnyxWebAuthnChallengeManager.class);

    private final StringSigner stringSigner_;

    private final ObjectMapper objectMapper_;

    @Injectable
    public OnyxWebAuthnChallengeManager(
            final StringSigner stringSigner,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper) {
        stringSigner_ = stringSigner;
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();
    }

    @Nullable
    @Override
    public String signRegistrationRequest(
            final WebAuthnRegistrationRequest request) {
        checkNotNull(request, "WebAuthn registration request cannot be null.");

        try {
            final String serializedRequest = objectMapper_.writeValueAsString(request);
            return stringSigner_.sign(serializedRequest);
        } catch (final Exception e) {
            LOG.warn("Failed to sign WebAuthn registration request: {}", request.getId(), e);
            return null;
        }
    }

    @Nullable
    @Override
    public WebAuthnRegistrationRequest extractRegistrationRequest(
            final String signedRequest) {
        checkNotNull(signedRequest, "Signed WebAuthn registration request cannot be null.");

        try {
            final String requestString = stringSigner_.verifyAndGet(signedRequest);
            if (requestString == null) {
                LOG.warn("Failed to verify WebAuthn registration request from signed string: {}",
                        signedRequest);
                return null;
            }

            final WebAuthnRegistrationRequest request =
                    objectMapper_.readValue(requestString, WebAuthnRegistrationRequest.class);

            final Instant now = Instant.now();
            if (now.isAfter(request.getExpiry())) {
                LOG.debug("WebAuthn registration request expired: {}", request.getId());
                return null;
            }

            return request;
        } catch (final Exception e) {
            LOG.warn("Failed to extract WebAuthn registration request from signed string: {}",
                    signedRequest, e);
            return null;
        }
    }

    @Nullable
    @Override
    public String signAuthenticationRequest(
            final WebAuthnAuthenticationRequest request) {
        checkNotNull(request, "WebAuthn authentication request cannot be null.");

        try {
            final String serializedRequest = objectMapper_.writeValueAsString(request);
            return stringSigner_.sign(serializedRequest);
        } catch (final Exception e) {
            LOG.warn("Failed to sign WebAuthn authentication request: {}", request.getId(), e);
            return null;
        }
    }

    @Nullable
    @Override
    public WebAuthnAuthenticationRequest extractAuthenticationRequest(
            final String signedRequest) {
        checkNotNull(signedRequest, "Signed WebAuthn authentication request cannot be null.");

        try {
            final String requestString = stringSigner_.verifyAndGet(signedRequest);
            if (requestString == null) {
                LOG.warn("Failed to verify WebAuthn authentication request from signed string: {}",
                        signedRequest);
                return null;
            }

            final WebAuthnAuthenticationRequest request =
                    objectMapper_.readValue(requestString, WebAuthnAuthenticationRequest.class);

            final Instant now = Instant.now();
            if (now.isAfter(request.getExpiry())) {
                LOG.debug("WebAuthn authentication request expired: {}", request.getId());
                return null;
            }

            return request;
        } catch (final Exception e) {
            LOG.warn("Failed to extract WebAuthn authentication request from signed string: {}",
                    signedRequest, e);
            return null;
        }
    }

}
