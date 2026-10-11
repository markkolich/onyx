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

package onyx.components.freemarker;

import com.google.common.net.MediaType;
import curacao.freemarker.entities.FreeMarkerContent;
import curacao.freemarker.transformers.html.HtmlCompressor;
import curacao.freemarker.transformers.html.HtmlMinifier;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.core.servlet.HttpStatus.SC_OK;

public interface OnyxFreeMarkerContent {

    static FreeMarkerContent.Builder newHtmlBuilder(
            final String templateName) {
        return newHtmlBuilder(templateName, SC_OK);
    }

    static FreeMarkerContent.Builder newHtmlBuilder(
            final String templateName,
            final int status) {
        return newBuilder(templateName, MediaType.HTML_UTF_8, status)
                .withTransformers(new HtmlMinifier(), new HtmlCompressor());
    }

    static FreeMarkerContent.Builder newXmlBuilder(
            final String templateName) {
        return newXmlBuilder(templateName, SC_OK);
    }

    static FreeMarkerContent.Builder newXmlBuilder(
            final String templateName,
            final int status) {
        return newBuilder(templateName, MediaType.XML_UTF_8, status)
                .withTransformers(new HtmlMinifier(), new HtmlCompressor());
    }

    static FreeMarkerContent.Builder newBuilder(
            final String templateName,
            final MediaType mediaType) {
        return newBuilder(templateName, mediaType, SC_OK);
    }

    static FreeMarkerContent.Builder newBuilder(
            final String templateName,
            final MediaType mediaType,
            final int status) {
        checkNotNull(templateName, "Template name cannot be null.");
        checkNotNull(mediaType, "Media type cannot be null.");

        return new FreeMarkerContent.Builder(templateName, mediaType, status);
    }

}
