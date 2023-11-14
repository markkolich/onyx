/*
 * Copyright (c) 2024 Mark S. Kolich
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

package onyx.mappers.response;

import curacao.core.servlet.HttpResponse;
import curacao.mappers.response.AbstractControllerReturnTypeMapper;
import onyx.components.FreeMarkerContentRenderer;
import onyx.entities.freemarker.FreeMarkerContent;
import onyx.entities.freemarker.Utf8TextEntity;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractFreeMarkerContentAwareResponseMapper<T>
        extends AbstractControllerReturnTypeMapper<T> {

    protected final FreeMarkerContentRenderer fmcRenderer_;

    public AbstractFreeMarkerContentAwareResponseMapper(
            @Nonnull final FreeMarkerContentRenderer fmcRenderer) {
        fmcRenderer_ = checkNotNull(fmcRenderer, "FreeMarker content renderer cannot be null.");
    }

    protected final void renderFreeMarkerContent(
            final HttpResponse response,
            final FreeMarkerContent content) throws Exception {
        final String rendered = fmcRenderer_.contentToString(content);
        renderEntity(response, new Utf8TextEntity(content.getEntityType(), content.getStatus(), rendered));
    }

}
