/*
 * Copyright (c) 2021 Mark S. Kolich
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

import curacao.entities.empty.StatusCodeOnlyCuracaoEntity;
import onyx.components.config.OnyxConfig;
import onyx.components.storage.AsynchronousResourcePool;

import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ExecutorService;

public abstract class AbstractOnyxController {

    protected final OnyxConfig onyxConfig_;

    protected final ExecutorService executorService_;

    protected AbstractOnyxController(
            final OnyxConfig onyxConfig,
            final AsynchronousResourcePool asynchronousResourcePool) {
        onyxConfig_ = onyxConfig;
        executorService_ = asynchronousResourcePool.getExecutorService();
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
