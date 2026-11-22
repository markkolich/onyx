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

import jakarta.servlet.ServletException;
import org.eclipse.jetty.ee11.servlet.ResourceServlet;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.http.content.HttpContent;
import org.eclipse.jetty.http.content.ResourceHttpContent;
import org.eclipse.jetty.server.AllowedResourceAliasChecker;
import org.eclipse.jetty.server.ResourceService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SymlinkAllowedResourceAliasChecker;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the mechanism {@link LocalCacheAwareResourceServlet} uses to serve a file that lives
 * outside the webapp: wrapping the {@link ResourceService}'s {@link HttpContent.Factory}, and
 * registering alias checkers scoped to the cache directory.
 *
 * <p>It stands in for the real servlet rather than driving it, because the real one reads its
 * cache configuration, signer and cache manager out of Curacao's component context, which cannot
 * be assembled without booting the whole application and its AWS clients. What is reproduced here
 * is the Jetty-facing half, which is the half that broke twice and the half a Jetty upgrade will
 * break again. The token validation the real servlet layers on top is ordinary code over
 * {@code CachedResourceSigner}, already covered by its own tests.
 *
 * <p>The trap this exists to catch: Jetty 12 alias-checks every resource a content factory returns,
 * against the context rather than against the directory the factory is serving from. A cached file
 * reached through a symlink is an alias of a path outside the context, so it is refused and the
 * request 404s - and the cache directory is a symlink exactly when it points at external storage,
 * which is the deployment that matters and never the development machine. So the cache here is
 * deliberately served through a symlink.
 */
public final class CachedResourceContentFactoryTest {

    private static final String CONTEXT_PATH = "/onyx";
    private static final String STATIC_PATH_SPEC = "/static/*";

    private static final String CACHE_URI_PREFIX = "/static/cache/";
    private static final String VALID_TOKEN = "a-validly-signed-token";

    private static final String CACHED_CONTENT = "pretend this is a video\n".repeat(500);
    private static final String WEBAPP_CONTENT = "body { color: rebeccapurple; }\n";

    @TempDir
    /* package */ static Path TEMP;

    /**
     * Set once the temporary directory exists, and read by the servlet below, which Jetty
     * instantiates. A holder rather than a plain static so it can stay final.
     */
    private static final AtomicReference<Path> CACHED_FILE = new AtomicReference<>();
    private static Server SERVER;
    private static HttpClient CLIENT;

    /**
     * Resolves a cache request the way the real servlet does: it recognizes the cache prefix,
     * treats the first path segment as the token and the second as the resource's name, and serves
     * the cache file the token names - using the name only to decide a content type, since cache
     * files are stored under a hash with no extension.
     */
    public static final class StubCachedResourceServlet extends ResourceServlet {

        @Override
        public void init() throws ServletException {
            super.init();

            final ServletContextHandler contextHandler = initContextHandler(getServletContext());
            final org.eclipse.jetty.util.resource.Resource cacheDirectory =
                    ResourceFactory.root().newResource(CACHED_FILE.get().getParent());
            contextHandler.addAliasCheck(new AllowedResourceAliasChecker(contextHandler, cacheDirectory));
            contextHandler.addAliasCheck(new SymlinkAllowedResourceAliasChecker(contextHandler, cacheDirectory));

            final ResourceService resourceService = getResourceService();
            final HttpContent.Factory webappContentFactory = resourceService.getHttpContentFactory();

            resourceService.setHttpContentFactory(pathInContext -> {
                final HttpContent cachedContent = cachedContent(pathInContext);

                return (cachedContent != null) ? cachedContent : webappContentFactory.getContent(pathInContext);
            });
        }

        private HttpContent cachedContent(
                final String pathInContext) {
            if (pathInContext == null || !pathInContext.startsWith(CACHE_URI_PREFIX)) {
                return null;
            }

            final String[] segments = pathInContext.substring(CACHE_URI_PREFIX.length()).split("/");
            if (segments.length != 2 || !VALID_TOKEN.equals(segments[0])) {
                return null;
            }

            return new ResourceHttpContent(ResourceFactory.root().newResource(CACHED_FILE.get()),
                    getServletContext().getMimeType(segments[1]));
        }

    }

    @BeforeAll
    public static void startServer() throws Exception {
        final Path webapp = Files.createDirectories(TEMP.resolve("webapp").resolve("static"));
        Files.writeString(webapp.resolve("app.css"), WEBAPP_CONTENT, StandardCharsets.UTF_8);

        // The cache is written through its real path but served through a symlink to it, which is
        // how a cache directory pointing at external storage behaves.
        final Path realCacheDirectory = Files.createDirectories(TEMP.resolve("cache-real"));
        final Path linkedCacheDirectory = Files.createSymbolicLink(TEMP.resolve("cache-link"),
                realCacheDirectory);
        Files.writeString(realCacheDirectory.resolve("a3f9beefcafe"), CACHED_CONTENT,
                StandardCharsets.UTF_8);
        CACHED_FILE.set(linkedCacheDirectory.resolve("a3f9beefcafe"));

        assertTrue(Files.isSymbolicLink(linkedCacheDirectory), "the cache has to be served via a symlink");

        final ServletHolder staticFiles = new ServletHolder(StubCachedResourceServlet.class);
        staticFiles.setInitParameter("dirAllowed", Boolean.FALSE.toString());
        staticFiles.setInitParameter("acceptRanges", Boolean.TRUE.toString());
        staticFiles.setInitParameter("pathInfoOnly", Boolean.FALSE.toString());

        SERVER = new Server(0);

        final ServletContextHandler context = new ServletContextHandler(CONTEXT_PATH,
                ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);
        context.setBaseResource(ResourceFactory.root().newResource(TEMP.resolve("webapp")));
        context.addServlet(staticFiles, STATIC_PATH_SPEC);
        SERVER.setHandler(context);

        SERVER.start();
        CLIENT = HttpClient.newHttpClient();
    }

    @AfterAll
    public static void stopServer() throws Exception {
        if (SERVER != null) {
            SERVER.stop();
        }
    }

    @Test
    public void servesACachedFileFromOutsideTheWebappTest() throws Exception {
        final HttpResponse<String> response = get(CACHE_URI_PREFIX + VALID_TOKEN + "/clip.mp4");

        assertEquals(200, response.statusCode());
        assertEquals(CACHED_CONTENT, response.body());
        // Taken from the name in the URL, since the file on disk is a hash with no extension.
        assertEquals("video/mp4", response.headers().firstValue("Content-Type").orElse(null));
        assertNotNull(response.headers().firstValue("Last-Modified").orElse(null));
    }

    /**
     * Serving through the content factory, rather than writing the response by hand, is what keeps
     * range requests working - and seeking is the whole point of caching large media locally.
     */
    @Test
    public void servesARangeOfACachedFileTest() throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(uri(CACHE_URI_PREFIX + VALID_TOKEN + "/clip.mp4"))
                .header("Range", "bytes=100-199")
                .build();
        final HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(206, response.statusCode());
        assertEquals(100, response.body().length());
        assertEquals(CACHED_CONTENT.substring(100, 200), response.body());
        assertEquals("bytes 100-199/" + CACHED_CONTENT.length(),
                response.headers().firstValue("Content-Range").orElse(null));
    }

    @Test
    public void refusedTokenFallsThroughToNotFoundTest() throws Exception {
        assertEquals(404, get(CACHE_URI_PREFIX + "not-the-signed-token/clip.mp4").statusCode());
    }

    /**
     * The token is the only way to reach a cached file. The cache directory is never a base
     * resource, so asking for a cache file by its hashed name under the static path must not
     * resolve, however the alias checkers are registered.
     */
    @Test
    public void cachedFileIsNotReachableWithoutATokenTest() throws Exception {
        assertEquals(404, get("/static/a3f9beefcafe").statusCode());
        assertEquals(404, get("/static/cache/a3f9beefcafe").statusCode());
    }

    @Test
    public void nonCacheRequestsStillReachTheWebappTest() throws Exception {
        final HttpResponse<String> response = get("/static/app.css");

        assertEquals(200, response.statusCode());
        assertEquals(WEBAPP_CONTENT, response.body());
    }

    private static HttpResponse<String> get(
            final String path) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(uri(path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static URI uri(
            final String path) {
        final int port = ((ServerConnector) SERVER.getConnectors()[0]).getLocalPort();

        return URI.create("http://127.0.0.1:" + port + CONTEXT_PATH + path);
    }

}
