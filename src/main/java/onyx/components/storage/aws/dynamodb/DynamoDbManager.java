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

package onyx.components.storage.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.google.common.annotations.VisibleForTesting;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.OnyxConfig;
import onyx.components.storage.ResourceManager;
import onyx.components.storage.aws.dynamodb.queries.*;
import onyx.entities.aws.dynamodb.Resource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@Component
public final class DynamoDbManager implements ResourceManager {

    private final OnyxConfig onyxConfig_;

    private final IDynamoDBMapper dbMapper_;

    @Injectable
    public DynamoDbManager(
            final OnyxConfig onyxConfig,
            final DynamoDbMapper dynamoDbMapper) {
        this(onyxConfig, dynamoDbMapper.getDbMapper());
    }

    @VisibleForTesting
    protected DynamoDbManager(
            final OnyxConfig onyxConfig,
            final IDynamoDBMapper dbMapper) {
        onyxConfig_ = onyxConfig;
        dbMapper_ = dbMapper;
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
        new CreateResource(resource).run(dbMapper_);
    }

    @Override
    public void updateResource(
            final Resource resource) {
        new UpdateResource(resource).run(dbMapper_);
    }

    @Override
    public void deleteResource(
            final Resource resource) {
        new DeleteResource(resource).run(dbMapper_);
    }

    @Override
    public void deleteResourceAsync(
            final Resource resource,
            final ExecutorService executorService) {
        executorService.submit(() -> deleteResource(resource));
    }

    @Nonnull
    @Override
    public List<Resource> searchResources(
            final String query,
            final String owner) {
        // TODO(mark): implement
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Nonnull
    @Override
    public List<Resource> listDirectory(
            final Resource directory,
            final Set<Resource.Visibility> visibility) {
        return new ListDirectory(directory, visibility).run(dbMapper_);
    }

    @Nonnull
    @Override
    public List<Resource> listHomeDirectories() {
        return new ListHomeDirectories().run(dbMapper_);
    }

}
