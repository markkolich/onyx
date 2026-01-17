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
import com.google.common.collect.*;
import onyx.components.config.aws.AwsConfig;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static onyx.components.storage.ResourceManager.ROOT_PATH;

public final class ListDirectory {

    private static final Logger LOG = LoggerFactory.getLogger(ListDirectory.class);

    private final AwsConfig awsConfig_;

    private final Resource directory_;
    private final Set<Resource.Visibility> visibility_;

    private final String parentIndexName_;

    public ListDirectory(
            final AwsConfig awsConfig,
            final Resource directory,
            final Set<Resource.Visibility> visibility) {
        awsConfig_ = checkNotNull(awsConfig, "AWS config cannot be null.");
        directory_ = checkNotNull(directory, "Resource directory cannot be null.");
        visibility_ = checkNotNull(visibility, "Resource directory child visibility cannot be null.");

        parentIndexName_ = awsConfig_.getAwsDynamoDbParentIndexName();
    }

    public List<Resource> run(
            final IDynamoDBMapper dbMapper) {
        final DynamoDBQueryExpression<Resource> qe = new DynamoDBQueryExpression<Resource>()
                .withIndexName(parentIndexName_)
                .withConsistentRead(false)
                .withExpressionAttributeNames(buildExpressionAttributes(visibility_))
                .withExpressionAttributeValues(buildExpressionAttributeValues(directory_.getPath(), visibility_))
                .withKeyConditionExpression(buildKeyConditionExpression())
                .withFilterExpression(buildFilterExpression(visibility_));

        final PaginatedQueryList<Resource> queryResult = dbMapper.query(Resource.class, qe);

        final ListMultimap<Resource.Type, Resource> resources = queryResult.stream()
                // Intentionally keep the root "/" out of the listing.
                .filter(resource -> !ROOT_PATH.equals(resource.getPath()))
                // Sort the results alphabetically based on path, prior to partitioning by type.
                .sorted(Comparator.comparing(Resource::getPath))
                .collect(Multimaps.toMultimap(Resource::getType, r -> r,
                MultimapBuilder.ListMultimapBuilder.treeKeys().arrayListValues()::build));

        final List<Resource> directories = resources.get(Resource.Type.DIRECTORY);
        final List<Resource> files = resources.get(Resource.Type.FILE);

        // Directories go first, then files sorted alphabetically.
        return ImmutableList.copyOf(Iterables.concat(directories, files));
    }

    private static Map<String, String> buildExpressionAttributes(
            final Set<Resource.Visibility> visibility) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        builder.put("#name0", "parent");
        for (int i = 0, l = visibility.size(); i < l; i++) {
            builder.put("#name" + (i + 1), "visibility");
        }

        return builder.build();
    }

    private static Map<String, AttributeValue> buildExpressionAttributeValues(
            final String path,
            final Set<Resource.Visibility> visibility) {
        final ImmutableMap.Builder<String, AttributeValue> builder = ImmutableMap.builder();
        builder.put(":value0", new AttributeValue().withS(path));
        int idx = 0;
        for (final Resource.Visibility v : visibility) {
            builder.put(":value" + (idx + 1), new AttributeValue().withS(v.toString()));
            idx++;
        }

        return builder.build();
    }

    private static String buildKeyConditionExpression() {
        return "#name0 = :value0";
    }

    private static String buildFilterExpression(
            final Set<Resource.Visibility> visibility) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0, l = visibility.size(); i < l; i++) {
            builder.append("#name").append(i + 1).append(" = :value").append(i + 1);
            builder.append((i < l - 1) ? " OR " : "");
        }

        return builder.toString();
    }

}
