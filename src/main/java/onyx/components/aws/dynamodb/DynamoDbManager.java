/*
 * Copyright (c) 2022 Mark S. Kolich
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
import onyx.entities.storage.aws.dynamodb.Resource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@Component
public final class DynamoDbManager implements ResourceManager {

    private final AwsConfig awsConfig_;

    private final SearchManager searchManager_;

    private final IDynamoDBMapper dbMapper_;

    @Injectable
    public DynamoDbManager(
            final AwsConfig awsConfig,
            final DynamoDbMapper dynamoDbMapper,
            final SearchManager searchManager) {
        this(awsConfig, dynamoDbMapper.getDbMapper(), searchManager);
    }

    @VisibleForTesting
    protected DynamoDbManager(
            final AwsConfig awsConfig,
            final IDynamoDBMapper dbMapper,
            final SearchManager searchManager) {
        awsConfig_ = awsConfig;
        dbMapper_ = dbMapper;
        searchManager_ = searchManager;
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
        new CreateResource(resource).run(dbMapper_, searchManager_::addResourceToIndex);
    }

    @Override
    public void updateResource(
            final Resource resource) {
        new UpdateResource(resource).run(dbMapper_, searchManager_::addResourceToIndex);
    }

    @Override
    public void deleteResource(
            final Resource resource) {
        new DeleteResource(awsConfig_, resource).run(dbMapper_, searchManager_::deleteResourceFromIndex);
    }

    @Override
    public void deleteResourceAsync(
            final Resource resource,
            final ExecutorService executorService) {
        executorService.submit(() -> deleteResource(resource));
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

}
