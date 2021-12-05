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

package onyx.mappers.response.exceptions;

import curacao.annotations.Injectable;
import curacao.annotations.Mapper;
import curacao.exceptions.routing.PathNotFoundException;
import onyx.components.FreeMarkerContentRenderer;
import onyx.entities.freemarker.FreeMarkerContent;
import onyx.mappers.response.AbstractFreeMarkerContentAwareResponseMapper;

import javax.annotation.Nonnull;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

@Mapper
public final class PathNotFoundExceptionResponseMapper
        extends AbstractFreeMarkerContentAwareResponseMapper<PathNotFoundException> {

    @Injectable
    public PathNotFoundExceptionResponseMapper(
            @Nonnull final FreeMarkerContentRenderer fmcRenderer) {
        super(fmcRenderer);
    }

    @Override
    public void render(
            final AsyncContext context,
            final HttpServletResponse response,
            @Nonnull final PathNotFoundException entity) throws Exception {
        final FreeMarkerContent content = new FreeMarkerContent.Builder("templates/errors/404.ftl", SC_NOT_FOUND)
                .build();

        renderFreeMarkerContent(response, content);
    }

}
