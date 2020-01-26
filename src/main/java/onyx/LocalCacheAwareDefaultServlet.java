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

package onyx;

import com.google.common.base.Splitter;
import curacao.CuracaoContextListener.CuracaoCoreObjectMap;
import onyx.components.config.OnyxConfig;
import onyx.components.storage.CacheManager;
import onyx.components.storage.cache.CachedResourceSigner;
import onyx.entities.storage.cache.CachedResourceToken;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import java.nio.file.Path;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class LocalCacheAwareDefaultServlet extends DefaultServlet {

    private static final Logger LOG = LoggerFactory.getLogger(LocalCacheAwareDefaultServlet.class);

    private static final Splitter SLASH_SPLITTER = Splitter.on("/").trimResults().omitEmptyStrings();

    private static final String LOCAL_CACHE_URI_PATH_PREFIX = "/static/cache/";

    @Override
    public Resource getResource(
            final String pathInContext) {
        if (!pathInContext.startsWith(LOCAL_CACHE_URI_PATH_PREFIX)) {
            return super.getResource(pathInContext);
        }

        final ServletContext context = getServletContext();

        // If the local cache is not enabled, immediately bail.
        final OnyxConfig onyxConfig = getComponentForServletContext(context, OnyxConfig.class);
        final boolean localCacheEnabled = onyxConfig.localCacheEnabled();
        if (!localCacheEnabled) {
            LOG.debug("Local resource cache not enabled; bye.");
            return null;
        }

        // Remove the cache prefix & extract the resource token from the path.
        final String resourceWithToken = StringUtils.removeStart(pathInContext, LOCAL_CACHE_URI_PATH_PREFIX);
        final List<String> tokens = SLASH_SPLITTER.splitToList(resourceWithToken);
        if (tokens.size() != 2) {
            return null;
        }

        // Validate the signed cache token.
        final String token = tokens.iterator().next();
        final CachedResourceSigner cachedResourceSigner =
                getComponentForServletContext(context, CachedResourceSigner.class);
        final CachedResourceToken cachedResourceToken =
                cachedResourceSigner.extractSignedCachedResourceToken(token);
        if (cachedResourceToken == null) {
            LOG.warn("Failed to validate signed cache token: {}", token);
            return null;
        }

        // Locate the cached asset/file for the resource token.
        final CacheManager cacheManager = getComponentForServletContext(context, CacheManager.class);
        final Path cachedResource = cacheManager.getCachedResourceFileForPath(cachedResourceToken.getPath());
        if (cachedResource == null) {
            LOG.warn("Got valid token, but found no asset/file in cache for path: {}",
                    cachedResourceToken.getPath());
            return null;
        }

        try {
            return Resource.newResource(cachedResource.toUri());
        } catch (final Exception e) {
            LOG.error("Failed to resolve cached resource file from path: {}",
                    cachedResourceToken.getPath(), e);
            return null;
        }
    }

    @Nonnull
    @SuppressWarnings("unchecked") // intentional & safe
    private static <T> T getComponentForServletContext(
            final ServletContext servletContext,
            final Class<T> componentClass) {
        final CuracaoCoreObjectMap coreObjectMap = CuracaoCoreObjectMap.objectMapFromContext(servletContext);
        checkNotNull(coreObjectMap, "Curacao core object map should not be null; context not initialized?");

        final T component = (T) coreObjectMap.componentTable_.getComponentForType(componentClass);
        checkNotNull(component, "Failed to find component for class: " + componentClass.getCanonicalName()
                + "; context not initialized?");

        return component;
    }

}
