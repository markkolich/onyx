/*
 * Copyright (c) 2024 Mark S. Kolich
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

import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.Path;
import onyx.components.config.OnyxConfig;
import onyx.components.storage.ResourceManager;
import onyx.components.storage.filter.ResourceFilter;
import onyx.entities.authentication.Session;
import onyx.entities.freemarker.DirectoryListing;
import onyx.entities.freemarker.FreeMarkerContent;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.resource.ResourceForbiddenException;
import onyx.exceptions.resource.ResourceNotFoundException;

import static onyx.util.FileUtils.humanReadableByteCountBin;
import static onyx.util.PathUtils.normalizePath;
import static onyx.util.PathUtils.splitNormalizedPathToElements;

@Controller
public final class Browse extends AbstractOnyxResourceFilterAwareController {

    @Injectable
    public Browse(
            final OnyxConfig onyxConfig,
            final ResourceManager resourceManager,
            final ResourceFilter resourceFilter) {
        super(onyxConfig, resourceManager, resourceFilter);
    }

    @RequestMapping(value = "^/browse/(?<username>[a-zA-Z0-9]+)$")
    public FreeMarkerContent browseUserHomeDirectory(
            @Path("username") final String username,
            final Session session) {
        return browseDirectory(username, ResourceManager.ROOT_PATH, session);
    }

    @RequestMapping(value = "^/browse/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$")
    public FreeMarkerContent browseDirectory(
            @Path("username") final String username,
            @Path("path") final String path,
            final Session session) {
        final String normalizedPath = normalizePath(username, path);

        final Resource resource = resourceManager_.getResourceAtPath(normalizedPath);
        if (resource == null) {
            throw new ResourceNotFoundException("Found no directory resource at path: "
                    + normalizedPath);
        } else if (!resourceFilter_.test(resource)) {
            throw new ResourceNotFoundException("Found no directory resource at path: "
                    + normalizedPath);
        }

        if (!Resource.Type.DIRECTORY.equals(resource.getType())) {
            throw new ResourceNotFoundException("Found no directory resource at path: "
                    + normalizedPath);
        } else if (Resource.Visibility.PRIVATE.equals(resource.getVisibility())) {
            // If the directory is a private directory, we have to ensure that the
            // authenticated user is the owner.
            if (session == null) {
                throw new ResourceNotFoundException("Found no directory resource at path: "
                        + normalizedPath);
            } else if (!session.getUsername().equals(resource.getOwner())) {
                throw new ResourceForbiddenException("Private directory not visible to authenticated user: "
                        + normalizedPath);
            }
        }

        final DirectoryListing listing = listDirectory(resource, session);

        final boolean userIsOwner = userIsOwner(resource, session);

        return new FreeMarkerContent.Builder("templates/browse.ftl")
                .withSession(session)
                .withAttr("view", "browse")
                .withAttr("resource", resource)
                .withAttr("breadcrumbs", splitNormalizedPathToElements(resource.getPath()))
                .withAttr("favorites", listing.getFavorites())
                .withAttr("nonFavorites", listing.getNonFavorites())
                .withAttr("allChildren", listing.getAll())
                .withAttr("directoryCount", listing.getDirectoryCount())
                .withAttr("fileCount", listing.getFileCount())
                .withAttr("totalFileDisplaySize", humanReadableByteCountBin(resource.getSize()))
                .withAttr("userIsOwner", userIsOwner)
                .build();
    }

}
