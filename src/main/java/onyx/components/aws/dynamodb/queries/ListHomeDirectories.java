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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import onyx.components.storage.ResourceManager;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static onyx.components.aws.dynamodb.DynamoDbManager.PARENT_INDEX_NAME;

public final class ListHomeDirectories {

    private static final Logger LOG = LoggerFactory.getLogger(ListHomeDirectories.class);

    public List<Resource> run(
            final DynamoDbTable<Resource> resourceTable) {
        final DynamoDbIndex<Resource> parentIndex = resourceTable.index(PARENT_INDEX_NAME);

        final QueryConditional queryConditional = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(ResourceManager.ROOT_PATH).build());

        final Expression filterExpression = Expression.builder()
                .expression("#type = :type")
                .expressionNames(buildExpressionNames())
                .expressionValues(buildExpressionValues())
                .build();

        final QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .filterExpression(filterExpression)
                .build();

        return parentIndex.query(request)
                .stream()
                .flatMap(page -> page.items().stream())
                // Sort the results alphabetically based on path.
                .sorted(Comparator.comparing(Resource::getPath))
                .collect(ImmutableList.toImmutableList());
    }

    private static Map<String, String> buildExpressionNames() {
        return ImmutableMap.of("#type", "type");
    }

    private static Map<String, AttributeValue> buildExpressionValues() {
        return ImmutableMap.of(":type",
                AttributeValue.builder().s(Resource.Type.DIRECTORY.toString()).build());
    }

}
