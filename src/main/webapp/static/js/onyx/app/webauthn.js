/* global ArrayBuffer, Uint8Array */
(function(parent, window, document) {
    'use strict';

    const

        // Namespace
        self = parent.WebAuthn = parent.WebAuthn || {},

        base64UrlToArrayBuffer = (base64Url) => {
            let base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const padding = base64.length % 4;
            if (padding) {
                base64 += '='.repeat(4 - padding);
            }
            const binary = window.atob(base64),
                buffer = new ArrayBuffer(binary.length),
                view = new Uint8Array(buffer);
            for (let ii = 0; ii < binary.length; ii++) {
                view[ii] = binary.charCodeAt(ii);
            }
            return buffer;
        },

        arrayBufferToBase64Url = (buffer) => {
            const bytes = new Uint8Array(buffer);
            let binary = '';
            for (let ii = 0; ii < bytes.byteLength; ii++) {
                binary += String.fromCharCode(bytes[ii]);
            }
            return window.btoa(binary)
                .replace(/\+/g, '-')
                .replace(/\//g, '_')
                .replace(/=+$/, '');
        },

        isAvailable = (callback) => {
            if (window.PublicKeyCredential
                    && window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable) {
                window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable()
                    .then((available) => {
                        available && callback();
                    });
            }
        },

        register = (baseApiUrl) => {
            const beginUrl = `${baseApiUrl}/v1/webauthn/register/begin`,
                finishUrl = `${baseApiUrl}/v1/webauthn/register/finish`;

            return $.post(beginUrl)
                .then((beginResponse) => {
                    const options = beginResponse.publicKeyCredentialCreationOptions,
                        requestId = beginResponse.requestId;

                    options.challenge = base64UrlToArrayBuffer(options.challenge);
                    options.user.id = base64UrlToArrayBuffer(options.user.id);

                    if (options.excludeCredentials) {
                        options.excludeCredentials = options.excludeCredentials.map((cred) => {
                            cred.id = base64UrlToArrayBuffer(cred.id);
                            return cred;
                        });
                    }

                    return navigator.credentials.create({publicKey: options})
                        .then((credential) => {
                            const credentialResponse = {
                                id: credential.id,
                                rawId: arrayBufferToBase64Url(credential.rawId),
                                type: credential.type,
                                response: {
                                    attestationObject: arrayBufferToBase64Url(
                                        credential.response.attestationObject),
                                    clientDataJSON: arrayBufferToBase64Url(
                                        credential.response.clientDataJSON)
                                }
                            };

                            if (credential.response.getTransports) {
                                credentialResponse.response.transports =
                                    credential.response.getTransports();
                            }

                            const clientExtensionResults = credential.getClientExtensionResults();
                            if (clientExtensionResults) {
                                credentialResponse.clientExtensionResults = clientExtensionResults;
                            }

                            return $.post(finishUrl, {
                                requestId: requestId,
                                credential: JSON.stringify(credentialResponse)
                            });
                        });
                });
        },

        authenticate = (baseApiUrl) => {
            const beginUrl = `${baseApiUrl}/v1/webauthn/login/begin`,
                finishUrl = `${baseApiUrl}/v1/webauthn/login/finish`;

            return $.post(beginUrl)
                .then((beginResponse) => {
                    const options = beginResponse.publicKeyCredentialRequestOptions,
                        requestId = beginResponse.requestId;

                    options.challenge = base64UrlToArrayBuffer(options.challenge);

                    if (options.allowCredentials) {
                        options.allowCredentials = options.allowCredentials.map((cred) => {
                            cred.id = base64UrlToArrayBuffer(cred.id);
                            return cred;
                        });
                    }

                    return navigator.credentials.get({publicKey: options})
                        .then((credential) => {
                            const credentialResponse = {
                                id: credential.id,
                                rawId: arrayBufferToBase64Url(credential.rawId),
                                type: credential.type,
                                response: {
                                    authenticatorData: arrayBufferToBase64Url(
                                        credential.response.authenticatorData),
                                    clientDataJSON: arrayBufferToBase64Url(
                                        credential.response.clientDataJSON),
                                    signature: arrayBufferToBase64Url(
                                        credential.response.signature)
                                }
                            };

                            if (credential.response.userHandle) {
                                credentialResponse.response.userHandle =
                                    arrayBufferToBase64Url(credential.response.userHandle);
                            }

                            const clientExtensionResults = credential.getClientExtensionResults();
                            if (clientExtensionResults) {
                                credentialResponse.clientExtensionResults = clientExtensionResults;
                            }

                            return $.post(finishUrl, {
                                requestId: requestId,
                                credential: JSON.stringify(credentialResponse)
                            });
                        })
                        .then((finishResponse) => {
                            if (finishResponse.success && finishResponse.redirectUrl) {
                                window.location.href = finishResponse.redirectUrl;
                            }
                            return finishResponse;
                        });
                });
        },

        init = () => {
            isAvailable(() => {
                const $webauthnLoginBtn = $('button#webauthn-login-btn');
                if ($webauthnLoginBtn.length) {
                    $webauthnLoginBtn.removeClass('d-none');
                    $webauthnLoginBtn.on('click', (e) => {
                        e.preventDefault();
                        authenticate(parent.baseApiUrl);
                    });
                }

                const $registerPasskey = $('a#webauthn-register-passkey');
                if ($registerPasskey.length) {
                    $registerPasskey.removeClass('d-none');
                    $registerPasskey.on('click', (e) => {
                        e.preventDefault();
                        register(parent.baseApiUrl);
                    });
                }
            });
        };

    init();

})(Onyx.App || {}, this, this.document);
