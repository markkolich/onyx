/*
 * Copyright (c) 2022 Mark S. Kolich
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

import curacao.core.servlet.HttpStatus;
import curacao.entities.empty.StatusCodeOnlyCuracaoEntity;
import onyx.components.config.OnyxConfig;

public abstract class AbstractOnyxController {

    protected final OnyxConfig onyxConfig_;

    protected AbstractOnyxController(
            final OnyxConfig onyxConfig) {
        onyxConfig_ = onyxConfig;
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
     * status code provided by {@link HttpStatus#SC_OK}.
     *
     * This method is intended to be used when a {@link HttpStatus#SC_OK} response
     * is desired with an empty response body.
     */
    protected static final StatusCodeOnlyCuracaoEntity ok() {
        return status(HttpStatus.SC_OK);
    }

    /**
     * Convenience method that returns an empty response entity with a preset HTTP 201 Created
     * status code provided by {@link HttpStatus#SC_CREATED}.
     */
    protected static final StatusCodeOnlyCuracaoEntity created() {
        return status(HttpStatus.SC_CREATED);
    }

    /**
     * Convenience method that returns a response entity with a preset HTTP 204 No Content
     * status code provided by {@link HttpStatus#SC_NO_CONTENT}.
     */
    protected static final StatusCodeOnlyCuracaoEntity noContent() {
        return status(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Convenience method that returns a response entity with a preset HTTP 401 Unauthorized
     * status code provided by {@link HttpStatus#SC_UNAUTHORIZED}.
     */
    protected static final StatusCodeOnlyCuracaoEntity unauthorized() {
        return status(HttpStatus.SC_UNAUTHORIZED);
    }

    /**
     * Convenience method that returns a response entity with a preset HTTP 403 Forbidden
     * status code provided by {@link HttpStatus#SC_FORBIDDEN}.
     */
    protected static final StatusCodeOnlyCuracaoEntity forbidden() {
        return status(HttpStatus.SC_FORBIDDEN);
    }

    /**
     * Convenience method that returns a response entity with a preset HTTP 400 Bad Request
     * status code provided by {@link HttpStatus#SC_BAD_REQUEST}.
     */
    protected static final StatusCodeOnlyCuracaoEntity badRequest() {
        return status(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Convenience method that returns a response entity with a preset HTTP 400 Bad Request
     * status code provided by {@link HttpStatus#SC_INTERNAL_SERVER_ERROR}.
     */
    protected static final StatusCodeOnlyCuracaoEntity internalServerError() {
        return status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Convenience method that returns a response entity with a preset HTTP 503 Service Unavailable
     * status code provided by {@link HttpStatus#SC_SERVICE_UNAVAILABLE}.
     */
    protected static final StatusCodeOnlyCuracaoEntity unavailable() {
        return status(HttpStatus.SC_SERVICE_UNAVAILABLE);
    }

}
