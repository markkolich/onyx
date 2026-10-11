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
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class OnyxFreeMarkerContentTest {

    @Test
    public void htmlBuilderTest() {
        final FreeMarkerContent ok = OnyxFreeMarkerContent.newHtmlBuilder("a.ftl").build();
        final FreeMarkerContent notFound = OnyxFreeMarkerContent.newHtmlBuilder("a.ftl", 404).build();

        assertCompressed(ok);
        assertCompressed(notFound);
        assertEquals(MediaType.HTML_UTF_8, ok.getMediaType());
        assertEquals(200, ok.getStatus());
        assertEquals(404, notFound.getStatus());
    }

    @Test
    public void xmlBuilderTest() {
        final FreeMarkerContent ok = OnyxFreeMarkerContent.newXmlBuilder("a.ftl").build();
        final FreeMarkerContent notFound = OnyxFreeMarkerContent.newXmlBuilder("a.ftl", 404).build();

        assertCompressed(ok);
        assertCompressed(notFound);
        assertEquals(MediaType.XML_UTF_8, ok.getMediaType());
        assertEquals(404, notFound.getStatus());
    }

    @Test
    public void plainBuilderHasNoTransformersTest() {
        final FreeMarkerContent robots =
                OnyxFreeMarkerContent.newBuilder("robots.ftl", MediaType.PLAIN_TEXT_UTF_8).build();

        assertTrue(robots.getTransformers().isEmpty());
        assertEquals(MediaType.PLAIN_TEXT_UTF_8, robots.getMediaType());
        assertEquals(200, robots.getStatus());
    }

    @Test
    public void callersCanAddTransformersTest() {
        final FreeMarkerContent content = OnyxFreeMarkerContent.newHtmlBuilder("a.ftl")
                .withTransformers(body -> body + "!")
                .build();

        assertEquals(3, content.getTransformers().size());
    }

    private static void assertCompressed(
            final FreeMarkerContent content) {
        // Exactly one minifier and one compressor, in that order.
        final List<?> transformers = content.getTransformers();
        assertEquals(2, transformers.size());
        assertInstanceOf(HtmlMinifier.class, transformers.get(0));
        assertInstanceOf(HtmlCompressor.class, transformers.get(1));
    }

}
