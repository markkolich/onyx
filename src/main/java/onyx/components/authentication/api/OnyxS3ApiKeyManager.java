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

package onyx.components.authentication.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.authentication.UserAuthenticator;
import onyx.components.aws.s3.OnyxS3Client;
import onyx.components.config.aws.AwsConfig;
import onyx.components.security.StringSigner;
import onyx.components.storage.AssetManager;
import onyx.entities.authentication.ApiKeyCredential;
import onyx.entities.authentication.Session;
import onyx.entities.authentication.Session.Type;
import onyx.exceptions.OnyxException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An {@link ApiKeyManager} implementation backed by API key credentials stored
 * in S3, keyed by a SHA-256 hash of the raw API key value. Each lookup performs a
 * direct, uncached S3 GetObject; there is deliberately no in-memory index or refresh
 * job fronting this component, which keeps a revoked key from working again the
 * moment its backing S3 object is deleted.
 *
 * <p>Every generated key is also signed with {@link StringSigner} before being
 * returned. That signature is never consulted by the S3 lookup itself &mdash; the
 * S3 object's existence remains the sole authority on whether a key is valid, which
 * is what preserves instant revocation. Instead, {@link #getSessionForApiKey(String)}
 * verifies the signature first, as a cheap, in-memory rejection of malformed or
 * never-issued-by-us input before spending an S3 GetObject on it.
 */
@Component
public final class OnyxS3ApiKeyManager implements ApiKeyManager {

    private static final Logger LOG = LoggerFactory.getLogger(OnyxS3ApiKeyManager.class);

    private static final String API_KEYS_S3_PREFIX =
            AssetManager.ONYX_METADATA_PATH_PREFIX + "/apikeys/";
    private static final String API_KEYS_USERNAME_S3_PREFIX =
            AssetManager.ONYX_METADATA_PATH_PREFIX + "/apikeys/username/";
    private static final String JSON_EXTENSION = ".json";

    private static final String APPLICATION_JSON = MediaType.JSON_UTF_8.toString();

    private final S3Client s3_;
    private final String bucketName_;

    private final ObjectMapper objectMapper_;

    private final UserAuthenticator userAuthenticator_;

    private final StringSigner stringSigner_;

    @Injectable
    public OnyxS3ApiKeyManager(
            final AwsConfig awsConfig,
            final OnyxS3Client onyxS3Client,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper,
            final UserAuthenticator userAuthenticator,
            final StringSigner stringSigner) {
        s3_ = onyxS3Client.getS3Client();
        bucketName_ = awsConfig.getAwsS3BucketName();
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();
        userAuthenticator_ = userAuthenticator;
        stringSigner_ = stringSigner;
    }

    @Nullable
    @Override
    public Session getSessionForApiKey(
            final String apiKey) {
        if (StringUtils.isBlank(apiKey)) {
            return null;
        }

        // Cheap, in-memory rejection of anything that isn't validly signed by this
        // server before spending an S3 GetObject on it. The signature is not what
        // makes the key valid though -- the S3 lookup below remains the sole
        // authority, which is what keeps revocation instant. Note: the lookup below
        // hashes the presented (signed) apiKey itself, not stringSigner_.verifyAndGet's
        // recovered inner value -- the credential was stored under a hash of the
        // signed string at creation time, so that's what has to be hashed here too.
        if (stringSigner_.verifyAndGet(apiKey) == null) {
            return null;
        }

        final ApiKeyCredential credential = getApiKeyCredential(apiKey);
        if (credential == null) {
            return null;
        }

        return userAuthenticator_.getSessionForUsername(Type.API, credential.getUsername());
    }

    /**
     * Generates a new cryptographically random API key for the given username, signs it,
     * persists its hash to S3, and returns the signed raw key alongside the
     * {@link ApiKeyCredential} that was persisted for it. The raw key is never persisted
     * anywhere; this is the only time it is ever available, so callers must capture and
     * hand it off immediately.
     *
     * <p>Writes the credential to two locations: the primary hash-keyed object that
     * {@link #getSessionForApiKey(String)} looks up directly by presented key, and a
     * second copy under a per-username prefix that allows listing all of a user's keys
     * without a full-bucket scan. Both objects carry the same content.
     */
    @Override
    public Pair<String, ApiKeyCredential> createApiKeyForUsername(
            final String username,
            @Nullable final String description) {
        checkNotNull(username, "Username cannot be null.");

        final String rawKey = UUID.randomUUID().toString();
        final String apiKey = stringSigner_.sign(rawKey);

        final String keyHash = getApiKeyHash(apiKey);

        final ApiKeyCredential credential = new ApiKeyCredential.Builder()
                .setKeyHash(keyHash)
                .setUsername(username)
                .setCreatedAt(Instant.now())
                .setDescription(description)
                .build();

        try {
            final String json = objectMapper_.writeValueAsString(credential);
            putJsonObject(API_KEYS_S3_PREFIX + keyHash + JSON_EXTENSION, json);
            putJsonObject(API_KEYS_USERNAME_S3_PREFIX + username + "/" + keyHash + JSON_EXTENSION, json);
        } catch (final Exception e) {
            throw new OnyxException("Failed to save API key for user: " + username, e);
        }

        return Pair.of(apiKey, credential);
    }

    /**
     * Lists all API key credentials belonging to the given username, via the
     * per-username secondary index maintained by {@link #createApiKeyForUsername}.
     * Bounded by that user's key count, not a full-bucket scan.
     */
    @Override
    public List<ApiKeyCredential> getApiKeysForUsername(
            final String username) {
        checkNotNull(username, "Username cannot be null.");

        final String prefix = API_KEYS_USERNAME_S3_PREFIX + username + "/";

        final ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName_)
                .prefix(prefix)
                .build();

        final ImmutableList.Builder<ApiKeyCredential> credentialsBuilder = ImmutableList.builder();
        for (final S3Object object : s3_.listObjectsV2Paginator(listRequest).contents()) {
            final ApiKeyCredential credential = getJsonObject(object.key(), ApiKeyCredential.class);
            if (credential != null) {
                credentialsBuilder.add(credential);
            }
        }

        return credentialsBuilder.build();
    }

    /**
     * Deletes the API key with the given hash from both the primary and per-username
     * S3 indexes, but only if it belongs to the given username &mdash; a caller cannot
     * delete another user's key even if they somehow learn its hash. Returns
     * {@code false} (a no-op) if no such key exists, or if it belongs to someone else.
     */
    @Override
    public boolean deleteApiKeyForUsername(
            final String username,
            final String keyHash) {
        checkNotNull(username, "Username cannot be null.");
        checkNotNull(keyHash, "Key hash cannot be null.");

        final ApiKeyCredential credential = getJsonObject(API_KEYS_S3_PREFIX + keyHash + JSON_EXTENSION,
                ApiKeyCredential.class);
        if (credential == null || !username.equals(credential.getUsername())) {
            return false;
        }

        deleteJsonObject(API_KEYS_S3_PREFIX + keyHash + JSON_EXTENSION);
        deleteJsonObject(API_KEYS_USERNAME_S3_PREFIX + username + "/" + keyHash + JSON_EXTENSION);

        return true;
    }

    @Nullable
    private ApiKeyCredential getApiKeyCredential(
            final String apiKey) {
        final String keyHash = getApiKeyHash(apiKey);

        return getJsonObject(API_KEYS_S3_PREFIX + keyHash + JSON_EXTENSION,
                ApiKeyCredential.class);
    }

    private static String getApiKeyHash(
            final String apiKey) {
        return DigestUtils.sha256Hex(apiKey);
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
            LOG.warn("Failed to read API key S3 object: {}", key, e);
            return null;
        }
    }

    private void putJsonObject(
            final String key,
            final String value) {
        final PutObjectRequest por = PutObjectRequest.builder()
                .bucket(bucketName_)
                .key(key)
                .contentType(APPLICATION_JSON)
                .build();
        s3_.putObject(por, RequestBody.fromString(value));
    }

    private void deleteJsonObject(
            final String key) {
        final DeleteObjectRequest dor = DeleteObjectRequest.builder()
                .bucket(bucketName_)
                .key(key)
                .build();
        s3_.deleteObject(dor);
    }

}
