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

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.collect.ImmutableMap;
import onyx.entities.aws.dynamodb.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class GetResource {

    private static final Logger LOG = LoggerFactory.getLogger(GetResource.class);

    private final String path_;

    public GetResource(
            final String path) {
        path_ = checkNotNull(path, "Resource path cannot be null.");
    }

    public Resource run(
            final IDynamoDBMapper dbMapper) {
        final DynamoDBQueryExpression<Resource> qe = new DynamoDBQueryExpression<Resource>()
                .withExpressionAttributeNames(buildExpressionAttributes())
                .withExpressionAttributeValues(buildExpressionAttributeValues())
                .withKeyConditionExpression(buildKeyConditionExpression());

        final PaginatedQueryList<Resource> resources = dbMapper.query(Resource.class, qe);
        if (resources.isEmpty()) {
            LOG.debug("Found no resource at path: {}", path_);
            return null;
        } else if (resources.size() > 1) {
            LOG.error("Found more than one resource at path: {}", path_);
            return null;
        }

        return resources.iterator().next();
    }

    private Map<String, String> buildExpressionAttributes() {
        return ImmutableMap.of("#name0", "path");
    }

    private Map<String, AttributeValue> buildExpressionAttributeValues() {
        return ImmutableMap.of(":value0", new AttributeValue().withS(path_));
    }

    private String buildKeyConditionExpression() {
        return "#name0 = :value0";
    }

}
