/*
 * Copyright (c) 2020 Mark S. Kolich
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

package onyx.components.config;

import com.amazonaws.regions.Regions;
import com.typesafe.config.Config;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public interface OnyxConfig {

    String CONTEXT_PATH_PROP = "context-path";
    String BASE_URI_PROP = "base-uri";
    String FULL_URI_PROP = "full-uri";
    String DEV_MODE_PROP = "dev-mode";

    String SESSION_DURATION_PROP = "session.duration";
    String SESSION_SIGNER_SECRET_PROP = "session.signer-secret";
    String SESSION_HTTPS_PROP = "session.https";
    String SESSION_USERS_PROP = "session.users";

    String LOCAL_CACHE_ENABLED = "local-cache.enabled";
    String LOCAL_CACHE_DIRECTORY = "local-cache.directory";
    String LOCAL_CACHE_TOKEN_SIGNER_SECRET = "local-cache.token-signer-secret";
    String LOCAL_CACHE_TOKEN_VALIDITY_DURATION = "local-cache.token-validity-duration";

    String AWS_ACCESS_KEY_PROP = "aws.access-key";
    String AWS_SECRET_KEY_PROP = "aws.secret-key";
    String AWS_REGION_PROP = "aws.region";

    String AWS_DYNAMO_DB_TABLE_NAME_PROP = "aws.dynamo-db.table-name";

    String AWS_S3_BUCKET_NAME_PROP = "aws.s3.bucket-name";
    String AWS_S3_ASSET_URL_VALIDITY_DURATION_PROP = "aws.s3.asset-url-validity-duration";

    Config getOnyxConfig();

    // Application config

    String getContextPath();

    String getBaseUri();

    String getFullUri();

    boolean isDevMode();

    // Session config

    long getSessionDuration(
            final TimeUnit timeUnit);

    String getSessionSignerSecret();

    boolean isSessionUsingHttps();

    // Local resource cache config

    boolean localCacheEnabled();

    Path getLocalCacheDirectory();

    String getLocalCacheTokenSignerSecret();

    long getLocalCacheTokenValidityDuration(
            final TimeUnit timeUnit);

    // AWS config

    String getAwsAccessKey();

    String getAwsSecretKey();

    Regions getAwsRegion();

    // AWS DynamoDB config

    String getAwsDynamoDbTableName();

    // AWS S3 config

    String getAwsS3BucketName();

    long getAwsS3PresignedAssetUrlValidityDuration(
            final TimeUnit timeUnit);

}
