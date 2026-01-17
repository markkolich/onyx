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

package onyx.controllers;

import com.google.common.collect.ImmutableList;
import onyx.components.config.OnyxConfig;
import onyx.components.storage.ResourceManager;
import onyx.components.storage.filter.ResourceFilter;
import onyx.entities.authentication.Session;
import onyx.entities.freemarker.DirectoryListing;
import onyx.entities.storage.aws.dynamodb.Resource;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static onyx.util.UserUtils.getVisibilityForResourceAndSession;

public abstract class AbstractOnyxResourceFilterAwareController extends AbstractOnyxFreeMarkerController {

    protected final ResourceFilter resourceFilter_;

    protected AbstractOnyxResourceFilterAwareController(
            final OnyxConfig onyxConfig,
            final ResourceManager resourceManager,
            final ResourceFilter resourceFilter) {
        super(onyxConfig, resourceManager);
        resourceFilter_ = resourceFilter;
    }

    protected DirectoryListing listDirectory(
            final Resource directory,
            @Nullable final Session session) {
        checkNotNull(directory, "Directory resource cannot be null.");

        final Set<Resource.Visibility> visibility =
                getVisibilityForResourceAndSession(directory, session);

        final List<Resource> listing = resourceManager_.listDirectory(
                directory,
                visibility,
                null);

        final List<Resource> filtered = listing.stream()
                .filter(resourceFilter_)
                .collect(ImmutableList.toImmutableList());

        return new DirectoryListing.Builder(filtered).build();
    }

}
