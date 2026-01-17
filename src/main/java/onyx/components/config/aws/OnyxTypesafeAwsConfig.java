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

package onyx.components.config.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.StorageClass;
import com.typesafe.config.Config;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.OnyxConfig;

import java.util.concurrent.TimeUnit;

@Component
public final class OnyxTypesafeAwsConfig implements AwsConfig {

    private final Config config_;

    @Injectable
    public OnyxTypesafeAwsConfig(
            final OnyxConfig onyxConfig) {
        config_ = onyxConfig.getOnyxConfig().getConfig(AWS_CONFIG_PATH);
    }

    @Override
    public String getAwsAccessKey() {
        return config_.getString(AWS_ACCESS_KEY_PROP);
    }

    @Override
    public String getAwsSecretKey() {
        return config_.getString(AWS_SECRET_KEY_PROP);
    }

    // DynamoDB config

    @Override
    public Regions getAwsDynamoDbRegion() {
        return Regions.fromName(config_.getString(AWS_DYNAMO_DB_REGION_PROP));
    }

    @Override
    public String getAwsDynamoDbTableName() {
        return config_.getString(AWS_DYNAMO_DB_TABLE_NAME_PROP);
    }

    @Override
    public String getAwsDynamoDbParentIndexName() {
        return config_.getString(AWS_DYNAMO_DB_PARENT_INDEX_NAME_PROP);
    }

    // S3 config

    @Override
    public Regions getAwsS3Region() {
        return Regions.fromName(config_.getString(AWS_S3_REGION_PROP));
    }

    @Override
    public String getAwsS3BucketName() {
        return config_.getString(AWS_S3_BUCKET_NAME_PROP);
    }

    @Override
    public boolean getAwsS3VersioningEnabled() {
        return config_.getBoolean(AWS_S3_VERSIONING_ENABLED_PROP);
    }

    @Override
    public long getAwsS3PresignedAssetUrlValidityDuration(
            final TimeUnit timeUnit) {
        return config_.getDuration(AWS_S3_ASSET_URL_VALIDITY_DURATION_PROP, timeUnit);
    }

    @Override
    public StorageClass getAwsS3DefaultStorageClass() {
        return StorageClass.fromValue(config_.getString(AWS_S3_DEFAULT_STORAGE_CLASS_PROP));
    }

    @Override
    public long getAwsS3MaxUploadFileSize() {
        return config_.getBytes(AWS_S3_MAX_UPLOAD_FILE_SIZE_PROP);
    }

    // SNS config

    @Override
    public Regions getAwsSnsRegion() {
        return Regions.fromName(config_.getString(AWS_SNS_REGION_PROP));
    }

}
