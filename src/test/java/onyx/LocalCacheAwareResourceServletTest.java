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

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Serves static files out of a webapp directory through {@link LocalCacheAwareResourceServlet} on a
 * real server, over real HTTP.
 *
 * <p>Jetty resolves a static file from several things that are invisible to the compiler: which
 * servlet class is mapped, the init parameters it is given, and how much of the request path it
 * considers. Jetty 12 changed all three - {@code DefaultServlet} became the container's default
 * mapping only, static files at a prefix moved to {@code ResourceServlet}, and {@code pathInfoOnly}
 * flipped to default true, which silently serves {@code /static/app.css} from one directory too
 * high. Nothing about that fails to compile, and the application's own unit tests never ask the
 * server for a file, so the only way to know is to serve one.
 *
 * <p>This covers the servlet's pass-through half. Its other half, serving a cached file from a
 * signed token, needs Curacao's component context and is covered by
 * {@link CachedResourceContentFactoryTest}.
 */
public final class LocalCacheAwareResourceServletTest {

    private static final String CONTEXT_PATH = "/onyx";
    private static final String STATIC_PATH_SPEC = "/static/*";
    private static final String CACHE_CONTROL = "public,max-age=3600";

    private static final String FILE_CONTENT = "body { color: rebeccapurple; }\n";

    @TempDir
    /* package */ static Path WEBAPP;

    private static Server SERVER;
    private static HttpClient CLIENT;

    @BeforeAll
    public static void startServer() throws Exception {
        // A nested directory, so that a servlet serving one level too high cannot find it by luck.
        final Path cssDirectory = Files.createDirectories(WEBAPP.resolve("static").resolve("css"));
        Files.writeString(cssDirectory.resolve("app.css"), FILE_CONTENT, StandardCharsets.UTF_8);

        final ServletHolder staticFiles = new ServletHolder(LocalCacheAwareResourceServlet.class);
        staticFiles.setAsyncSupported(true);
        staticFiles.setInitParameter("dirAllowed", Boolean.FALSE.toString());
        staticFiles.setInitParameter("cacheControl", CACHE_CONTROL);
        staticFiles.setInitParameter("acceptRanges", Boolean.TRUE.toString());
        staticFiles.setInitParameter("pathInfoOnly", Boolean.FALSE.toString());

        SERVER = new Server(0);

        final ServletContextHandler context = new ServletContextHandler(CONTEXT_PATH,
                ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);
        context.setBaseResource(ResourceFactory.root().newResource(WEBAPP));
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

    /**
     * The path under the servlet's mapping has to be kept whole. With {@code pathInfoOnly} left at
     * Jetty 12's default, this request would look for the file in the webapp root and 404.
     */
    @Test
    public void servesAStaticFileFromTheFullPathTest() throws Exception {
        final HttpResponse<String> response = get("/static/css/app.css");

        assertEquals(200, response.statusCode());
        assertEquals(FILE_CONTENT, response.body());
        assertEquals(CACHE_CONTROL, response.headers().firstValue("Cache-Control").orElse(null));
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/css"),
                "unexpected content type: " + response.headers().firstValue("Content-Type").orElse("-"));
    }

    /**
     * Cached files are large media that clients seek around in, so range support is not optional.
     */
    @Test
    public void servesARangeOfAStaticFileTest() throws Exception {
        final HttpResponse<String> response = CLIENT.send(
                HttpRequest.newBuilder(uri("/static/css/app.css")).header("Range", "bytes=0-6").build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(206, response.statusCode());
        assertEquals(FILE_CONTENT.substring(0, 7), response.body());
        assertEquals("bytes 0-6/" + FILE_CONTENT.length(),
                response.headers().firstValue("Content-Range").orElse(null));
    }

    @Test
    public void missingStaticFileIsNotFoundTest() throws Exception {
        assertEquals(404, get("/static/css/does-not-exist.css").statusCode());
    }

    /**
     * {@code dirAllowed} is off, so a directory must not come back as a listing of its contents.
     */
    @Test
    public void directoryListingIsRefusedTest() throws Exception {
        final HttpResponse<String> response = get("/static/css/");

        assertTrue(response.statusCode() == 403 || response.statusCode() == 404,
                "expected a refusal, got " + response.statusCode() + ": " + response.body());
        assertFalseContains(response.body(), "app.css");
    }

    private static void assertFalseContains(
            final String body,
            final String needle) {
        assertTrue(!body.contains(needle), "response leaked a directory listing: " + body);
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
