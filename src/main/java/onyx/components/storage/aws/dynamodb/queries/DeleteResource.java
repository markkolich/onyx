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
import com.google.common.collect.ImmutableMap;
import onyx.components.storage.ResourceManager;
import onyx.entities.aws.dynamodb.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class DeleteResource {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteResource.class);

    private final Resource resource_;

    public DeleteResource(
            final Resource resource) {
        resource_ = checkNotNull(resource, "Resource cannot be null.");
    }

    public void run(
            final IDynamoDBMapper dbMapper) {
        if (Resource.Type.FILE.equals(resource_.getType())) {
            dbMapper.delete(resource_);
        } else if (Resource.Type.DIRECTORY.equals(resource_.getType())) {
            // First, delete the parent directory itself.
            dbMapper.delete(resource_);

            // Then, delete any of the children recursively.
            final DynamoDBScanExpression se = new DynamoDBScanExpression()
                    .withExpressionAttributeNames(buildExpressionAttributes())
                    .withExpressionAttributeValues(buildExpressionAttributeValues())
                    .withFilterExpression(buildFilterExpression());
            final PaginatedScanList<Resource> scanResult = dbMapper.scan(Resource.class, se);
            for (final Resource child : scanResult) {
                dbMapper.delete(child);
            }
        }
    }

    private Map<String, String> buildExpressionAttributes() {
        return ImmutableMap.of("#name0", "path");
    }

    private Map<String, AttributeValue> buildExpressionAttributeValues() {
        // IMPORTANT: note the trailing slash on the value, which is to scan for any children
        // of the directory.
        return ImmutableMap.of(":value0",
                new AttributeValue().withS(resource_.getPath() + ResourceManager.ROOT_PATH));
    }

    private String buildFilterExpression() {
        return "begins_with(#name0, :value0)";
    }

}
