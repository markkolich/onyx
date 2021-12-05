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

package onyx.util;

import onyx.AbstractOnyxTest;
import onyx.util.ContentDisposition.DispositionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ContentDispositionTest extends AbstractOnyxTest {

    public ContentDispositionTest() throws Exception {
    }

    @Test
    public void testSimple() {
        final ContentDisposition header = ContentDisposition.Builder.csv()
                .setFileName("WITH. SPACES.-123456")
                .build();

        assertEquals("attachment; filename=\"WITH_SPACES_-123456.csv\"", header.toString());
    }

    @Test
    public void testTypeWithFileName() {
        final ContentDisposition header = ContentDisposition.Builder.pdf()
                .setFileName("foobar")
                .setFileExtension(".pdf") // not leading dot
                .build();

        assertEquals("inline; filename=\"foobar.pdf\"", header.toString());
    }

    @Test
    public void testTypeWithName() {
        final ContentDisposition header = new ContentDisposition.Builder()
                .setType(DispositionType.FORM_DATA)
                .setName("foobar")
                .build();

        assertEquals("form-data; name=\"foobar\"", header.toString());
    }

    @Test
    public void testPlainNoNameOrExtension() {
        final ContentDisposition header = new ContentDisposition.Builder()
                .setType(DispositionType.INLINE)
                .build();

        assertEquals("inline", header.toString());
    }

    @Test
    public void testMissingTypeShouldFail() {
        assertThrows(NullPointerException.class, () -> new ContentDisposition.Builder().build());
    }

}
