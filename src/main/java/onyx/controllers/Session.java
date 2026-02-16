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

package onyx.controllers;

import com.google.common.primitives.Ints;
import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.RequestBody;
import curacao.annotations.parameters.convenience.Cookie;
import curacao.core.servlet.AsyncContext;
import curacao.core.servlet.HttpResponse;
import onyx.components.authentication.CookieManager;
import onyx.components.authentication.SessionManager;
import onyx.components.authentication.UserAuthenticator;
import onyx.components.authentication.twofactor.TwoFactorAuthCodeManager;
import onyx.components.authentication.twofactor.TwoFactorAuthTokenManager;
import onyx.components.config.OnyxConfig;
import onyx.components.config.authentication.twofactor.TwoFactorAuthConfig;
import onyx.components.security.StringSigner;
import onyx.components.storage.ResourceManager;
import onyx.entities.authentication.User;
import onyx.entities.authentication.twofactor.TrustedDeviceToken;
import onyx.entities.authentication.twofactor.TwoFactorAuthToken;
import onyx.entities.freemarker.FreeMarkerContent;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import static curacao.annotations.RequestMapping.Method.POST;
import static onyx.components.authentication.CookieManager.RETURN_TO_COOKIE_NAME;
import static onyx.components.authentication.CookieManager.SESSION_COOKIE_NAME;
import static onyx.components.authentication.CookieManager.TRUSTED_DEVICE_COOKIE_NAME;
import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;

@Controller
public final class Session extends AbstractOnyxFreeMarkerController {

    private static final Logger LOG = LoggerFactory.getLogger(Session.class);

    private static final String USERNAME_FIELD = "username";
    private static final String PASSWORD_FIELD = "password";
    private static final String TOKEN_FIELD = "token";
    private static final String CODE_FIELD = "code";
    private static final String TRUST_DEVICE_FIELD = "trustDevice";

    private final CookieManager cookieManager_;
    private final SessionManager sessionManager_;

    private final UserAuthenticator userAuthenticator_;

    private final TwoFactorAuthConfig twoFactorAuthConfig_;
    private final TwoFactorAuthTokenManager twoFactorAuthTokenManager_;
    private final TwoFactorAuthCodeManager twoFactorAuthCodeManager_;

    private final StringSigner stringSigner_;

    @Injectable
    public Session(
            final OnyxConfig onyxConfig,
            final ResourceManager resourceManager,
            final CookieManager cookieManager,
            final SessionManager sessionManager,
            final UserAuthenticator userAuthenticator,
            final TwoFactorAuthConfig twoFactorAuthConfig,
            final TwoFactorAuthTokenManager twoFactorAuthTokenManager,
            final TwoFactorAuthCodeManager twoFactorAuthCodeManager,
            final StringSigner stringSigner) {
        super(onyxConfig, resourceManager);
        cookieManager_ = cookieManager;
        sessionManager_ = sessionManager;
        userAuthenticator_ = userAuthenticator;
        twoFactorAuthConfig_ = twoFactorAuthConfig;
        twoFactorAuthTokenManager_ = twoFactorAuthTokenManager;
        twoFactorAuthCodeManager_ = twoFactorAuthCodeManager;
        stringSigner_ = stringSigner;
    }

    @RequestMapping("^/login$")
    public FreeMarkerContent login() {
        return new FreeMarkerContent.Builder("templates/login.ftl")
                .build();
    }

    @RequestMapping(value = "^/login$", methods = POST)
    public FreeMarkerContent doLogin(
            @Cookie(TRUSTED_DEVICE_COOKIE_NAME) final String trustedDeviceCookie,
            @Cookie(RETURN_TO_COOKIE_NAME) final String returnToCookie,
            @RequestBody(USERNAME_FIELD) final String username,
            @RequestBody(PASSWORD_FIELD) final String password,
            final HttpResponse response,
            final AsyncContext context) throws Exception {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return login();
        }

        final Pair<User, onyx.entities.authentication.Session> sessionPair =
                userAuthenticator_.getSessionForCredentials(username, password);
        if (sessionPair == null) {
            return login();
        }

        final User user = sessionPair.getLeft();
        final onyx.entities.authentication.Session session = sessionPair.getRight();

        final boolean twoFactorAuthEnabled = twoFactorAuthConfig_.twoFactorAuthEnabled();
        if (!twoFactorAuthEnabled) {
            // If 2FA is not enabled, process the login and let the user in.
            processLogin(session, returnToCookie, response, context);
            return null;
        }

        // Check for the presence of a trusted device token. If a trusted device token
        // is present and valid, then there is no need to prompt the user for 2FA - process
        // the login and let the user in.
        if (StringUtils.isNotBlank(trustedDeviceCookie)) {
            final TrustedDeviceToken trustedDeviceToken =
                    twoFactorAuthTokenManager_.extractTrustedDeviceToken(trustedDeviceCookie);
            if (trustedDeviceToken == null) {
                LOG.warn("Failed to validate trusted device token from cookie: {}",
                        trustedDeviceCookie);
            } else if (user.getUsername().equals(trustedDeviceToken.getUsername())) {
                // Validate that the username on the 2FA trusted device token matches
                // that of the user attempting to authenticate.
                processLogin(session, returnToCookie, response, context);
                return null;
            }
        }

        return processTwoFactorLogin(user, session);
    }

    @RequestMapping(value = "^/login/verify$", methods = POST)
    public void doLoginVerify(
            @Cookie(RETURN_TO_COOKIE_NAME) final String returnToCookie,
            @RequestBody(TOKEN_FIELD) final String signedToken,
            @RequestBody(CODE_FIELD) final String code,
            @RequestBody(TRUST_DEVICE_FIELD) final String trustDevice,
            final HttpResponse response,
            final AsyncContext context) throws Exception {
        final boolean twoFactorAuthEnabled = twoFactorAuthConfig_.twoFactorAuthEnabled();
        if (!twoFactorAuthEnabled) {
            logout(response, context);
            return;
        } else if (StringUtils.isBlank(signedToken) || StringUtils.isBlank(code)) {
            logout(response, context);
            return;
        }

        // Extract the signed 2FA token on the request.
        final TwoFactorAuthToken token = twoFactorAuthTokenManager_.extractSignedToken(signedToken);
        if (token == null) {
            LOG.warn("Failed to extract valid 2FA token from request: {}", signedToken);
            logout(response, context);
            return;
        }

        final onyx.entities.authentication.Session session = token.getSession();
        final String username = session.getUsername();

        final String generatedTokenHash = twoFactorAuthTokenManager_.generateTokenHash(username, code);
        // Use constant-time comparison to prevent timing attacks
        final boolean hashesMatch = MessageDigest.isEqual(
                getBytesUtf8(token.getHash()),
                getBytesUtf8(generatedTokenHash));
        if (!hashesMatch) {
            LOG.warn("2FA token hash mismatch for user: {}", username);
            logout(response, context);
            return;
        }

        final boolean trustThisDevice = StringUtils.isNotBlank(trustDevice);
        if (trustThisDevice) {
            final long trustedDeviceTokenDurationInSeconds =
                    twoFactorAuthConfig_.getTrustedDeviceTokenDuration(TimeUnit.SECONDS);
            final Instant trustedDeviceTokenExpiry =
                    Instant.now().plusSeconds(trustedDeviceTokenDurationInSeconds);

            final TrustedDeviceToken trustedDeviceToken = new TrustedDeviceToken.Builder()
                    .setId(UUID.randomUUID().toString())
                    .setUsername(session.getUsername())
                    .setExpiry(trustedDeviceTokenExpiry)
                    .build();
            final String signedTrustedDeviceToken =
                    twoFactorAuthTokenManager_.signTrustedDeviceToken(trustedDeviceToken);

            final long trustedDeviceTokenCookieMaxAgeInSeconds =
                    twoFactorAuthConfig_.getTrustedDeviceTokenCookieMaxAge(TimeUnit.SECONDS);

            cookieManager_.setCookie(TRUSTED_DEVICE_COOKIE_NAME, signedTrustedDeviceToken,
                    Ints.checkedCast(trustedDeviceTokenCookieMaxAgeInSeconds), response);
        }

        // If we get here, then the provided 2FA verification code was valid and the
        // session extracted from the 2FA token is good to go - let the user in!
        processLogin(session, returnToCookie, response, context);
    }

    @RequestMapping("^/logout$")
    public void logout(
            final HttpResponse response,
            final AsyncContext context) throws Exception {
        processLogout(response, context);
    }

    private void processLogin(
            final onyx.entities.authentication.Session session,
            @Nullable final String returnToCookie,
            final HttpResponse response,
            final AsyncContext context) throws Exception {
        final String signedSession = sessionManager_.signSession(session);
        cookieManager_.setCookie(SESSION_COOKIE_NAME, signedSession, response);

        cookieManager_.clearCookie(RETURN_TO_COOKIE_NAME, response);

        String redirectTo = onyxConfig_.getViewSafeFullUri() + "/browse/" + session.getUsername();
        if (StringUtils.isNotBlank(returnToCookie)) {
            final String verifiedReturnTo = stringSigner_.verifyAndGet(returnToCookie);
            if (verifiedReturnTo != null && verifiedReturnTo.startsWith("/")) {
                redirectTo = onyxConfig_.getViewSafeFullUri() + verifiedReturnTo;
            }
        }

        response.sendRedirect(redirectTo);
        context.complete();
    }

    private FreeMarkerContent processTwoFactorLogin(
            final User user,
            final onyx.entities.authentication.Session session) {
        final int randomCodeLength = twoFactorAuthConfig_.getRandomCodeLength();
        final String randomCode = RandomStringUtils.secure().nextNumeric(randomCodeLength);

        final String tokenHash = twoFactorAuthTokenManager_.generateTokenHash(user.getUsername(), randomCode);

        final long tokenDurationInSeconds =
                twoFactorAuthConfig_.getTokenDuration(TimeUnit.SECONDS);
        final Instant tokenExpiry =
                Instant.now().plusSeconds(tokenDurationInSeconds);
        final TwoFactorAuthToken token = new TwoFactorAuthToken.Builder()
                .setId(UUID.randomUUID().toString())
                .setHash(tokenHash)
                .setSession(session)
                .setExpiry(tokenExpiry)
                .build();

        // Send 2FA verification code!
        twoFactorAuthCodeManager_.sendVerificationCodeToUser(user, randomCode);

        final String signedToken = twoFactorAuthTokenManager_.signToken(token);
        return new FreeMarkerContent.Builder("templates/login-verification.ftl")
                .withAttr(TOKEN_FIELD, signedToken)
                .build();
    }

    private void processLogout(
            final HttpResponse response,
            final AsyncContext context) throws Exception {
        cookieManager_.clearCookie(SESSION_COOKIE_NAME, response);
        cookieManager_.clearCookie(RETURN_TO_COOKIE_NAME, response);

        final boolean twoFactorAuthEnabled = twoFactorAuthConfig_.twoFactorAuthEnabled();
        if (twoFactorAuthEnabled) {
            cookieManager_.clearCookie(TRUSTED_DEVICE_COOKIE_NAME, response);
        }

        response.sendRedirect(onyxConfig_.getViewSafeFullUri());
        context.complete();
    }

}
