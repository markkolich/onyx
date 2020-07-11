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

import com.google.common.io.Resources;
import curacao.CuracaoContextListener;
import curacao.CuracaoDispatcherServlet;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import javax.annotation.Nullable;
import java.net.URL;

import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SECURITY;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;

public final class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private static final String CONTEXT_PATH = "/onyx";
    private static final String STATIC_SERVLET_MAPPING_UNDER_CONTEXT = "/static/*";
    private static final String CURACAO_SERVLET_MAPPING_UNDER_CONTEXT = "/*";

    public static final String CONTEXT_ATTRIBUTE_BASE_RESOURCE = "onyx.base-resource";

    @Option(names = {"--port"}, paramLabel = "PORT", description = "Server port.",
            defaultValue = "8080", required = true)
    private int port_;

    public static void main(
            final String... args) {
        try {
            final Application onyx = new Application();
            new CommandLine(onyx).parseArgs(args);

            final Server server = onyx.buildServer();
            server.start();
            server.join();
        } catch (final Exception e) {
            LOG.error("Onyx server startup failed.", e);
        }
    }

    private Server buildServer() throws Exception {
        // Setup a new queued thread pool.
        final QueuedThreadPool pool = new QueuedThreadPool();
        // Use substring(1) to strip the leading "/" on the context name.
        pool.setName("jetty-" + CONTEXT_PATH.substring(1));

        // Instantiate a new server instance using said thread pool.
        final Server server = new Server(pool);
        final HttpConfiguration config = new HttpConfiguration();
        config.setSendXPoweredBy(false); // Hide X-Powered-By: Jetty
        config.setSendServerVersion(false); // Hide Server: Jetty-9.z

        // Grab a NIO connector for the server.
        final HttpConnectionFactory factory = new HttpConnectionFactory(config);
        final ServerConnector connector = new ServerConnector(server, factory);
        connector.setPort(port_);
        connector.setIdleTimeout(30000L); // 30-seconds
        server.addConnector(connector);

        // Setup a new Servlet context based on our parameters.
        // See https://github.com/eclipse/jetty.project/issues/3963
        final WebAppContext context = new WebAppContext(server, CONTEXT_PATH, null, null, null, null,
                NO_SESSIONS | NO_SECURITY);
        // No sessions, and no security handlers.
        context.setSessionHandler(null);
        context.setContextPath(CONTEXT_PATH);
        // We do not use JSPs (Java Server Pages) so disable the defaults descriptor
        // which disables loading the JSP engine and slightly improves startup time.
        // http://jetty.4.x6.nabble.com/disable-jsp-engine-when-starting-jetty-td17393.html
        context.setDefaultsDescriptor(null);
        // Intentionally skip scanning JARs for Servlet 3 annotations.
        context.setAttribute(WebInfConfiguration.WEBINF_JAR_PATTERN, "^$");
        context.setAttribute(WebInfConfiguration.CONTAINER_JAR_PATTERN, "^$");

        final Resource baseResource = getBaseResourceForRuntime();
        context.setBaseResource(baseResource);
        // Attach the base resource to the context so any components or controllers that need
        // this at runtime can easily access it.
        context.setAttribute(CONTEXT_ATTRIBUTE_BASE_RESOURCE, baseResource);

        final ServletHolder defaultHolder = new ServletHolder("default", LocalCacheAwareDefaultServlet.class);
        defaultHolder.setAsyncSupported(true); // Async supported = true
        defaultHolder.setInitParameter(DefaultServlet.CONTEXT_INIT + "dirAllowed", "false");
        defaultHolder.setInitParameter(DefaultServlet.CONTEXT_INIT + "acceptRanges", "true");
        defaultHolder.setInitParameter(DefaultServlet.CONTEXT_INIT + "cacheControl", "public,max-age=3600");
        context.addServlet(defaultHolder, STATIC_SERVLET_MAPPING_UNDER_CONTEXT);

        final ServletHolder curacaoHolder = new ServletHolder("curacao", CuracaoDispatcherServlet.class);
        curacaoHolder.setAsyncSupported(true); // Async supported = true
        context.addEventListener(new CuracaoContextListener()); // Required
        context.addServlet(curacaoHolder, CURACAO_SERVLET_MAPPING_UNDER_CONTEXT);

        final ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
        errorHandler.addErrorPage(ErrorPageErrorHandler.GLOBAL_ERROR_PAGE, "/forward-errors");
        context.setErrorHandler(errorHandler);

        // Attach the context handler to the server, and go!
        server.setHandler(context);
        server.setStopAtShutdown(true);

        return server;
    }

    @Nullable
    private Resource getBaseResourceForRuntime() throws Exception {
        // In dev, the base resource will be something like "src/main/webapp".
        final Resource srcMainWebApp = Resource.newResource("src/main/webapp");
        if (srcMainWebApp.exists()) {
            return srcMainWebApp;
        }

        // In prod/deployment, the app runs within a JAR and so the base resource
        // will be a "webapp" directory within the fat JAR.
        final URL webApp = Resources.getResource("webapp");
        return Resource.newResource(webApp);
    }

}
