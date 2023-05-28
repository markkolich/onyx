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

package onyx.components.authentication.twofactor;

import com.fasterxml.jackson.databind.ObjectMapper;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.security.StringSigner;
import onyx.entities.authentication.twofactor.TrustedDeviceToken;
import onyx.entities.authentication.twofactor.TwoFactorAuthToken;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Instant;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public final class OnyxTwoFactorAuthTokenManager implements TwoFactorAuthTokenManager {

    private static final Logger LOG = LoggerFactory.getLogger(OnyxTwoFactorAuthTokenManager.class);

    private final StringSigner stringSigner_;

    private final ObjectMapper objectMapper_;

    @Injectable
    public OnyxTwoFactorAuthTokenManager(
            final StringSigner stringSigner,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper) {
        stringSigner_ = stringSigner;
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();
    }

    @Nullable
    @Override
    public String signToken(
            final TwoFactorAuthToken token) {
        checkNotNull(token, "2FA token cannot be null.");

        try {
            final String serializedToken = objectMapper_.writeValueAsString(token);
            return stringSigner_.sign(serializedToken);
        } catch (final Exception e) {
            LOG.warn("Failed to sign 2FA token for session ID: {}", token.getSession().getId(), e);
            return null;
        }
    }

    @Nullable
    @Override
    public String signTrustedDeviceToken(
            final TrustedDeviceToken trustedDeviceToken) {
        checkNotNull(trustedDeviceToken, "Trusted device token cannot be null.");

        try {
            final String serializedTrustedDeviceToken = objectMapper_.writeValueAsString(trustedDeviceToken);
            return stringSigner_.sign(serializedTrustedDeviceToken);
        } catch (final Exception e) {
            LOG.warn("Failed to sign trusted device token by ID: {}", trustedDeviceToken.getId(), e);
            return null;
        }
    }

    @Nullable
    @Override
    public TwoFactorAuthToken extractSignedToken(
            final String signedToken) {
        checkNotNull(signedToken, "Signed 2FA token string cannot be null.");

        try {
            final String tokenString = stringSigner_.verifyAndGet(signedToken);
            final TwoFactorAuthToken token =
                    objectMapper_.readValue(tokenString, TwoFactorAuthToken.class);

            final Instant now = Instant.now();
            if (now.isAfter(token.getExpiry())) {
                LOG.debug("2FA token expired for session ID: {}", token.getSession().getId());
                return null;
            }

            return token;
        } catch (final Exception e) {
            LOG.warn("Failed to get 2FA token from signed token string: {}", signedToken, e);
            return null;
        }
    }

    @Nullable
    @Override
    public TrustedDeviceToken extractTrustedDeviceToken(
            final String signedTrustedDeviceToken) {
        checkNotNull(signedTrustedDeviceToken, "Trusted device token cannot be null.");

        try {
            final String trustedDeviceString =
                    stringSigner_.verifyAndGet(signedTrustedDeviceToken);
            final TrustedDeviceToken trustedDeviceToken =
                    objectMapper_.readValue(trustedDeviceString, TrustedDeviceToken.class);

            final Instant now = Instant.now();
            if (now.isAfter(trustedDeviceToken.getExpiry())) {
                LOG.debug("Trusted device token expired for token ID: {}",
                        trustedDeviceToken.getId());
                return null;
            }

            return trustedDeviceToken;
        } catch (final Exception e) {
            LOG.warn("Failed to get trusted device token from signed string: {}",
                    signedTrustedDeviceToken, e);
            return null;
        }
    }

    @Override
    public String generateTokenHash(
            final String username,
            final String code) {
        checkNotNull(username, "2FA username cannot be null.");
        checkNotNull(code, "2FA verification code cannot be null.");

        return DigestUtils.sha512Hex(String.format("%s_%s", username, code));
    }

}
