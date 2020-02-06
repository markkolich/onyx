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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import curacao.entities.empty.StatusCodeOnlyCuracaoEntity;
import onyx.components.config.OnyxConfig;
import onyx.components.storage.AsynchronousResourcePool;
import org.apache.commons.lang3.tuple.Triple;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.amazonaws.util.SdkHttpUtils.urlDecode;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

public abstract class AbstractOnyxController {

    private static final Splitter SLASH_SPLITTER = Splitter.on("/").omitEmptyStrings().trimResults();

    protected final OnyxConfig onyxConfig_;

    protected final ExecutorService executorService_;

    protected AbstractOnyxController(
            final OnyxConfig onyxConfig,
            final AsynchronousResourcePool asynchronousResourcePool) {
        onyxConfig_ = onyxConfig;
        executorService_ = asynchronousResourcePool.getExecutorService();
    }

    /**
     * Given a username and a path, normalizes the path eliminating any unnecessary leading
     * or trailing slashes.
     */
    protected static String normalizePath(
            final String username,
            final String path) {
        final String normalizedPath;
        if (path.endsWith("/") && !"/".equals(path)) {
            normalizedPath = String.format("/%s/%s", username, path.substring(0, path.length() - 1));
        } else if ("".equals(path) || "/".equals(path)) {
            normalizedPath = String.format("/%s", username);
        } else {
            normalizedPath = String.format("/%s/%s", username, path);
        }

        return normalizedPath;
    }

    /**
     * Given a normalized path such as "/x/y/z" this method returns a list of triples
     * representing the unique paths to each element in the path, including the parents. E.g.,
     * <code>[("/", "/x", "x"), ("/x", "/x/y", "y"), ("/x/y", "/x/y/z", "z")]</code>
     */
    protected static List<Triple<String, String, String>> splitNormalizedPathToElements(
            final String normalizedPath) {
        final ImmutableList.Builder<Triple<String, String, String>> builder = ImmutableList.builder();

        final StringBuilder parentPathBuilder = new StringBuilder("/");
        final List<String> elements = SLASH_SPLITTER.splitToList(normalizedPath);
        for (int i = 0, l = elements.size(); i < l; i++) {
            final StringBuilder pathBuilder = new StringBuilder();
            for (int j = 0; j < i + 1; j++) {
                pathBuilder.append("/").append(elements.get(j));
                if (i < j) {
                    pathBuilder.append("/");
                }
            }

            final String parentPath = parentPathBuilder.toString();
            final String path = pathBuilder.toString();
            // IMPORTANT: note the URL decoding followed by HTML escaping to safely
            // decode and escape any non-HTML safe characters in a resource name.
            final String name = escapeHtml4(urlDecode(elements.get(i)));

            builder.add(Triple.of(parentPath, path, name));

            if (i > 0) {
                parentPathBuilder.append("/");
            }
            parentPathBuilder.append(elements.get(i));
        }

        return builder.build();
    }

    /**
     * Returns an empty response entity with the given HTTP response status code.
     */
    protected static final StatusCodeOnlyCuracaoEntity status(
            final int statusCode) {
        return new StatusCodeOnlyCuracaoEntity(statusCode);
    }

    /**
     * Convenience method that returns an empty response entity with a preset HTTP 200 OK
     * status code provided by {@link HttpServletResponse#SC_OK}.
     *
     * This method is intended to be used when a {@link HttpServletResponse#SC_OK} response
     * is desired with an empty response body.
     */
    protected static final StatusCodeOnlyCuracaoEntity ok() {
        return status(HttpServletResponse.SC_OK);
    }

    /**
     * Convenience method that returns an empty response entity with a preset HTTP 201 Created
     * status code provided by {@link HttpServletResponse#SC_CREATED}.
     */
    protected static final StatusCodeOnlyCuracaoEntity created() {
        return status(HttpServletResponse.SC_CREATED);
    }

    /**
     * Convenience method that returns a response entity with a preset HTTP 204 No Content
     * status code provided by {@link HttpServletResponse#SC_NO_CONTENT}.
     */
    protected static final StatusCodeOnlyCuracaoEntity noContent() {
        return status(HttpServletResponse.SC_NO_CONTENT);
    }

    /**
     * Convenience method that returns a response entity with a preset HTTP 401 Unauthorized
     * status code provided by {@link HttpServletResponse#SC_UNAUTHORIZED}.
     */
    protected static final StatusCodeOnlyCuracaoEntity unauthorized() {
        return status(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Convenience method that returns a response entity with a preset HTTP 403 Forbidden
     * status code provided by {@link HttpServletResponse#SC_FORBIDDEN}.
     */
    protected static final StatusCodeOnlyCuracaoEntity forbidden() {
        return status(HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Convenience method that returns a response entity with a preset HTTP 400 Bad Request
     * status code provided by {@link HttpServletResponse#SC_BAD_REQUEST}.
     */
    protected static final StatusCodeOnlyCuracaoEntity badRequest() {
        return status(HttpServletResponse.SC_BAD_REQUEST);
    }

    /**
     * Convenience method that returns a response entity with a preset HTTP 400 Bad Request
     * status code provided by {@link HttpServletResponse#SC_INTERNAL_SERVER_ERROR}.
     */
    protected static final StatusCodeOnlyCuracaoEntity internalServerError() {
        return status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Convenience method that returns a response entity with a preset HTTP 503 Service Unavailable
     * status code provided by {@link HttpServletResponse#SC_SERVICE_UNAVAILABLE}.
     */
    protected static final StatusCodeOnlyCuracaoEntity unavailable() {
        return status(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

}
