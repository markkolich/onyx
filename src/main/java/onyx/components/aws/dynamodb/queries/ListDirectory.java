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

package onyx.components.aws.dynamodb.queries;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
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

    private final Resource directory_;
    private final Set<Resource.Visibility> visibility_;

    public ListDirectory(
            final Resource directory,
            final Set<Resource.Visibility> visibility) {
        directory_ = checkNotNull(directory, "Resource directory cannot be null.");
        visibility_ = checkNotNull(visibility, "Resource directory child visibility cannot be null.");
    }

    public List<Resource> run(
            final IDynamoDBMapper dbMapper) {
        final DynamoDBScanExpression se = new DynamoDBScanExpression()
                .withExpressionAttributeNames(buildExpressionAttributes())
                .withExpressionAttributeValues(buildExpressionAttributeValues())
                .withFilterExpression(buildFilterExpression());

        final PaginatedScanList<Resource> scanResult = dbMapper.scan(Resource.class, se);

        final List<Resource> directories = scanResult.stream()
                // Intentionally keep the root "/" out of the listing.
                .filter(resource -> !ROOT_PATH.equals(resource.getPath()))
                .filter(resource -> Resource.Type.DIRECTORY.equals(resource.getType()))
                // Sort the results alphabetically based on path.
                .sorted(Comparator.comparing(Resource::getPath))
                .collect(ImmutableList.toImmutableList());
        final List<Resource> files = scanResult.stream()
                .filter(resource -> Resource.Type.FILE.equals(resource.getType()))
                // Sort the results alphabetically based on path.
                .sorted(Comparator.comparing(Resource::getPath))
                .collect(ImmutableList.toImmutableList());

        // Directories go first, then files and links sorted alphabetically.
        return ImmutableList.copyOf(Iterables.concat(directories, files));
    }

    private Map<String, String> buildExpressionAttributes() {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        builder.put("#name0", "parent");
        for (int i = 0, l = visibility_.size(); i < l; i++) {
            builder.put("#name" + (i + 1), "visibility");
        }

        return builder.build();
    }

    private Map<String, AttributeValue> buildExpressionAttributeValues() {
        final ImmutableMap.Builder<String, AttributeValue> builder = ImmutableMap.builder();
        builder.put(":value0", new AttributeValue().withS(directory_.getPath()));
        int idx = 0;
        for (final Resource.Visibility v : visibility_) {
            builder.put(":value" + (idx + 1), new AttributeValue().withS(v.toString()));
            idx++;
        }

        return builder.build();
    }

    private String buildFilterExpression() {
        final StringBuilder builder = new StringBuilder("#name0 = :value0");
        if (!visibility_.isEmpty()) {
            builder.append(" AND (");
            for (int i = 0, l = visibility_.size(); i < l; i++) {
                builder.append("#name" + (i + 1) + " = :value" + (i + 1));
                builder.append((i < l - 1) ? " OR " : "");
            }
            builder.append(")");
        }

        return builder.toString();
    }

}
