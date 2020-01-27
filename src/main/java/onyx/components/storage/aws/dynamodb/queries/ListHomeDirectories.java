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

package onyx.components.storage.aws.dynamodb.queries;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import onyx.components.storage.ResourceManager;
import onyx.entities.aws.dynamodb.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ListHomeDirectories {

    private static final Logger LOG = LoggerFactory.getLogger(ListHomeDirectories.class);

    public List<Resource> run(
            final IDynamoDBMapper dbMapper) {
        final DynamoDBScanExpression se = new DynamoDBScanExpression()
                .withExpressionAttributeNames(buildExpressionAttributes())
                .withExpressionAttributeValues(buildExpressionAttributeValues())
                .withFilterExpression(buildFilterExpression());

        final PaginatedScanList<Resource> scanResult = dbMapper.scan(Resource.class, se);

        return scanResult.stream()
                // Sort the results alphabetically based on path.
                .sorted(Comparator.comparing(Resource::getPath))
                .collect(ImmutableList.toImmutableList());
    }

    private Map<String, String> buildExpressionAttributes() {
        return ImmutableMap.of(
                "#name0", "parent",
                "#name1", "type");
    }

    private Map<String, AttributeValue> buildExpressionAttributeValues() {
        return ImmutableMap.of(
                ":value0", new AttributeValue().withS(ResourceManager.ROOT_PATH),
                ":value1", new AttributeValue().withS(Resource.Type.DIRECTORY.toString()));
    }

    private String buildFilterExpression() {
        return "#name0 = :value0 AND #name1 = :value1";
    }

}
