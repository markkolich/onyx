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

/**
 * Sends a redirect to a pre-signed S3 URL whose path carries percent-encoded percent signs, which is
 * what a download of a resource with a space or a non-ASCII character in its name actually redirects to.
 *
 * <p>Resource paths are stored percent-encoded, so an S3 key holds literal {@code %} characters, and the
 * SDK encodes each one as {@code %25} when it signs the URL: "foo bar.mp4" becomes {@code foo%2520bar.mp4}.
 * Jetty treats {@code %25} followed by hex as an ambiguous path encoding and, from Jetty 12, refuses to
 * send such a {@code Location} unless the redirect compliance allows it - where Jetty 11 sent it happily.
 * The failure is a 500 from deep inside {@code Response.sendRedirect}, and only for resources named this
 * way, so a deployment looks healthy until somebody downloads a file with a space in its name.
 */
public final class RedirectUriComplianceTest {

    private static final String CONTEXT_PATH = "/onyx";

    /**
     * A pre-signed URL for the S3 key {@code mark/%E0%A4%90.../anothercol/foo%20bar.mp4}, which is how
     * a resource named "foo bar.mp4" under a Devanagari-named directory is stored.
     */
    private static final String PRESIGNED_URL_WITH_ENCODED_PERCENTS =
            "https://bucket.s3.us-west-1.amazonaws.com/mark/%25E0%25A4%2590/anothercol/foo%2520bar.mp4"
            + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=deadbeef";

    /**
     * The same redirect Jetty refuses by default is sent without complaint once the connector is
     * configured the way the application configures it.
     */
    @Test
    public void redirectsToAPresignedUrlCarryingEncodedPercentsTest() throws Exception {
        final UriCompliance compliance = UriCompliance.DEFAULT_REDIRECT
                .with("onyx-redirect", UriCompliance.Violation.AMBIGUOUS_PATH_ENCODING);

        final HttpResponse<String> response = sendRedirectThrough(connector -> {
            final HttpConnectionFactory connectionFactory =
                    connector.getConnectionFactory(HttpConnectionFactory.class);
            connectionFactory.getHttpConfiguration().setRedirectUriCompliance(compliance);
        });

        assertEquals(302, response.statusCode());
        assertEquals(PRESIGNED_URL_WITH_ENCODED_PERCENTS,
                response.headers().firstValue("Location").orElse(null));
    }

    /**
     * And the same redirect against Jetty's untouched default, to show this test is testing something:
     * without the compliance change it is a 500, not a redirect.
     */
    @Test
    public void jettysDefaultRefusesTheSameRedirectTest() throws Exception {
        final HttpResponse<String> response = sendRedirectThrough(connector -> { });

        assertEquals(500, response.statusCode());
    }

    private static HttpResponse<String> sendRedirectThrough(
            final Consumer<ServerConnector> connectorCustomizer) throws Exception {
        final Server server = new Server();

        final ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        connectorCustomizer.accept(connector);
        server.addConnector(connector);

        final ServletContextHandler context = new ServletContextHandler(CONTEXT_PATH,
                ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);
        context.addServlet(new ServletHolder(new RedirectingServlet()), "/download/*");
        server.setHandler(context);

        server.start();
        try {
            final int port = connector.getLocalPort();

            return HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build()
                    .send(HttpRequest.newBuilder(URI.create(
                                    "http://127.0.0.1:" + port + CONTEXT_PATH + "/download/mark/clip.mp4"))
                            .build(),
                            HttpResponse.BodyHandlers.ofString());
        } finally {
            server.stop();
        }
    }

    /**
     * Stands in for the download controller, which ends in {@code response.sendRedirect(downloadUrl)}.
     */
    private static final class RedirectingServlet extends HttpServlet {

        @Override
        protected void doGet(
                final HttpServletRequest request,
                final HttpServletResponse response) throws IOException {
            response.sendRedirect(PRESIGNED_URL_WITH_ENCODED_PERCENTS);
        }

    }

}
