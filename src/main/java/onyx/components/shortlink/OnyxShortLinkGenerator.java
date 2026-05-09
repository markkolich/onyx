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

package onyx.components.shortlink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.aws.s3.OnyxS3Client;
import onyx.components.config.OnyxConfig;
import onyx.components.config.aws.AwsConfig;
import onyx.components.storage.AssetManager;
import onyx.entities.shortlink.OnyxShortLink;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URL;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public final class OnyxShortLinkGenerator implements ShortLinkGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(OnyxShortLinkGenerator.class);

    private static final String SHORTLINKS_S3_PREFIX =
            AssetManager.ONYX_METADATA_PATH_PREFIX + "/shortlinks/";
    private static final String JSON_EXTENSION = ".json";

    private static final String SHORT_LINK_PATH_FORMAT = "%s/s/%s";

    private static final String APPLICATION_JSON = MediaType.JSON_UTF_8.toString();

    private final OnyxConfig onyxConfig_;

    private final S3Client s3_;
    private final String bucketName_;

    private final ObjectMapper objectMapper_;

    @Injectable
    public OnyxShortLinkGenerator(
            final OnyxConfig onyxConfig,
            final AwsConfig awsConfig,
            final OnyxS3Client onyxS3Client,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper) {
        onyxConfig_ = onyxConfig;
        s3_ = onyxS3Client.getS3Client();
        bucketName_ = awsConfig.getAwsS3BucketName();
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();
    }

    @Override
    public URL createShortLinkForResource(
            final Resource resource) {
        checkNotNull(resource, "Resource cannot be null.");

        try {
            final String code = DigestUtils.sha512Hex(resource.getPath())
                    .substring(0, 8);

            final OnyxShortLink shortLink = new OnyxShortLink.Builder()
                    .setResourcePath(resource.getPath())
                    .build();
            final String json = objectMapper_.writeValueAsString(shortLink);
            putJsonObject(SHORTLINKS_S3_PREFIX + code + JSON_EXTENSION, json);

            final String shortLinkUrl =
                    String.format(SHORT_LINK_PATH_FORMAT, onyxConfig_.getViewSafeFullUri(), code);
            return URI.create(shortLinkUrl).toURL();
        } catch (final Exception e) {
            LOG.error("Failed to generate short link for resource: {}", resource.getPath(), e);
            return null;
        }
    }

    @Nullable
    public OnyxShortLink getShortLink(
            final String code) {
        checkNotNull(code, "Short link code cannot be null.");

        return getJsonObject(SHORTLINKS_S3_PREFIX + code + JSON_EXTENSION, OnyxShortLink.class);
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
            LOG.warn("Failed to read short link S3 object: {}", key, e);
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

}
