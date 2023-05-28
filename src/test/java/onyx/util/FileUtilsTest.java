/*
 * Copyright (c) 2023 Mark S. Kolich
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
import org.junit.jupiter.api.Test;

import static onyx.util.FileUtils.humanReadableByteCountBin;
import static onyx.util.FileUtils.humanReadableByteCountSI;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class FileUtilsTest extends AbstractOnyxTest {

    public FileUtilsTest() throws Exception {
    }

    @Test
    public void humanReadableByteCountSITest() {
        assertEquals("0 bytes", humanReadableByteCountSI(0L));
        assertEquals("27 bytes", humanReadableByteCountSI(27L));
        assertEquals("999 bytes", humanReadableByteCountSI(999L));

        assertEquals("1 KB", humanReadableByteCountSI(1000L));
        assertEquals("1.0 KB", humanReadableByteCountSI(1023L));
        assertEquals("1.0 KB", humanReadableByteCountSI(1024L));
        assertEquals("1.7 KB", humanReadableByteCountSI(1728L));
        assertEquals("110.6 KB", humanReadableByteCountSI(110592L));

        assertEquals("1 MB", humanReadableByteCountSI(1000000L));
        assertEquals("7.1 MB", humanReadableByteCountSI(7077888L));
        assertEquals("453.0 MB", humanReadableByteCountSI(452984832L));

        assertEquals("1 GB", humanReadableByteCountSI(1000000000L));
        assertEquals("2.1 GB", humanReadableByteCountSI(2101029248L));

        assertEquals("1.9 TB", humanReadableByteCountSI(1855425871872L));

        assertEquals("9.2 EB", humanReadableByteCountSI(Long.MAX_VALUE));
    }

    @Test
    public void humanReadableByteCountBinTest() {
        assertEquals("0 bytes", humanReadableByteCountBin(0L));
        assertEquals("27 bytes", humanReadableByteCountBin(27L));
        assertEquals("999 bytes", humanReadableByteCountBin(999L));
        assertEquals("1000 bytes", humanReadableByteCountBin(1000L));
        assertEquals("1023 bytes", humanReadableByteCountBin(1023L));

        assertEquals("1 KB", humanReadableByteCountBin(1024L));
        assertEquals("1.7 KB", humanReadableByteCountBin(1728L));
        assertEquals("108 KB", humanReadableByteCountBin(110592L));
        assertEquals("976.6 KB", humanReadableByteCountBin(1000000L));

        assertEquals("6.8 MB", humanReadableByteCountBin(7077888L));
        assertEquals("432 MB", humanReadableByteCountBin(452984832L));
        assertEquals("953.7 MB", humanReadableByteCountBin(1000000000L));

        assertEquals("2.0 GB", humanReadableByteCountBin(2101029248L));
        assertEquals("2 GB", humanReadableByteCountBin(2147483648L));

        assertEquals("1.7 TB", humanReadableByteCountBin(1855425871872L));

        assertEquals("8.0 EB", humanReadableByteCountBin(Long.MAX_VALUE));
    }

}
