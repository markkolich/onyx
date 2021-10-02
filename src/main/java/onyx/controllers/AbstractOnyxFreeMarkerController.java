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

package onyx.controllers;

import com.google.common.collect.ImmutableSet;
import onyx.components.config.OnyxConfig;
import onyx.components.storage.AsynchronousResourcePool;
import onyx.components.storage.ResourceManager;
import onyx.entities.authentication.Session;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.util.FileUtils;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractOnyxFreeMarkerController extends AbstractOnyxController {

    protected final ResourceManager resourceManager_;

    protected AbstractOnyxFreeMarkerController(
            final OnyxConfig onyxConfig,
            final AsynchronousResourcePool asynchronousResourcePool,
            final ResourceManager resourceManager) {
        super(onyxConfig, asynchronousResourcePool);
        resourceManager_ = resourceManager;
    }

    protected List<Resource> listDirectory(
            final Resource directory,
            @Nullable final Session session) {
        checkNotNull(directory, "Directory resource cannot be null.");

        final boolean userIsOwner = userIsOwner(directory, session);

        final ImmutableSet.Builder<Resource.Visibility> visibility = ImmutableSet.builder();
        visibility.add(Resource.Visibility.PUBLIC);
        // Only show private resources inside of the directory if the authenticated
        // user is the directory owner.
        if (userIsOwner) {
            visibility.add(Resource.Visibility.PRIVATE);
        }

        return resourceManager_.listDirectory(directory,
                visibility.build(),
                // Sort only if the user is the directory resource owner.
                userIsOwner ? ResourceManager.Extensions.Sort.FAVORITE : null);
    }

    protected long countDirectories(
            final List<Resource> resources) {
        if (CollectionUtils.isEmpty(resources)) {
            return 0L;
        }

        return resources.stream()
                .filter(c -> Resource.Type.DIRECTORY.equals(c.getType()))
                .count();
    }

    protected long countFiles(
            final List<Resource> resources) {
        if (CollectionUtils.isEmpty(resources)) {
            return 0L;
        }

        return resources.stream()
                .filter(c -> Resource.Type.FILE.equals(c.getType()))
                .count();
    }

    protected long countTotalFileSize(
            final List<Resource> resources) {
        if (CollectionUtils.isEmpty(resources)) {
            return 0L;
        }

        return resources.stream()
                .filter(c -> Resource.Type.FILE.equals(c.getType()))
                .map(Resource::getSize)
                .mapToLong(Long::longValue)
                .sum();
    }

    protected String countTotalFileSizeForDisplay(
            final List<Resource> resources) {
        return FileUtils.humanReadableByteCountBin(countTotalFileSize(resources));
    }

    protected boolean userIsOwner(
            final Resource resource,
            @Nullable final Session session) {
        if (session == null) {
            return false;
        }

        return session.getUsername().equals(resource.getOwner());
    }

}
