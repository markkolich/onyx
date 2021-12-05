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

import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.Path;
import curacao.util.ContentTypes;
import onyx.components.config.OnyxConfig;
import onyx.components.config.cache.LocalCacheConfig;
import onyx.components.storage.CacheManager;
import onyx.components.storage.ResourceManager;
import onyx.entities.authentication.Session;
import onyx.entities.freemarker.FreeMarkerContent;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.resource.ResourceForbiddenException;
import onyx.exceptions.resource.ResourceNotFoundException;
import org.apache.commons.io.FilenameUtils;

import java.util.List;

import static onyx.util.FileUtils.humanReadableByteCountBin;
import static onyx.util.PathUtils.normalizePath;
import static onyx.util.PathUtils.splitNormalizedPathToElements;

@Controller
public final class Details extends AbstractOnyxFreeMarkerController {

    private static final String DEFAULT_CONTENT_TYPE = MediaType.OCTET_STREAM.toString();

    private final LocalCacheConfig localCacheConfig_;

    private final CacheManager cacheManager_;

    @Injectable
    public Details(
            final OnyxConfig onyxConfig,
            final LocalCacheConfig localCacheConfig,
            final ResourceManager resourceManager,
            final CacheManager cacheManager) {
        super(onyxConfig, resourceManager);
        localCacheConfig_ = localCacheConfig;
        cacheManager_ = cacheManager;
    }

    @RequestMapping(value = "^/details/(?<username>[a-zA-Z0-9]+)$")
    public FreeMarkerContent resourceDetailsHomeDirectory(
            @Path("username") final String username,
            final Session session) {
        return resourceDetails(username, ResourceManager.ROOT_PATH, session);
    }

    @RequestMapping(value = "^/details/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$")
    public FreeMarkerContent resourceDetails(
            @Path("username") final String username,
            @Path("path") final String path,
            final Session session) {
        final String normalizedPath = normalizePath(username, path);

        final Resource resource = resourceManager_.getResourceAtPath(normalizedPath);
        if (resource == null) {
            throw new ResourceNotFoundException("Found no resource at path: "
                    + normalizedPath);
        }

        if (Resource.Visibility.PRIVATE.equals(resource.getVisibility())) {
            // If the resource is private, we have to ensure that the authenticated
            // user is the owner.
            if (session == null) {
                throw new ResourceNotFoundException("Found no resource at path: "
                        + normalizedPath);
            } else if (!session.getUsername().equals(resource.getOwner())) {
                throw new ResourceForbiddenException("Private resource not visible to authenticated user: "
                        + normalizedPath);
            }
        }

        // Whether or not the user is authenticated (has a valid session).
        final boolean userAuthenticated = (session != null);
        // Whether or not the user is authenticated (has a valid session)
        // AND is the resource owner.
        final boolean userIsOwner = (userAuthenticated && session.getUsername().equals(resource.getOwner()));

        // Only directories have children; any other resource explicitly have none.
        final List<Resource> children = (Resource.Type.DIRECTORY.equals(resource.getType())) ?
                listDirectory(resource, session) : ImmutableList.of();

        final String extension = FilenameUtils.getExtension(resource.getName()).toLowerCase();
        final String contentType = ContentTypes.getContentTypeForExtension(extension, DEFAULT_CONTENT_TYPE);

        final boolean localCacheEnabled = localCacheConfig_.localCacheEnabled();
        final boolean hasResourceInCache = (localCacheEnabled && userIsOwner)
                && cacheManager_.hasResourceInCache(resource);

        return new FreeMarkerContent.Builder("templates/details.ftl")
                .withSession(session)
                .withAttr("view", "details")
                .withAttr("resource", resource)
                .withAttr("breadcrumbs", splitNormalizedPathToElements(resource.getPath()))
                .withAttr("children", children)
                .withAttr("directoryCount", countDirectories(children))
                .withAttr("fileCount", countFiles(children))
                .withAttr("totalFileDisplaySize", humanReadableByteCountBin(resource.getSize()))
                .withAttr("contentType", contentType)
                .withAttr("userIsOwner", userIsOwner)
                .withAttr("hasResourceInCache", hasResourceInCache)
                .build();
    }

}
