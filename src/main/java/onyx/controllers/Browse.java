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

package onyx.controllers;

import com.google.common.collect.ImmutableSet;
import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.Path;
import onyx.components.config.OnyxConfig;
import onyx.components.config.cache.LocalCacheConfig;
import onyx.components.storage.AssetManager;
import onyx.components.storage.AsynchronousResourcePool;
import onyx.components.storage.CacheManager;
import onyx.components.storage.ResourceManager;
import onyx.components.storage.ResourceManager.Extensions;
import onyx.entities.authentication.Session;
import onyx.entities.freemarker.FreeMarkerContent;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.resource.ResourceForbiddenException;
import onyx.exceptions.resource.ResourceNotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Triple;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.util.List;

import static onyx.util.PathUtils.normalizePath;
import static onyx.util.PathUtils.splitNormalizedPathToElements;

@Controller
public final class Browse extends AbstractOnyxController {

    private final LocalCacheConfig localCacheConfig_;

    private final AssetManager assetManager_;
    private final ResourceManager resourceManager_;
    private final CacheManager cacheManager_;

    @Injectable
    public Browse(
            final OnyxConfig onyxConfig,
            final AsynchronousResourcePool asynchronousResourcePool,
            final LocalCacheConfig localCacheConfig,
            final AssetManager assetManager,
            final ResourceManager resourceManager,
            final CacheManager cacheManager) {
        super(onyxConfig, asynchronousResourcePool);
        localCacheConfig_ = localCacheConfig;
        assetManager_ = assetManager;
        resourceManager_ = resourceManager;
        cacheManager_ = cacheManager;
    }

    @RequestMapping(value = "^/browse/(?<username>[a-zA-Z0-9]*)$")
    public FreeMarkerContent browseUserRoot(
            @Path("username") final String username,
            final Session session) {
        return browseDirectory(username, "/", session);
    }

    @RequestMapping(value = "^/browse/(?<username>[a-zA-Z0-9]*)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$")
    public FreeMarkerContent browseDirectory(
            @Path("username") final String username,
            @Path("path") final String path,
            final Session session) {
        final String normalizedPath = normalizePath(username, path);

        final Resource directory = resourceManager_.getResourceAtPath(normalizedPath);
        if (directory == null) {
            throw new ResourceNotFoundException("Found no directory resource at path: " + normalizedPath);
        }

        if (!Resource.Type.DIRECTORY.equals(directory.getType())) {
            throw new ResourceNotFoundException("Found no directory resource at path: " + path);
        } else if (Resource.Visibility.PRIVATE.equals(directory.getVisibility())) {
            // If the directory is a private directory, we have to ensure that the
            // authenticated user is the owner.
            if (session == null) {
                throw new ResourceNotFoundException("Found no directory resource at path: "
                        + normalizedPath);
            } else if (!session.getUsername().equals(directory.getOwner())) {
                throw new ResourceForbiddenException("Private directory not visible to authenticated user: "
                        + normalizedPath);
            }
        }

        // Whether or not the user is authenticated (has a valid session).
        final boolean userAuthenticated = (session != null);
        // Whether or not the user is authenticated (has a valid session) AND is
        // the directory resource owner.
        final boolean userIsOwner = (userAuthenticated && session.getUsername().equals(directory.getOwner()));

        final ImmutableSet.Builder<Resource.Visibility> visibility = ImmutableSet.builder();
        visibility.add(Resource.Visibility.PUBLIC);
        // Only show private resources inside of the directory if the authenticated user is
        // the directory resource owner.
        if (userIsOwner) {
            visibility.add(Resource.Visibility.PRIVATE);
        }

        final List<Resource> children = resourceManager_.listDirectory(directory, visibility.build(),
                // Sort only if the user is the directory resource owner.
                userIsOwner ? Extensions.Sort.FAVORITE : Extensions.Sort.NONE);
        final List<Triple<String, String, String>> breadcrumbs = splitNormalizedPathToElements(directory.getPath());

        final long directoryCount = children.stream()
                .filter(c -> Resource.Type.DIRECTORY.equals(c.getType()))
                .count();
        final long fileCount = children.stream()
                .filter(c -> Resource.Type.FILE.equals(c.getType()))
                .count();

        final long totalFileSize = children.stream()
                .filter(c -> Resource.Type.FILE.equals(c.getType()))
                .map(Resource::getSize)
                .mapToLong(Long::longValue)
                .sum();
        final String totalFileDisplaySize = FileUtils.byteCountToDisplaySize(totalFileSize);

        return new FreeMarkerContent.Builder("templates/browse.ftl")
                .withSession(session)
                .withAttr("breadcrumbs", breadcrumbs)
                .withAttr("directory", directory)
                .withAttr("children", children)
                .withAttr("directoryCount", directoryCount)
                .withAttr("fileCount", fileCount)
                .withAttr("totalFileDisplaySize", totalFileDisplaySize)
                .build();
    }

    @RequestMapping(value = "^/file/(?<username>[a-zA-Z0-9]*)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$")
    public void redirectToFileDownload(
            @Path("username") final String username,
            @Path("path") final String path,
            final Session session,
            final HttpServletResponse response,
            final AsyncContext context) throws Exception {
        final String normalizedPath = normalizePath(username, path);

        final Resource file = resourceManager_.getResourceAtPath(normalizedPath);
        if (file == null) {
            throw new ResourceNotFoundException("Found no file resource at path: " + normalizedPath);
        }

        if (!Resource.Type.FILE.equals(file.getType())) {
            throw new ResourceNotFoundException("Found no file resource at path: " + normalizedPath);
        } else if (Resource.Visibility.PRIVATE.equals(file.getVisibility())) {
            // If the file is a private file, we have to ensure that the authenticated user is the owner.
            if (session == null) {
                throw new ResourceNotFoundException("Found no file resource at path: "
                        + normalizedPath);
            } else if (!session.getUsername().equals(file.getOwner())) {
                throw new ResourceForbiddenException("Private file not visible to authenticated user: "
                        + normalizedPath);
            }
        }

        final URL downloadUrl;
        {
            final boolean localCacheEnabled = localCacheConfig_.localCacheEnabled();

            // Only favorite files are stored in the local cache.
            if (localCacheEnabled && file.getFavorite()) {
                // Attempt to resolve the file from the local cache first; then if the file
                // is not found in the cache, generate the S3 download URL.
                final URL cacheUrl = cacheManager_.getCachedDownloadUrlForResource(file);
                if (cacheUrl != null) {
                    // File was found in cache; send back cached resource URL.
                    downloadUrl = cacheUrl;
                } else {
                    // File was not found in cache; trigger a download of the file to the cache
                    // only if the resource has private visibility.
                    if (Resource.Visibility.PRIVATE.equals(file.getVisibility())) {
                        cacheManager_.downloadResourceToCacheAsync(file, executorService_);
                    }

                    downloadUrl = assetManager_.getPresignedDownloadUrlForResource(file);
                }
            } else {
                // Not a favorite file; would not be in the cache as only favorite files can
                // be stored locally. Generate the S3 download URL.
                downloadUrl = assetManager_.getPresignedDownloadUrlForResource(file);
            }
        }

        response.sendRedirect(downloadUrl.toString());
        context.complete();
    }

}
