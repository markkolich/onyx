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
import com.typesafe.config.ConfigFactory;
import curacao.annotations.Component;

import java.util.concurrent.TimeUnit;

/**
 * An {@link OnyxConfig} implementation backed by the Typesafe (a.k.a., Lightbend)
 * configuration library:
 *
 * https://github.com/lightbend/config
 */
@Component
public final class OnyxTypesafeConfig implements OnyxConfig {

    private static final String ONYX_CONFIG_PATH = "onyx";

    private final Config config_;

    public OnyxTypesafeConfig() {
        config_ = ConfigFactory.load().getConfig(ONYX_CONFIG_PATH);
    }

    @Override
    public Config getOnyxConfig() {
        return config_;
    }

    // Application config

    @Override
    public String getContextPath() {
        return config_.getString(CONTEXT_PATH_PROP);
    }

    @Override
    public String getBaseUri() {
        return config_.getString(BASE_URI_PROP);
    }

    @Override
    public String getFullUri() {
        return config_.getString(FULL_URI_PROP);
    }

    @Override
    public boolean isDevMode() {
        return config_.getBoolean(DEV_MODE_PROP);
    }

    @Override
    public long getSessionDuration(
            final TimeUnit timeUnit) {
        return config_.getDuration(SESSION_DURATION_PROP, timeUnit);
    }

    @Override
    public String getSessionSignerSecret() {
        return config_.getString(SESSION_SIGNER_SECRET_PROP);
    }

    @Override
    public boolean isSessionUsingHttps() {
        return config_.getBoolean(SESSION_HTTPS_PROP);
    }

    // AWS

    @Override
    public String getAwsAccessKey() {
        return config_.getString(AWS_ACCESS_KEY_PROP);
    }

    @Override
    public String getAwsSecretKey() {
        return config_.getString(AWS_SECRET_KEY_PROP);
    }

    @Override
    public Regions getAwsRegion() {
        return Regions.fromName(config_.getString(AWS_REGION_PROP));
    }

    // AWS DynamoDB

    @Override
    public String getAwsDynamoDbTableName() {
        return config_.getString(AWS_DYNAMO_DB_TABLE_NAME_PROP);
    }

    // AWS S3

    @Override
    public String getAwsS3BucketName() {
        return config_.getString(AWS_S3_BUCKET_NAME_PROP);
    }

    @Override
    public long getAwsS3PresignedAssetUrlValidityDuration(
            final TimeUnit timeUnit) {
        return config_.getDuration(AWS_S3_ASSET_URL_VALIDITY_DURATION_PROP, timeUnit);
    }

}
