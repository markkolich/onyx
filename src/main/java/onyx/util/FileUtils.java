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

package onyx.util;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public final class FileUtils {

    // Cannot instantiate
    private FileUtils() {
    }

    public static String humanReadableByteCountSI(
            final long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " bytes";
        }

        final CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        long bytesLargerThan1Kb = bytes;
        while (bytesLargerThan1Kb <= -999_950 || bytesLargerThan1Kb >= 999_950) {
            bytesLargerThan1Kb /= 1000;
            ci.next();
        }

        // Nicety to drop any precision when the trailing decimal place is just a zero.
        // E.g., "1.0 KB" becomes just "1 KB"
        final String format;
        if ((bytesLargerThan1Kb / 1000.0) % 1 == 0) {
            format = "%.0f %cB";
        } else {
            format = "%.1f %cB";
        }

        return String.format(format, bytesLargerThan1Kb / 1000.0, ci.current());
    }

    public static String humanReadableByteCountBin(
            final long bytes) {
        final long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " bytes";
        }

        long value = absB;
        final CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);

        // Nicety to drop any precision when the trailing decimal place is just a zero.
        // E.g., "1.0 KB" becomes just "1 KB"
        final String format;
        if ((value / 1024.0) % 1 == 0) {
            format = "%.0f %cB";
        } else {
            format = "%.1f %cB";
        }

        return String.format(format, value / 1024.0, ci.current());
    }

}
