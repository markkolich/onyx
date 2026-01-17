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

package onyx.components.aws.dynamodb.queries;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import onyx.components.config.aws.AwsConfig;
import onyx.components.storage.ResourceManager;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ListHomeDirectories {

    private static final Logger LOG = LoggerFactory.getLogger(ListHomeDirectories.class);

    private final AwsConfig awsConfig_;

    private final String parentIndexName_;

    public ListHomeDirectories(
            final AwsConfig awsConfig) {
        awsConfig_ = checkNotNull(awsConfig, "AWS config cannot be null.");

        parentIndexName_ = awsConfig_.getAwsDynamoDbParentIndexName();
    }

    public List<Resource> run(
            final IDynamoDBMapper dbMapper) {
        final DynamoDBQueryExpression<Resource> qe = new DynamoDBQueryExpression<Resource>()
                .withIndexName(parentIndexName_)
                .withConsistentRead(false)
                .withExpressionAttributeNames(buildExpressionAttributes())
                .withExpressionAttributeValues(buildExpressionAttributeValues())
                .withKeyConditionExpression(buildKeyConditionExpression())
                .withFilterExpression(buildFilterExpression());

        final PaginatedQueryList<Resource> queryResult = dbMapper.query(Resource.class, qe);

        return queryResult.stream()
                // Sort the results alphabetically based on path.
                .sorted(Comparator.comparing(Resource::getPath))
                .collect(ImmutableList.toImmutableList());
    }

    private static Map<String, String> buildExpressionAttributes() {
        return ImmutableMap.of(
                "#name0", "parent",
                "#name1", "type");
    }

    private static Map<String, AttributeValue> buildExpressionAttributeValues() {
        return ImmutableMap.of(
                ":value0", new AttributeValue().withS(ResourceManager.ROOT_PATH),
                ":value1", new AttributeValue().withS(Resource.Type.DIRECTORY.toString()));
    }

    private static String buildKeyConditionExpression() {
        return "#name0 = :value0";
    }

    private static String buildFilterExpression() {
        return "#name1 = :value1";
    }

}
