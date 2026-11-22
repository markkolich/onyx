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

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Requests for a resource whose stored name contains a percent sign, which is every resource with
 * a space or a non-ASCII character in its name.
 *
 * <p>Onyx stores a path already percent-encoded, so a file called "foobar 06.jpeg" is stored as
 * {@code foobar%2006.jpeg}. Addressing it in a URL means encoding that percent in turn, giving
 * {@code foobar%252006.jpeg}. Jetty decodes {@code %25} to a percent, sees a percent sequence still
 * in the decoded path, and calls that {@link UriCompliance.Violation#AMBIGUOUS_PATH_ENCODING} -
 * rejecting the request with a 400 before any controller runs.
 *
 * <p>This is the inbound twin of {@link RedirectUriComplianceTest}: the same violation, refused on
 * the way in by {@code setUriCompliance} rather than on the way out by
 * {@code setRedirectUriCompliance}. Allowing it inbound is only safe because nothing in Onyx
 * resolves a decoded path against a file system - routes match the raw encoded path, DynamoDB and
 * S3 are keyed by it, and the local cache names files by its SHA-256.
 */
public final class RequestUriComplianceTest {

    private static final String CONTEXT_PATH = "/onyx";

    /**
     * "/mark/foo/bar/foobar 06.jpeg" as Onyx stores it, then encoded for a URL.
     */
    private static final String REQUEST_PATH = "/details/mark/foo/bar/foobar%252006.jpeg";

    /**
     * What a controller must see: one level decoded, which is the stored path.
     */
    private static final String STORED_PATH = "/details/mark/foo/bar/foobar%2006.jpeg";

    private static final UriCompliance ONYX_URI_COMPLIANCE = UriCompliance.DEFAULT
            .with("onyx", UriCompliance.Violation.AMBIGUOUS_PATH_ENCODING);

    /**
     * Jetty's untouched default, to show what is actually being fixed.
     */
    @Test
    public void jettysDefaultRefusesAPercentEncodedPercentTest() throws Exception {
        final HttpResponse<String> response = get(connector -> { }, context -> { });

        assertEquals(400, response.statusCode(),
                "expected Jetty to refuse the ambiguous path encoding");
    }

    @Test
    public void allowingTheViolationLetsTheRequestThroughTest() throws Exception {
        final HttpResponse<String> response = get(connector -> {
            connector.getConnectionFactory(HttpConnectionFactory.class)
                    .getHttpConfiguration()
                    .setUriCompliance(ONYX_URI_COMPLIANCE);
        }, context -> {
            context.getServletHandler().setDecodeAmbiguousURIs(true);
        });

        assertEquals(200, response.statusCode());
        // The decoded path is the stored one, so Onyx's route patterns and DynamoDB keys line up.
        assertEquals(STORED_PATH, response.body(),
                "the servlet did not see the stored, singly-decoded path");
    }

    /**
     * The connector setting on its own is not enough, and this is the part that is easy to get
     * wrong: with the violation allowed at the HTTP layer the request reaches the servlet layer,
     * which refuses it separately because {@code getServletPath()} and {@code getPathInfo()}
     * cannot represent an ambiguous path. Jetty says so in
     * {@code ServletHandler.setDecodeAmbiguousURIs}'s own javadoc.
     */
    @Test
    public void connectorComplianceAloneIsNotEnoughTest() throws Exception {
        final HttpResponse<String> response = get(connector -> {
            connector.getConnectionFactory(HttpConnectionFactory.class)
                    .getHttpConfiguration()
                    .setUriCompliance(ONYX_URI_COMPLIANCE);
        }, context -> { });

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("AMBIGUOUS_PATH_ENCODING"),
                "expected the servlet layer's own rejection: " + response.body());
    }

    private static HttpResponse<String> get(
            final Consumer<ServerConnector> connectorCustomizer,
            final Consumer<ServletContextHandler> contextCustomizer) throws Exception {
        final Server server = new Server();

        final ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        connectorCustomizer.accept(connector);
        server.addConnector(connector);

        final ServletContextHandler context = new ServletContextHandler(CONTEXT_PATH,
                ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);
        context.addServlet(new ServletHolder(new EchoPathServlet()), "/details/*");
        contextCustomizer.accept(context);
        server.setHandler(context);

        server.start();
        try {
            final int port = connector.getLocalPort();

            return HttpClient.newBuilder()
                    .build()
                    .send(HttpRequest.newBuilder(URI.create(
                                    "http://127.0.0.1:" + port + CONTEXT_PATH + REQUEST_PATH))
                            .build(),
                            HttpResponse.BodyHandlers.ofString());
        } finally {
            server.stop();
        }
    }

    /**
     * Answers with the path it was given, so the test can assert how far Jetty decoded it.
     */
    private static final class EchoPathServlet extends HttpServlet {

        @Override
        protected void doGet(
                final HttpServletRequest request,
                final HttpServletResponse response) throws IOException {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print(request.getServletPath() + request.getPathInfo());
        }

    }

}
