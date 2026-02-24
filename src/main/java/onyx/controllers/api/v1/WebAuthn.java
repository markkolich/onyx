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

package onyx.controllers.api.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.RequestBody;
import curacao.annotations.parameters.convenience.Cookie;
import curacao.core.servlet.HttpResponse;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.authentication.SessionManager;
import onyx.components.authentication.UserAuthenticator;
import onyx.components.authentication.webauthn.WebAuthnCredentialRepository;
import onyx.components.authentication.webauthn.WebAuthnChallengeManager;
import onyx.components.config.OnyxConfig;
import onyx.components.config.authentication.webauthn.WebAuthnConfig;
import onyx.controllers.api.AbstractOnyxApiController;
import onyx.entities.authentication.Session;
import onyx.entities.api.request.v1.webauthn.WebAuthnAuthenticationRequest;
import onyx.entities.api.request.v1.webauthn.WebAuthnRegistrationRequest;
import onyx.entities.api.response.v1.webauthn.WebAuthnLoginBeginResponse;
import onyx.entities.api.response.v1.webauthn.WebAuthnLoginFinishResponse;
import onyx.entities.api.response.v1.webauthn.WebAuthnRegisterBeginResponse;
import onyx.entities.api.response.v1.webauthn.WebAuthnRegisterFinishResponse;
import onyx.exceptions.api.ApiBadRequestException;
import onyx.exceptions.api.ApiForbiddenException;
import onyx.exceptions.api.ApiUnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.RandomUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static curacao.annotations.RequestMapping.Method.POST;
import static onyx.components.authentication.CookieManager.RETURN_TO_COOKIE_NAME;

@Controller
public final class WebAuthn extends AbstractOnyxApiController {

    private static final Logger LOG = LoggerFactory.getLogger(WebAuthn.class);

    private static final String REQUEST_ID_FIELD = "requestId";
    private static final String CREDENTIAL_FIELD = "credential";

    private static final String PUBLIC_KEY = "publicKey";

    private final WebAuthnConfig webAuthnConfig_;
    private final WebAuthnChallengeManager challengeManager_;
    private final WebAuthnCredentialRepository webAuthnCredentialRepository_;

    private final UserAuthenticator userAuthenticator_;
    private final SessionManager sessionManager_;

    private final ObjectMapper objectMapper_;

    private final RelyingParty relyingParty_;

    @Injectable
    public WebAuthn(
            final OnyxConfig onyxConfig,
            final WebAuthnConfig webAuthnConfig,
            final WebAuthnChallengeManager challengeManager,
            final WebAuthnCredentialRepository webAuthnCredentialRepository,
            final UserAuthenticator userAuthenticator,
            final SessionManager sessionManager,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper) {
        super(onyxConfig);
        webAuthnConfig_ = webAuthnConfig;
        challengeManager_ = challengeManager;
        webAuthnCredentialRepository_ = webAuthnCredentialRepository;
        userAuthenticator_ = userAuthenticator;
        sessionManager_ = sessionManager;
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();

        final RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                .id(webAuthnConfig.getRpId())
                .name(webAuthnConfig.getRpName())
                .build();

        relyingParty_ = RelyingParty.builder()
                .identity(rpIdentity)
                .credentialRepository(webAuthnCredentialRepository)
                .origins(Set.of(webAuthnConfig.getRpOrigin()))
                .build();
    }

    @RequestMapping(value = "^/api/v1/webauthn/register/begin$", methods = POST)
    public WebAuthnRegisterBeginResponse registerBegin(
            final Session session) throws Exception {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        }

        if (!webAuthnConfig_.isWebAuthnEnabled()) {
            throw new ApiBadRequestException("WebAuthn is not enabled.");
        }

        final String username = session.getUsername();

        final Optional<ByteArray> existingUserHandle =
                webAuthnCredentialRepository_.getUserHandleForUsername(username);
        final ByteArray userHandle = existingUserHandle.orElseGet(
                () -> new ByteArray(RandomUtils.secure().randomBytes(32)));

        final UserIdentity userIdentity = UserIdentity.builder()
                .name(username)
                .displayName(username)
                .id(userHandle)
                .build();

        final AuthenticatorSelectionCriteria authenticatorSelection =
                AuthenticatorSelectionCriteria.builder()
                .residentKey(ResidentKeyRequirement.REQUIRED)
                .build();

        final StartRegistrationOptions startRegOptions = StartRegistrationOptions.builder()
                .user(userIdentity)
                .authenticatorSelection(authenticatorSelection)
                .build();
        final PublicKeyCredentialCreationOptions creationOptions =
                relyingParty_.startRegistration(startRegOptions);

        final long challengeTimeoutInSeconds =
                webAuthnConfig_.getChallengeTimeout(TimeUnit.SECONDS);
        final Instant expiry = Instant.now().plusSeconds(challengeTimeoutInSeconds);

        final WebAuthnRegistrationRequest registrationRequest =
                new WebAuthnRegistrationRequest.Builder()
                .setId(UUID.randomUUID().toString())
                .setUsername(username)
                .setChallenge(creationOptions.getChallenge().getBase64Url())
                .setUserHandle(userHandle.getBase64Url())
                .setCreationOptionsJson(creationOptions.toJson())
                .setExpiry(expiry)
                .build();

        final String signedRequest = challengeManager_.signRegistrationRequest(registrationRequest);
        if (signedRequest == null) {
            throw new ApiBadRequestException("Failed to sign WebAuthn registration request.");
        }

        final String creationOptionsJson = creationOptions.toCredentialsCreateJson();
        final JsonNode creationOptionsNode = objectMapper_.readTree(creationOptionsJson)
                .get(PUBLIC_KEY);

        return new WebAuthnRegisterBeginResponse.Builder(objectMapper_)
                .setRequestId(signedRequest)
                .setPublicKeyCredentialCreationOptions(creationOptionsNode)
                .build();
    }

    @RequestMapping(value = "^/api/v1/webauthn/register/finish$", methods = POST)
    public WebAuthnRegisterFinishResponse registerFinish(
            @RequestBody(REQUEST_ID_FIELD) final String requestId,
            @RequestBody(CREDENTIAL_FIELD) final String credentialJson,
            final Session session) throws Exception {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        }

        if (!webAuthnConfig_.isWebAuthnEnabled()) {
            throw new ApiBadRequestException("WebAuthn is not enabled.");
        }

        if (requestId == null || credentialJson == null) {
            throw new ApiBadRequestException("Missing required fields.");
        }

        final WebAuthnRegistrationRequest registrationRequest =
                challengeManager_.extractRegistrationRequest(requestId);
        if (registrationRequest == null) {
            throw new ApiBadRequestException("Invalid or expired registration request.");
        }

        if (!session.getUsername().equals(registrationRequest.getUsername())) {
            throw new ApiForbiddenException("Session username does not match registration request.");
        }

        final ByteArray userHandle = ByteArray.fromBase64Url(registrationRequest.getUserHandle());

        final PublicKeyCredentialCreationOptions creationOptions =
                PublicKeyCredentialCreationOptions.fromJson(registrationRequest.getCreationOptionsJson());

        final PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc =
                PublicKeyCredential.parseRegistrationResponseJson(credentialJson);

        try {
            final FinishRegistrationOptions finishRegOptions = FinishRegistrationOptions.builder()
                    .request(creationOptions)
                    .response(pkc)
                    .build();
            final RegistrationResult registrationResult =
                    relyingParty_.finishRegistration(finishRegOptions);

            final RegisteredCredential registeredCredential = RegisteredCredential.builder()
                    .credentialId(registrationResult.getKeyId().getId())
                    .userHandle(userHandle)
                    .publicKeyCose(registrationResult.getPublicKeyCose())
                    .signatureCount(registrationResult.getSignatureCount())
                    .build();

            webAuthnCredentialRepository_.addCredential(session.getUsername(), registeredCredential);

            return new WebAuthnRegisterFinishResponse.Builder(objectMapper_)
                    .setSuccess(true)
                    .build();
        } catch (final Exception e) {
            LOG.warn("WebAuthn registration failed for user: {}", session.getUsername(), e);
            throw new ApiBadRequestException("WebAuthn registration verification failed.");
        }
    }

    @RequestMapping(value = "^/api/v1/webauthn/login/begin$", methods = POST)
    public WebAuthnLoginBeginResponse loginBegin() throws Exception {
        if (!webAuthnConfig_.isWebAuthnEnabled()) {
            throw new ApiBadRequestException("WebAuthn is not enabled.");
        }

        // No username required - the authenticator will present available resident credentials
        // (discoverable passkeys) for this origin and the user picks one. The userHandle in the
        // assertion response maps back to the username.
        final StartAssertionOptions startAssertionOptions =
                StartAssertionOptions.builder().build();
        final AssertionRequest assertionRequest =
                relyingParty_.startAssertion(startAssertionOptions);

        final long challengeTimeoutInSeconds =
                webAuthnConfig_.getChallengeTimeout(TimeUnit.SECONDS);
        final Instant expiry = Instant.now().plusSeconds(challengeTimeoutInSeconds);

        final ByteArray challenge =
                assertionRequest.getPublicKeyCredentialRequestOptions().getChallenge();

        final WebAuthnAuthenticationRequest authenticationRequest =
                new WebAuthnAuthenticationRequest.Builder()
                .setId(UUID.randomUUID().toString())
                .setChallenge(challenge.getBase64Url())
                .setAssertionRequestJson(assertionRequest.toJson())
                .setExpiry(expiry)
                .build();

        final String signedRequest =
                challengeManager_.signAuthenticationRequest(authenticationRequest);
        if (signedRequest == null) {
            throw new ApiBadRequestException("Failed to sign WebAuthn authentication request.");
        }

        final String assertionRequestJson = assertionRequest.toCredentialsGetJson();
        final JsonNode assertionOptionsNode = objectMapper_.readTree(assertionRequestJson)
                .get(PUBLIC_KEY);

        return new WebAuthnLoginBeginResponse.Builder(objectMapper_)
                .setRequestId(signedRequest)
                .setPublicKeyCredentialRequestOptions(assertionOptionsNode)
                .build();
    }

    @RequestMapping(value = "^/api/v1/webauthn/login/finish$", methods = POST)
    public WebAuthnLoginFinishResponse loginFinish(
            @RequestBody(REQUEST_ID_FIELD) final String requestId,
            @RequestBody(CREDENTIAL_FIELD) final String credentialJson,
            @Cookie(RETURN_TO_COOKIE_NAME) final String returnToCookie,
            final HttpResponse response) throws Exception {
        if (!webAuthnConfig_.isWebAuthnEnabled()) {
            throw new ApiBadRequestException("WebAuthn is not enabled.");
        }

        if (requestId == null || credentialJson == null) {
            throw new ApiBadRequestException("Missing required fields.");
        }

        final WebAuthnAuthenticationRequest authenticationRequest =
                challengeManager_.extractAuthenticationRequest(requestId);
        if (authenticationRequest == null) {
            throw new ApiBadRequestException("Invalid or expired authentication request.");
        }

        final AssertionRequest assertionRequest =
                AssertionRequest.fromJson(authenticationRequest.getAssertionRequestJson());

        final PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
                PublicKeyCredential.parseAssertionResponseJson(credentialJson);

        try {
            final FinishAssertionOptions finishAssertionOptions = FinishAssertionOptions.builder()
                    .request(assertionRequest)
                    .response(pkc)
                    .build();
            final AssertionResult assertionResult =
                    relyingParty_.finishAssertion(finishAssertionOptions);

            if (!assertionResult.isSuccess()) {
                throw new ApiBadRequestException("WebAuthn authentication verification failed.");
            }

            webAuthnCredentialRepository_.updateSignatureCount(
                    assertionResult.getCredential().getCredentialId(),
                    assertionResult.getSignatureCount());

            final String username = assertionResult.getUsername();
            final Session session = userAuthenticator_.getSessionForUsername(Session.Type.USER, username);

            final String redirectUrl = sessionManager_.processLogin(
                    session, returnToCookie, response);

            return new WebAuthnLoginFinishResponse.Builder(objectMapper_)
                    .setSuccess(true)
                    .setRedirectUrl(redirectUrl)
                    .build();
        } catch (final Exception e) {
            LOG.warn("WebAuthn authentication failed.", e);
            throw new ApiBadRequestException("WebAuthn authentication verification failed.");
        }
    }

}
