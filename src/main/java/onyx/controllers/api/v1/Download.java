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

package onyx.controllers.api.v1;

import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.Path;
import curacao.annotations.parameters.Query;
import curacao.core.servlet.AsyncContext;
import curacao.core.servlet.HttpResponse;
import onyx.components.config.OnyxConfig;
import onyx.components.config.cache.LocalCacheConfig;
import onyx.components.storage.AssetManager;
import onyx.components.storage.CacheManager;
import onyx.components.storage.ResourceManager;
import onyx.controllers.api.AbstractOnyxApiController;
import onyx.entities.authentication.Session;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.api.ApiForbiddenException;
import onyx.exceptions.api.ApiNotFoundException;
import org.apache.commons.lang3.BooleanUtils;

import java.net.URL;
import java.time.Instant;

import static curacao.annotations.RequestMapping.Method.GET;
import static onyx.util.UserUtils.userIsNotOwner;
import static onyx.util.PathUtils.normalizePath;

@Controller
public final class Download extends AbstractOnyxApiController {

    private final LocalCacheConfig localCacheConfig_;

    private final ResourceManager resourceManager_;
    private final AssetManager assetManager_;
    private final CacheManager cacheManager_;

    @Injectable
    public Download(
            final OnyxConfig onyxConfig,
            final LocalCacheConfig localCacheConfig,
            final ResourceManager resourceManager,
            final AssetManager assetManager,
            final CacheManager cacheManager) {
        super(onyxConfig);
        localCacheConfig_ = localCacheConfig;
        resourceManager_ = resourceManager;
        assetManager_ = assetManager;
        cacheManager_ = cacheManager;
    }

    @RequestMapping(value = "^/api/v1/download/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = GET)
    public void downloadFile(
            @Path("username") final String username,
            @Path("path") final String path,
            @Query("nocache") final Boolean noCache,
            final Session session,
            final HttpResponse response,
            final AsyncContext context) throws Exception {
        final String normalizedPath = normalizePath(username, path);

        final Resource file = resourceManager_.getResourceAtPath(normalizedPath);
        if (file == null) {
            throw new ApiNotFoundException("Found no file resource at path: "
                    + normalizedPath);
        }

        if (!Resource.Type.FILE.equals(file.getType())) {
            throw new ApiNotFoundException("Found no file resource at path: "
                    + normalizedPath);
        } else if (Resource.Visibility.PRIVATE.equals(file.getVisibility())) {
            // If the file is a private file, we have to ensure that the authenticated user is the owner.
            if (session == null) {
                throw new ApiNotFoundException("Found no file resource at path: "
                        + normalizedPath);
            } else if (userIsNotOwner(file, session)) {
                throw new ApiForbiddenException("Private file not visible to authenticated user: "
                        + normalizedPath);
            }
        }

        final URL downloadUrl;
        {
            final boolean localCacheEnabled = localCacheConfig_.localCacheEnabled();

            final boolean skipCache = BooleanUtils.toBooleanDefaultIfNull(noCache, false);

            // Only favorite files are stored in the local cache.
            if (!skipCache && localCacheEnabled && file.getFavorite()) {
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
                        cacheManager_.downloadResourceToCacheAsync(file);
                    }

                    downloadUrl = assetManager_.getPresignedDownloadUrlForResource(file);
                }
            } else {
                // Not a favorite file; would not be in the cache as only favorite files can
                // be stored locally. Generate the S3 download URL.
                downloadUrl = assetManager_.getPresignedDownloadUrlForResource(file);
            }
        }

        file.setLastAccessedAt(Instant.now()); // now
        resourceManager_.updateResourceAsync(file);

        response.sendRedirect(downloadUrl.toString());
        context.complete();
    }

}
