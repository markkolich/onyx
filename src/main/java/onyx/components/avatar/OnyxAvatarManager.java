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

package onyx.components.avatar;

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.aws.s3.OnyxS3Client;
import onyx.components.config.aws.AwsConfig;
import onyx.components.storage.AssetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.net.URL;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public final class OnyxAvatarManager implements AvatarManager {

    private static final Logger LOG = LoggerFactory.getLogger(OnyxAvatarManager.class);

    private static final String ONYX_AVATAR_PATH_PREFIX =
            AssetManager.ONYX_METADATA_PATH_PREFIX + "/avatars";

    private final AwsConfig awsConfig_;
    private final S3Client s3_;
    private final S3Presigner presigner_;

    @Injectable
    public OnyxAvatarManager(
            final AwsConfig awsConfig,
            final OnyxS3Client onyxS3Client) {
        awsConfig_ = awsConfig;
        s3_ = onyxS3Client.getS3Client();
        presigner_ = onyxS3Client.getPresigner();
    }

    @Override
    public URL getPresignedAvatarUrlForUsername(
            final String username) {
        final String bucketName = awsConfig_.getAwsS3BucketName();
        final String key = ONYX_AVATAR_PATH_PREFIX + "/" + username + ".jpg";

        try {
            s3_.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
        } catch (final NoSuchKeyException e) {
            return null;
        } catch (final Exception e) {
            LOG.info("Failed to check avatar in S3 for username: {}", username, e);
            return null;
        }

        final long validitySeconds =
                awsConfig_.getAwsS3PresignedAssetUrlValidityDuration(TimeUnit.SECONDS);

        final GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        final GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(validitySeconds))
                .getObjectRequest(getRequest)
                .build();

        return presigner_.presignGetObject(presignRequest).url();
    }

}
