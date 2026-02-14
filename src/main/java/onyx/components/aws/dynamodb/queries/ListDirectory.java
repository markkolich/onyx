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

import com.google.common.collect.*;
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
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static onyx.components.aws.dynamodb.DynamoDbManager.PARENT_INDEX_NAME;
import static onyx.components.storage.ResourceManager.ROOT_PATH;

public final class ListDirectory {

    private static final Logger LOG = LoggerFactory.getLogger(ListDirectory.class);

    private final Resource directory_;
    private final Set<Resource.Visibility> visibility_;

    public ListDirectory(
            final Resource directory,
            final Set<Resource.Visibility> visibility) {
        directory_ = checkNotNull(directory, "Resource directory cannot be null.");
        visibility_ = checkNotNull(visibility, "Resource directory child visibility cannot be null.");
    }

    public List<Resource> run(
            final DynamoDbTable<Resource> resourceTable) {
        final DynamoDbIndex<Resource> parentIndex = resourceTable.index(PARENT_INDEX_NAME);

        final QueryConditional queryConditional = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(directory_.getPath()).build());

        final QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .filterExpression(buildFilterExpression(visibility_))
                .build();

        final ListMultimap<Resource.Type, Resource> resources = parentIndex.query(request)
                .stream()
                .flatMap(page -> page.items().stream())
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

    private static Expression buildFilterExpression(
            final Set<Resource.Visibility> visibility) {
        final ImmutableMap.Builder<String, String> nameBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<String, AttributeValue> valueBuilder = ImmutableMap.builder();
        final StringBuilder expression = new StringBuilder();

        int idx = 0;
        for (final Resource.Visibility v : visibility) {
            if (idx > 0) {
                expression.append(" OR ");
            }
            final String namePlaceholder = "#vis" + idx;
            final String valuePlaceholder = ":vis" + idx;
            expression.append(namePlaceholder).append(" = ").append(valuePlaceholder);
            nameBuilder.put(namePlaceholder, "visibility");
            valueBuilder.put(valuePlaceholder, AttributeValue.builder().s(v.toString()).build());
            idx++;
        }

        return Expression.builder()
                .expression(expression.toString())
                .expressionNames(nameBuilder.build())
                .expressionValues(valueBuilder.build())
                .build();
    }

}
