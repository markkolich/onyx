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

package onyx;

import com.google.common.base.Splitter;
import com.google.common.net.MediaType;
import curacao.servlet.jakarta.JakartaServletContext;
import curacao.util.http.ContentTypes;
import jakarta.servlet.ServletException;
import onyx.components.config.cache.LocalCacheConfig;
import onyx.components.storage.CacheManager;
import onyx.components.storage.cache.CachedResourceSigner;
import onyx.entities.storage.cache.CachedResourceToken;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Strings;
import org.eclipse.jetty.ee11.servlet.ResourceServlet;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.http.content.HttpContent;
import org.eclipse.jetty.http.content.ResourceHttpContent;
import org.eclipse.jetty.server.AllowedResourceAliasChecker;
import org.eclipse.jetty.server.ResourceService;
import org.eclipse.jetty.server.SymlinkAllowedResourceAliasChecker;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.core.CuracaoCoreObjectMap.getComponentFromContext;

/**
 * Serves the webapp's static files, and additionally serves a locally cached resource when the request
 * carries a valid signed cache token under {@link #LOCAL_CACHE_URI_PATH_PREFIX}.
 *
 * <p>Cached files live outside the webapp directory, so they cannot be reached by resolving a path against
 * the context's base resource. Jetty 11 allowed a {@code DefaultServlet} subclass to override
 * {@code getResource(String)} and hand back any resource it liked; Jetty 12 removed that hook and resolves
 * content through a {@link ResourceService} and its {@link HttpContent.Factory} instead. So this wraps that
 * factory: cache requests are answered from the cache directory, and everything else falls through to the
 * factory Jetty built, which serves the webapp as usual.
 *
 * <p>Going through the content factory, rather than answering the request directly, is what keeps range
 * requests, conditional GETs, {@code Last-Modified} and content-type negotiation working for cached files -
 * all of which matter, since the cache holds the large media files that clients seek around in.
 *
 * <p>The signed token remains the only way to reach a cached file. The cache directory is never a base
 * resource, so no plain static path can resolve into it, which is what keeps a private cached file from
 * being served to anyone who can guess its hashed name.
 */
public final class LocalCacheAwareResourceServlet extends ResourceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(LocalCacheAwareResourceServlet.class);

    private static final Splitter SLASH_SPLITTER = Splitter.on("/").trimResults().omitEmptyStrings();

    private static final String LOCAL_CACHE_URI_PATH_PREFIX = "/static/cache/";

    private static final String DEFAULT_CONTENT_TYPE = MediaType.OCTET_STREAM.toString();

    /**
     * A cache request's path, after the prefix: the signed token and then the resource's name.
     */
    private static final int CACHE_PATH_TOKEN_COUNT = 2;

    /**
     * Whether the cache directory's alias checkers have been registered on the context yet. They
     * are registered on the first cache request rather than in {@link #init()}, deliberately:
     * finding the cache directory means asking Curacao for a component, and a servlet that did
     * that while initializing would refuse to serve the webapp's static files at all whenever the
     * application had not booted. Only cache requests depend on Curacao this way.
     */
    private final AtomicBoolean cacheAliasesAllowed_ = new AtomicBoolean(false);

    @Override
    public void init() throws ServletException {
        super.init();

        final ResourceService resourceService = getResourceService();

        // The factory Jetty assembled for the webapp's base resource, which stays responsible for
        // everything that is not a cache request.
        final HttpContent.Factory webappContentFactory = resourceService.getHttpContentFactory();

        resourceService.setHttpContentFactory(pathInContext -> {
            final HttpContent cachedContent = getCachedContent(pathInContext);
            if (cachedContent != null) {
                return cachedContent;
            }

            return webappContentFactory.getContent(pathInContext);
        });
    }

    /**
     * Permits the context to serve resources that live under the local cache directory.
     *
     * <p>{@link ResourceService} alias-checks every resource its content factory hands back, against the
     * context rather than against whatever the factory thinks it is serving. A cached file reached through
     * a symlink - which is exactly how the cache is laid out when its directory points at external storage
     * - counts as an alias of a path outside the context, so without this the cache would answer 404 on a
     * deployment whose cache directory is a symlink, while working perfectly on one where it is not.
     *
     * <p>Both checkers are deliberately scoped to the cache directory, not to the context as a whole: the
     * context keeps refusing aliases everywhere else, so this does not reopen the symlink escapes that the
     * alias checking exists to prevent.
     */
    private void allowAliasesUnderCacheDirectory(
            final Path cacheDirectory) {
        final ServletContextHandler contextHandler = initContextHandler(getServletContext());
        final Resource cacheResource = ResourceFactory.root().newResource(cacheDirectory);

        contextHandler.addAliasCheck(new AllowedResourceAliasChecker(contextHandler, cacheResource));
        contextHandler.addAliasCheck(new SymlinkAllowedResourceAliasChecker(contextHandler, cacheResource));
    }

    private curacao.core.servlet.ServletContext curacaoServletContext() {
        return new JakartaServletContext(getServletContext());
    }

    /**
     * Returns the cached file the given path's signed token names, or null when the path is not a cache
     * request, the cache is disabled, the token does not verify or has expired, or the file is not in the
     * cache - in which case the caller falls through to the webapp, which answers 404.
     */
    @Nullable
    private HttpContent getCachedContent(
            final String pathInContext) {
        if (pathInContext == null || !pathInContext.startsWith(LOCAL_CACHE_URI_PATH_PREFIX)) {
            return null;
        }

        final curacao.core.servlet.ServletContext curacaoServletContext = curacaoServletContext();

        // If the local cache is not enabled, immediately bail.
        final LocalCacheConfig localCacheConfig =
                getComponentFromContext(curacaoServletContext, LocalCacheConfig.class);
        checkNotNull(localCacheConfig, "Local cache config cannot be null; context not initialized?");
        final boolean localCacheEnabled = localCacheConfig.localCacheEnabled();
        if (!localCacheEnabled) {
            LOG.debug("Local resource cache not enabled; bye.");
            return null;
        }

        if (cacheAliasesAllowed_.compareAndSet(false, true)) {
            allowAliasesUnderCacheDirectory(localCacheConfig.getLocalCacheDirectory());
        }

        // Remove the cache prefix & extract the resource token from the path. Note the resource name
        // that follows the token is only counted, not read: it is there so the browser and any
        // downstream proxy see a real filename, and the token alone says which file to serve.
        final String resourceWithToken = Strings.CS.removeStart(pathInContext, LOCAL_CACHE_URI_PATH_PREFIX);
        final List<String> tokens = SLASH_SPLITTER.splitToList(resourceWithToken);
        if (tokens.size() != CACHE_PATH_TOKEN_COUNT) {
            return null;
        }

        // Validate the signed cache token.
        final CachedResourceSigner cachedResourceSigner =
                getComponentFromContext(curacaoServletContext, CachedResourceSigner.class);
        checkNotNull(cachedResourceSigner, "Cached resource signer cannot be null; context not initialized?");
        final String token = tokens.get(0);
        final CachedResourceToken cachedResourceToken =
                cachedResourceSigner.extractSignedCachedResourceToken(token);
        if (cachedResourceToken == null) {
            LOG.warn("Failed to validate signed cache token.");
            return null;
        }

        // Locate the cached asset/file for the resource token.
        final CacheManager cacheManager =
                getComponentFromContext(curacaoServletContext, CacheManager.class);
        checkNotNull(cacheManager, "Cache manager cannot be null; context not initialized?");
        final boolean hasResourceInCache = cacheManager.hasResourceInCache(cachedResourceToken.getPath());
        if (!hasResourceInCache) {
            LOG.warn("Got valid token, but found no asset/file in cache for path: {}",
                    cachedResourceToken.getPath());
            return null;
        }

        final Path cachedResource = cacheManager.getCachedFileForResource(cachedResourceToken.getPath());

        // Root-scoped, so the resource is not released underneath a response still being written; a
        // cached file is a plain path on disk and holds nothing that needs reclaiming.
        final Resource resource = ResourceFactory.root().newResource(cachedResource);

        // Cache files are stored under their hashed name, with no extension, so the content type comes
        // from the resource name in the URL instead.
        final String extension = FilenameUtils.getExtension(tokens.get(1)).toLowerCase();
        final String contentType =
                ContentTypes.getContentTypeForExtension(extension, DEFAULT_CONTENT_TYPE);

        return new ResourceHttpContent(resource, contentType);
    }

}
