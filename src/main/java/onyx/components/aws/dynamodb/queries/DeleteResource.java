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
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static onyx.components.aws.dynamodb.DynamoDbManager.PARENT_INDEX_NAME;
import static onyx.components.storage.ResourceManager.ROOT_PATH;

public final class DeleteResource {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteResource.class);

    private static final int BATCH_WRITE_MAX_SIZE = 25;

    private final Resource resource_;

    public DeleteResource(
            final Resource resource) {
        resource_ = checkNotNull(resource, "Resource cannot be null.");
    }

    public void run(
            final DynamoDbEnhancedClient enhancedClient,
            final DynamoDbTable<Resource> resourceTable,
            @Nullable final Consumer<Resource> callback) {
        deleteResource(resource_, callback, enhancedClient, resourceTable);
    }

    private void deleteResource(
            final Resource resource,
            final Consumer<Resource> callback,
            final DynamoDbEnhancedClient enhancedClient,
            final DynamoDbTable<Resource> resourceTable) {
        final Resource.Type resourceType = resource.getType();
        if (Resource.Type.FILE.equals(resourceType)) {
            resourceTable.deleteItem(resource);
            if (callback != null) {
                callback.accept(resource);
            }
        } else if (Resource.Type.DIRECTORY.equals(resourceType)) {
            // First, delete the parent directory itself.
            resourceTable.deleteItem(resource);
            if (callback != null) {
                callback.accept(resource);
            }

            final DynamoDbIndex<Resource> parentIndex = resourceTable.index(PARENT_INDEX_NAME);

            final QueryConditional queryConditional = QueryConditional.keyEqualTo(
                    Key.builder().partitionValue(resource.getPath()).build());

            final QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                    .queryConditional(queryConditional)
                    .build();

            final ListMultimap<Resource.Type, Resource> resources = parentIndex.query(request)
                    .stream()
                    .flatMap(page -> page.items().stream())
                    // Intentionally always skip the root "/" directory.
                    .filter(r -> !ROOT_PATH.equals(r.getPath()))
                    // Sort the results alphabetically based on path.
                    .sorted(Comparator.comparing(Resource::getPath))
                    .collect(Multimaps.toMultimap(Resource::getType, r -> r,
                    MultimapBuilder.ListMultimapBuilder.treeKeys().arrayListValues()::build));

            // Then, batch delete any files.
            final List<Resource> files = resources.get(Resource.Type.FILE);
            if (CollectionUtils.isNotEmpty(files)) {
                batchDeleteResources(files, enhancedClient, resourceTable);
                if (callback != null) {
                    files.forEach(callback);
                }
            }

            // Lastly, recursively delete any directories.
            final List<Resource> directories = resources.get(Resource.Type.DIRECTORY);
            if (CollectionUtils.isNotEmpty(directories)) {
                for (final Resource directory : directories) {
                    deleteResource(directory, callback, enhancedClient, resourceTable);
                }
            }
        }
    }

    private void batchDeleteResources(
            final List<Resource> resources,
            final DynamoDbEnhancedClient enhancedClient,
            final DynamoDbTable<Resource> resourceTable) {
        final List<List<Resource>> batches = Lists.partition(resources, BATCH_WRITE_MAX_SIZE);
        for (final List<Resource> batch : batches) {
            final WriteBatch.Builder<Resource> writeBatchBuilder = WriteBatch.builder(Resource.class)
                    .mappedTableResource(resourceTable);
            for (final Resource resource : batch) {
                writeBatchBuilder.addDeleteItem(resource);
            }

            final BatchWriteItemEnhancedRequest batchRequest = BatchWriteItemEnhancedRequest.builder()
                    .writeBatches(writeBatchBuilder.build())
                    .build();

            final BatchWriteResult result = enhancedClient.batchWriteItem(batchRequest);
            final List<Key> unprocessedItems = result.unprocessedDeleteItemsForTable(resourceTable);
            if (CollectionUtils.isNotEmpty(unprocessedItems)) {
                LOG.error("Failed to delete {} resources in batch delete operation.",
                        unprocessedItems.size());
            }
        }
    }

}
