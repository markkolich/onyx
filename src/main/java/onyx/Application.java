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

import com.kolich.buildversion.BuildVersion;
import curacao.jetty11.CuracaoJettyServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import javax.annotation.Nonnull;

public final class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private static final String CONTEXT_PATH = "/onyx";

    private static final String THREAD_NAME_FORMAT = "jetty-onyx-%d";

    private static final String STATIC_PATH_SPEC = "/static/*";
    private static final String STATIC_CACHE_CONTROL = "public,max-age=3600";

    private static final String STATIC_SERVLET_DIR_ALLOWED = "dirAllowed";
    private static final String STATIC_SERVLET_CACHE_CONTROL = "cacheControl";
    private static final String STATIC_SERVLET_ACCEPT_RANGES = "acceptRanges";

    /**
     * Every error the container sends (such as a static file's 404) is forwarded here; no controller maps
     * it, so Curacao answers with its not-found handling, which renders Onyx's own error page.
     */
    private static final String ERROR_PAGE = "/forward-errors";

    @Option(names = {"--host"}, paramLabel = "HOST", description = "Server host.",
            defaultValue = "127.0.0.1")
    private String host_;

    @Option(names = {"--port"}, paramLabel = "PORT", description = "Server port.",
            defaultValue = "8080", required = true)
    private int port_;

    public static void main(
            final String... args) {
        try {
            final BuildVersion buildVersion = BuildVersion.getInstance();
            LOG.info("Onyx-{}; built: {}; git: {}; jvm {}",
                    buildVersion.getVersion(),
                    buildVersion.getTimestamp(),
                    buildVersion.getBuildNumber(),
                    System.getProperty("java.runtime.version", System.getProperty("java.version")));

            final Application onyx = new Application();
            new CommandLine(onyx).parseArgs(args);

            final ServletHolder staticFiles = createLocaleCacheAwareStaticServletHolder();

            final Server server = new CuracaoJettyServer.Builder(CONTEXT_PATH)
                    .setThreadNameFormat(THREAD_NAME_FORMAT)
                    .setHost(onyx.host_)
                    .setPort(onyx.port_)
                    .addServlet(staticFiles, STATIC_PATH_SPEC)
                    .setErrorPage(ERROR_PAGE)
                    .build();

            server.start();
            server.join();
        } catch (final Exception e) {
            LOG.error("Onyx server startup failed.", e);
        }
    }

    @Nonnull
    private static ServletHolder createLocaleCacheAwareStaticServletHolder() {
        final ServletHolder staticFiles = new ServletHolder(LocalCacheAwareDefaultServlet.class);
        staticFiles.setAsyncSupported(true);

        // Jetty's DefaultServlet lists directories unless told not to.
        staticFiles.setInitParameter(STATIC_SERVLET_DIR_ALLOWED, Boolean.FALSE.toString());
        staticFiles.setInitParameter(STATIC_SERVLET_CACHE_CONTROL, STATIC_CACHE_CONTROL);
        staticFiles.setInitParameter(STATIC_SERVLET_ACCEPT_RANGES, Boolean.TRUE.toString());

        return staticFiles;
    }

}
