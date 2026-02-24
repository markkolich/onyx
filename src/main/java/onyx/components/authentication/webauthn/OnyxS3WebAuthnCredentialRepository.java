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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialType;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.storage.AssetManager;
import onyx.components.aws.s3.OnyxS3Client;
import onyx.components.config.aws.AwsConfig;
import onyx.entities.authentication.webauthn.WebAuthnCredential;
import onyx.entities.authentication.webauthn.WebAuthnUserCredential;
import onyx.entities.authentication.webauthn.WebAuthnUserHandle;
import onyx.exceptions.OnyxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public final class OnyxS3WebAuthnCredentialRepository implements WebAuthnCredentialRepository {

    private static final Logger LOG = LoggerFactory.getLogger(OnyxS3WebAuthnCredentialRepository.class);

    private static final String CREDENTIALS_USERNAME_S3_PREFIX =
            AssetManager.ONYX_METADATA_PATH_PREFIX + "/webauthn/username/";
    private static final String CREDENTIALS_USERHANDLE_S3_PREFIX =
            AssetManager.ONYX_METADATA_PATH_PREFIX + "/webauthn/user-handle/";
    private static final String CREDENTIALS_CREDENTIAL_ID_S3_PREFIX =
            AssetManager.ONYX_METADATA_PATH_PREFIX + "/webauthn/credential-id/";

    private static final String JSON_EXTENSION = ".json";

    private static final String APPLICATION_JSON = MediaType.JSON_UTF_8.toString();

    private final S3Client s3_;
    private final String bucketName_;

    private final ObjectMapper objectMapper_;

    @Injectable
    public OnyxS3WebAuthnCredentialRepository(
            final AwsConfig awsConfig,
            final OnyxS3Client onyxS3Client,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper) {
        s3_ = onyxS3Client.getS3Client();
        bucketName_ = awsConfig.getAwsS3BucketName();
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();
    }

    @Override
    public void addCredential(
            final String username,
            final RegisteredCredential credential) {
        final WebAuthnCredential newCredential = new WebAuthnCredential.Builder()
                .setCredentialId(credential.getCredentialId().getBase64Url())
                .setPublicKeyCose(credential.getPublicKeyCose().getBase64Url())
                .setSignatureCount(credential.getSignatureCount())
                .build();

        final WebAuthnUserCredential existingCredential = getCredentialForUser(username);

        final ImmutableList.Builder<WebAuthnCredential> credentialsBuilder =
                ImmutableList.builder();
        if (existingCredential != null) {
            credentialsBuilder.addAll(existingCredential.getCredentials());
        }
        credentialsBuilder.add(newCredential);

        final WebAuthnUserCredential updatedCredential = new WebAuthnUserCredential.Builder()
                .setUsername(username)
                .setUserHandle(credential.getUserHandle().getBase64Url())
                .setCredentials(credentialsBuilder.build())
                .build();
        putCredentialForUser(username, updatedCredential);

        final WebAuthnUserHandle userHandle = new WebAuthnUserHandle.Builder()
                .setUsername(username)
                .build();
        putUserHandle(credential.getUserHandle().getBase64Url(), userHandle);
        putUserHandleForCredentialId(credential.getCredentialId().getBase64Url(), userHandle);
    }

    @Override
    public void removeCredential(
            final ByteArray credentialId) {
        final String username = getUsernameForCredentialId(credentialId);
        if (username == null) {
            LOG.warn("Cannot remove credential: unknown credential ID: {}", credentialId);
            return;
        }

        LOG.info("Removing WebAuthn credential for user: {}", username);

        final WebAuthnUserCredential credential = getCredentialForUser(username);
        if (credential == null) {
            return;
        }

        final List<WebAuthnCredential> updatedCredentials = credential.getCredentials()
                .stream()
                .filter(e -> !e.getCredentialId().equals(credentialId.getBase64Url()))
                .collect(ImmutableList.toImmutableList());

        final WebAuthnUserCredential updatedCredential = new WebAuthnUserCredential.Builder()
                .setUsername(username)
                .setUserHandle(credential.getUserHandle())
                .setCredentials(updatedCredentials)
                .build();

        putCredentialForUser(username, updatedCredential);
    }

    @Override
    public void updateSignatureCount(
            final ByteArray credentialId,
            final long signatureCount) {
        final String username = getUsernameForCredentialId(credentialId);
        if (username == null) {
            LOG.warn("Cannot update signature count: unknown credential ID: {}", credentialId);
            return;
        }

        final WebAuthnUserCredential credential = getCredentialForUser(username);
        if (credential == null) {
            return;
        }

        final List<WebAuthnCredential> updatedCredentials = credential.getCredentials()
                .stream()
                .map(e -> {
                    if (e.getCredentialId().equals(credentialId.getBase64Url())) {
                        return new WebAuthnCredential.Builder()
                                .setCredentialId(e.getCredentialId())
                                .setPublicKeyCose(e.getPublicKeyCose())
                                .setSignatureCount(signatureCount)
                                .build();
                    }
                    return e;
                })
                .collect(ImmutableList.toImmutableList());

        final WebAuthnUserCredential updatedCredential = new WebAuthnUserCredential.Builder()
                .setUsername(username)
                .setUserHandle(credential.getUserHandle())
                .setCredentials(updatedCredentials)
                .build();

        putCredentialForUser(username, updatedCredential);
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(
            final String username) {
        final WebAuthnUserCredential credential = getCredentialForUser(username);
        if (credential == null) {
            return ImmutableSet.of();
        }

        return credential.getCredentials()
                .stream()
                .map(e -> PublicKeyCredentialDescriptor.builder()
                        .id(decodeBase64toByteArray(e.getCredentialId()))
                        .type(PublicKeyCredentialType.PUBLIC_KEY)
                        .build())
                .collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(
            final String username) {
        final WebAuthnUserCredential credential = getCredentialForUser(username);
        if (credential == null) {
            return Optional.empty();
        }

        return Optional.of(decodeBase64toByteArray(credential.getUserHandle()));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(
            final ByteArray userHandle) {
        final WebAuthnUserHandle userHandleFile = getUserHandle(userHandle.getBase64Url());
        if (userHandleFile == null) {
            return Optional.empty();
        }

        return Optional.of(userHandleFile.getUsername());
    }

    @Override
    public Optional<RegisteredCredential> lookup(
            final ByteArray credentialId,
            final ByteArray userHandle) {
        final Optional<String> username = getUsernameForUserHandle(userHandle);
        if (username.isEmpty()) {
            return Optional.empty();
        }

        final WebAuthnUserCredential credentialFile = getCredentialForUser(username.get());
        if (credentialFile == null) {
            return Optional.empty();
        }

        return credentialFile.getCredentials().stream()
                .filter(e -> e.getCredentialId().equals(credentialId.getBase64Url()))
                .map(e -> RegisteredCredential.builder()
                        .credentialId(decodeBase64toByteArray(e.getCredentialId()))
                        .userHandle(userHandle)
                        .publicKeyCose(decodeBase64toByteArray(e.getPublicKeyCose()))
                        .signatureCount(e.getSignatureCount())
                        .build())
                .findFirst();
    }

    @Override
    public Set<RegisteredCredential> lookupAll(
            final ByteArray credentialId) {
        final String username = getUsernameForCredentialId(credentialId);
        if (username == null) {
            return ImmutableSet.of();
        }

        final WebAuthnUserCredential credentialFile = getCredentialForUser(username);
        if (credentialFile == null) {
            return ImmutableSet.of();
        }

        final ByteArray userHandle = decodeBase64toByteArray(credentialFile.getUserHandle());

        return credentialFile.getCredentials().stream()
                .filter(e -> e.getCredentialId().equals(credentialId.getBase64Url()))
                .map(e -> RegisteredCredential.builder()
                        .credentialId(decodeBase64toByteArray(e.getCredentialId()))
                        .userHandle(userHandle)
                        .publicKeyCose(decodeBase64toByteArray(e.getPublicKeyCose()))
                        .signatureCount(e.getSignatureCount())
                        .build())
                .collect(ImmutableSet.toImmutableSet());
    }

    // S3 helpers

    @Nullable
    private String getUsernameForCredentialId(
            final ByteArray credentialId) {
        final WebAuthnUserHandle userHandle = getUserHandleForCredentialId(credentialId.getBase64Url());
        if (userHandle == null) {
            return null;
        }

        return userHandle.getUsername();
    }

    @Nullable
    private WebAuthnUserCredential getCredentialForUser(
            final String username) {
        return getJsonObject(CREDENTIALS_USERNAME_S3_PREFIX + username + JSON_EXTENSION,
                WebAuthnUserCredential.class);
    }

    private void putCredentialForUser(
            final String username,
            final WebAuthnUserCredential credential) {
        try {
            final String json = objectMapper_.writeValueAsString(credential);
            putJsonObject(CREDENTIALS_USERNAME_S3_PREFIX + username + JSON_EXTENSION,
                    json);
        } catch (final Exception e) {
            throw new OnyxException("Failed to save credential for user: " + username, e);
        }
    }

    @Nullable
    private WebAuthnUserHandle getUserHandle(
            final String userHandleBase64Url) {
        return getJsonObject(CREDENTIALS_USERHANDLE_S3_PREFIX + userHandleBase64Url + JSON_EXTENSION,
                WebAuthnUserHandle.class);
    }

    private void putUserHandle(
            final String userHandleBase64Url,
            final WebAuthnUserHandle userHandle) {
        try {
            final String json = objectMapper_.writeValueAsString(userHandle);
            putJsonObject(CREDENTIALS_USERHANDLE_S3_PREFIX + userHandleBase64Url + JSON_EXTENSION,
                    json);
        } catch (final Exception e) {
            throw new OnyxException("Failed to save user handle object", e);
        }
    }

    @Nullable
    private WebAuthnUserHandle getUserHandleForCredentialId(
            final String credentialIdBase64Url) {
        return getJsonObject(CREDENTIALS_CREDENTIAL_ID_S3_PREFIX + credentialIdBase64Url + JSON_EXTENSION,
                WebAuthnUserHandle.class);
    }

    private void putUserHandleForCredentialId(
            final String credentialIdBase64Url,
            final WebAuthnUserHandle userHandle) {
        try {
            final String json = objectMapper_.writeValueAsString(userHandle);
            putJsonObject(CREDENTIALS_CREDENTIAL_ID_S3_PREFIX + credentialIdBase64Url + JSON_EXTENSION,
                    json);
        } catch (final Exception e) {
            throw new OnyxException("Failed to save credential ID object", e);
        }
    }

    @Nullable
    private <T> T getJsonObject(
            final String key,
            final Class<T> type) {
        try {
            final GetObjectRequest gor = GetObjectRequest.builder()
                    .bucket(bucketName_)
                    .key(key)
                    .build();
            final byte[] bytes = s3_.getObjectAsBytes(gor).asByteArray();
            return objectMapper_.readValue(bytes, type);
        } catch (final NoSuchKeyException e) {
            return null;
        } catch (final Exception e) {
            LOG.warn("Failed to read S3 object: {}", key, e);
            return null;
        }
    }

    private void putJsonObject(
            final String key,
            final String value) {
        try {
            final PutObjectRequest por = PutObjectRequest.builder()
                    .bucket(bucketName_)
                    .key(key)
                    .contentType(APPLICATION_JSON)
                    .build();
            s3_.putObject(por, RequestBody.fromString(value));
        } catch (final Exception e) {
            LOG.warn("Failed to save S3 object: {}", key, e);
        }
    }

    private static ByteArray decodeBase64toByteArray(
            final String base64Url) {
        try {
            return ByteArray.fromBase64Url(base64Url);
        } catch (final Exception e) {
            throw new OnyxException("Failed to decode base-64 URL string: "
                    + base64Url, e);
        }
    }

}
