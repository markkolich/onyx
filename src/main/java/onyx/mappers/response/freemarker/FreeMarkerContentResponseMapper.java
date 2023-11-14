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

package onyx.mappers.response.freemarker;

import curacao.annotations.Injectable;
import curacao.annotations.Mapper;
import curacao.core.servlet.AsyncContext;
import curacao.core.servlet.HttpResponse;
import onyx.components.FreeMarkerContentRenderer;
import onyx.entities.freemarker.FreeMarkerContent;
import onyx.mappers.response.AbstractFreeMarkerContentAwareResponseMapper;
import onyx.mappers.response.freemarker.helpers.FreeMarkerLinkHeaderDnsPrefetchHelper;
import onyx.mappers.response.freemarker.helpers.FreeMarkerSessionRefresherHelper;

import javax.annotation.Nonnull;

@Mapper
public final class FreeMarkerContentResponseMapper
        extends AbstractFreeMarkerContentAwareResponseMapper<FreeMarkerContent> {

    private final FreeMarkerSessionRefresherHelper sessionRefresherHelper_;
    private final FreeMarkerLinkHeaderDnsPrefetchHelper linkHeaderDnsPrefetchHelper_;

    @Injectable
    public FreeMarkerContentResponseMapper(
            @Nonnull final FreeMarkerContentRenderer fmcRenderer,
            @Nonnull final FreeMarkerSessionRefresherHelper sessionRefresherHelper,
            @Nonnull final FreeMarkerLinkHeaderDnsPrefetchHelper linkHeaderDnsPrefetchHelper) {
        super(fmcRenderer);
        sessionRefresherHelper_ = sessionRefresherHelper;
        this.linkHeaderDnsPrefetchHelper_ = linkHeaderDnsPrefetchHelper;
    }

    @Override
    public void render(
            final AsyncContext context,
            final HttpResponse response,
            @Nonnull final FreeMarkerContent content) throws Exception {
        sessionRefresherHelper_.refreshSession(response, content);
        linkHeaderDnsPrefetchHelper_.addLinkHeader(response);

        renderFreeMarkerContent(response, content);
    }

}
