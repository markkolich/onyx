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

package onyx.components.storage.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.components.ComponentDestroyable;
import onyx.components.storage.aws.AwsClientConfiguration;
import onyx.components.storage.aws.AwsCredentials;
import onyx.components.config.OnyxConfig;

@Component
public final class DynamoDbClient implements ComponentDestroyable {

    private final AmazonDynamoDB dynamoDb_;

    @Injectable
    public DynamoDbClient(
            final OnyxConfig onyxConfig,
            final AwsCredentials awsCredentials,
            final AwsClientConfiguration awsClientConfiguration) {
        dynamoDb_ = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(awsCredentials.getCredentialsProvider())
                .withClientConfiguration(awsClientConfiguration.getClientConfiguration())
                .withRegion(onyxConfig.getAwsRegion())
                .build();
    }

    public AmazonDynamoDB getDbClient() {
        return dynamoDb_;
    }

    @Override
    public void destroy() throws Exception {
        dynamoDb_.shutdown();
    }

}
