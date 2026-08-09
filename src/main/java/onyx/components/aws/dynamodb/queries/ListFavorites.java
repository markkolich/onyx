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
import onyx.entities.storage.aws.dynamodb.Resource;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static onyx.components.aws.dynamodb.DynamoDbManager.FAVORITE_INDEX_NAME;

/**
 * Recursively lists all favorited resources under a given directory, at any
 * depth, via a single bounded {@code Query} against the sparse
 * {@code favorite-index} GSI (partition key {@code favoriteOwner}, sort key
 * {@code path}). Because {@code favoriteOwner} is only populated on
 * favorited resources, the key condition alone already guarantees every
 * result is a favorite; a filter expression is layered on top to further
 * restrict results to the given {@code types} (e.g. {@code FILE}-only, to
 * exclude favorited directories from a given view). Because {@code path} is
 * the GSI's sort key, results come back already ordered by path with no
 * client-side sort needed.
 */
public final class ListFavorites {

    private final Resource directory_;
    private final Set<Resource.Type> types_;

    public ListFavorites(
            final Resource directory,
            final Set<Resource.Type> types) {
        directory_ = checkNotNull(directory, "Resource directory cannot be null.");
        types_ = checkNotNull(types, "Resource types cannot be null.");
    }

    public List<Resource> run(
            final DynamoDbTable<Resource> resourceTable) {
        final DynamoDbIndex<Resource> favoriteIndex = resourceTable.index(FAVORITE_INDEX_NAME);

        final String path = directory_.getPath();
        final String prefix = path.endsWith("/") ? path : path + "/";

        final QueryConditional queryConditional = QueryConditional.sortBeginsWith(
                Key.builder().partitionValue(directory_.getOwner()).sortValue(prefix).build());

        final QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .filterExpression(buildTypeFilterExpression(types_))
                .build();

        return favoriteIndex.query(request)
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(ImmutableList.toImmutableList());
    }

    private static Expression buildTypeFilterExpression(
            final Set<Resource.Type> types) {
        final ImmutableMap.Builder<String, String> nameBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<String, AttributeValue> valueBuilder = ImmutableMap.builder();
        final StringBuilder expression = new StringBuilder();

        int idx = 0;
        for (final Resource.Type type : types) {
            if (idx > 0) {
                expression.append(" OR ");
            }
            final String namePlaceholder = "#type" + idx;
            final String valuePlaceholder = ":type" + idx;
            expression.append(namePlaceholder).append(" = ").append(valuePlaceholder);
            nameBuilder.put(namePlaceholder, "type");
            valueBuilder.put(valuePlaceholder, AttributeValue.builder().s(type.toString()).build());
            idx++;
        }

        return Expression.builder()
                .expression(expression.toString())
                .expressionNames(nameBuilder.build())
                .expressionValues(valueBuilder.build())
                .build();
    }

}
