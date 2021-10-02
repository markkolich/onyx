/*
 * Copyright (c) 2022 Mark S. Kolich
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

import java.util.concurrent.TimeUnit;

public interface AwsConfig {

    String AWS_CONFIG_PATH = "aws";

    String AWS_ACCESS_KEY_PROP = "access-key";
    String AWS_SECRET_KEY_PROP = "secret-key";

    String AWS_DYNAMO_DB_REGION_PROP = "dynamo-db.region";
    String AWS_DYNAMO_DB_TABLE_NAME_PROP = "dynamo-db.table-name";
    String AWS_DYNAMO_DB_PARENT_INDEX_NAME_PROP = "dynamo-db.parent-index-name";

    String AWS_S3_REGION_PROP = "s3.region";
    String AWS_S3_BUCKET_NAME_PROP = "s3.bucket-name";
    String AWS_S3_ASSET_URL_VALIDITY_DURATION_PROP = "s3.asset-url-validity-duration";
    String AWS_S3_DEFAULT_STORAGE_CLASS_PROP = "s3.default-storage-class";

    String AWS_SNS_REGION_PROP = "sns.region";

    String getAwsAccessKey();

    String getAwsSecretKey();

    // DynamoDB config

    Regions getAwsDynamoDbRegion();

    String getAwsDynamoDbTableName();

    String getAwsDynamoDbParentIndexName();

    // S3 config

    Regions getAwsS3Region();

    String getAwsS3BucketName();

    long getAwsS3PresignedAssetUrlValidityDuration(
            final TimeUnit timeUnit);

    StorageClass getAwsS3DefaultStorageClass();

    // SNS config

    Regions getAwsSnsRegion();

}
