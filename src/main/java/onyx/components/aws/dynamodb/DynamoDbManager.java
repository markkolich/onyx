/*
 * Copyright (c) 2023 Mark S. Kolich
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

package onyx.components.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.aws.dynamodb.queries.*;
import onyx.components.config.aws.AwsConfig;
import onyx.components.search.SearchManager;
import onyx.components.storage.ResourceManager;
import onyx.components.storage.async.AsyncResourceThreadPool;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.OnyxException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public final class DynamoDbManager implements ResourceManager {

    private final AwsConfig awsConfig_;

    private final IDynamoDBMapper dbMapper_;

    private final SearchManager searchManager_;

    private final ExecutorService asyncResourceExecutorService_;

    @Injectable
    public DynamoDbManager(
            final AwsConfig awsConfig,
            final DynamoDbMapper dynamoDbMapper,
            final SearchManager searchManager,
            final AsyncResourceThreadPool asyncResourceThreadPool) {
        this(awsConfig, dynamoDbMapper.getDbMapper(), searchManager,
                asyncResourceThreadPool.getExecutorService());
    }

    @VisibleForTesting
    public DynamoDbManager(
            final AwsConfig awsConfig,
            final IDynamoDBMapper dbMapper,
            final SearchManager searchManager,
            final ExecutorService executorService) {
        awsConfig_ = awsConfig;
        dbMapper_ = dbMapper;
        searchManager_ = searchManager;
        asyncResourceExecutorService_ = executorService;
    }

    @Nullable
    @Override
    public Resource getResourceAtPath(
            final String path) {
        return new GetResource(path).run(dbMapper_);
    }

    @Override
    public void createResource(
            final Resource resource) {
        new CreateResource(resource).run(dbMapper_, r -> {
            // Index the addition of the resource asynchronously.
            searchManager_.addResourceToIndexAsync(r, asyncResourceExecutorService_);

            // Update the parent resource size and all of its ancestors.
            updateParentResourceSizesAsync(r, Extensions.Op.ADD);
        });
    }

    @Override
    public void createResourceAsync(
            final Resource resource) {
        asyncResourceExecutorService_.submit(() -> createResource(resource));
    }

    @Override
    public void updateResource(
            final Resource resource) {
        new UpdateResource(resource).run(dbMapper_, r -> {
            // Index the update of the resource asynchronously.
            searchManager_.addResourceToIndexAsync(r, asyncResourceExecutorService_);
        });
    }

    @Override
    public void updateResourceAsync(
            final Resource resource) {
        asyncResourceExecutorService_.submit(() -> updateResource(resource));
    }

    @Override
    public void deleteResource(
            final Resource resource) {
        new DeleteResource(awsConfig_, resource).run(dbMapper_, r -> {
            // Index the deletion of the resource asynchronously.
            searchManager_.deleteResourceFromIndexAsync(r, asyncResourceExecutorService_);

            // Update the parent resource size and all of its ancestors.
            updateParentResourceSizesAsync(r, Extensions.Op.SUBTRACT);
        });
    }

    @Override
    public void deleteResourceAsync(
            final Resource resource) {
        asyncResourceExecutorService_.submit(() -> deleteResource(resource));
    }

    @Nonnull
    @Override
    public List<Resource> listDirectory(
            final Resource directory,
            final Set<Resource.Visibility> visibility,
            @Nullable final Extensions.Sort sort) {
        final List<Resource> resources = new ListDirectory(awsConfig_, directory, visibility).run(dbMapper_);

        final List<Resource> sorted;
        if (Extensions.Sort.FAVORITE.equals(sort)) {
            final ImmutableList.Builder<Resource> sortedBuilder = ImmutableList.builder();

            // Favorite resources first.
            sortedBuilder.addAll(resources.stream()
                    .filter(r -> Resource.Type.DIRECTORY.equals(r.getType()))
                    .filter(Resource::getFavorite)
                    .collect(ImmutableList.toImmutableList()));
            sortedBuilder.addAll(resources.stream()
                    .filter(r -> Resource.Type.FILE.equals(r.getType()))
                    .filter(Resource::getFavorite)
                    .collect(ImmutableList.toImmutableList()));

            // Then, add all other non-favorite resources.
            sortedBuilder.addAll(resources.stream()
                    .filter(r -> !r.getFavorite())
                    .collect(ImmutableList.toImmutableList()));

            sorted = sortedBuilder.build();
        } else {
            sorted = resources;
        }

        return sorted;
    }

    @Nonnull
    @Override
    public List<Resource> listHomeDirectories() {
        return new ListHomeDirectories(awsConfig_).run(dbMapper_);
    }

    // Helpers

    private void updateParentResourceSizesAsync(
            final Resource child,
            final Extensions.Op op) {
        checkNotNull(child, "Resource child cannot be null.");
        checkNotNull(op, "Resource operation cannot be null.");

        if (child.getSize() <= 0L) {
            // Nothing to update if the child is empty.
            return;
        }

        asyncResourceExecutorService_.submit(() -> {
            Resource parent = getResourceAtPath(child.getParent());
            while (parent != null && !ResourceManager.ROOT_PATH.equals(parent.getPath())) {
                if (Extensions.Op.ADD.equals(op)) {
                    final long newParentSize = parent.getSize() + child.getSize();
                    parent.setSize(newParentSize);
                } else if (Extensions.Op.SUBTRACT.equals(op)) {
                    final long newParentSize = parent.getSize() - child.getSize();
                    parent.setSize(Math.max(0L, newParentSize));
                } else {
                    throw new OnyxException("Unknown/unsupported resource operation: " + op);
                }

                updateResource(parent);

                parent = getResourceAtPath(parent.getParent());
            }
        });
    }

}
