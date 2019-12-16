/*
 * Copyright (c) 2020 Mark S. Kolich
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

import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.regex.Pattern;

public final class HtmlUtils {

    /**
     * A pre-compiled set of {@link Pattern}'s used to "compress" strings of HTML
     * by removing whitespace, new lines, and other useless whitespace formatting
     * characters.  Pre-compiling the patterns ideally performs better than running
     * through a bunch of chained {@link String#replaceAll(String, String)} calls.
     */
    private static final Set<Pattern> HTML_COMPRESSOR_PATTERNS = ImmutableSet.of(
            Pattern.compile("^[\\x20\\t]+"),
            Pattern.compile("[\\x20\\t]+$"),
            Pattern.compile("\\s*(\\r\\n|\\n)+\\s*"),
            Pattern.compile("^[\\r\\n]+"));

    // Cannot instantiate
    private HtmlUtils() {
    }

    /**
     * Strips whitespace and other non-printable HTML characters between tags.
     * @param uncompressed the string to minify
     * @return the minified response
     */
    public static String minify(
            final String uncompressed) {
        String stripped = uncompressed;
        for (final Pattern p : HTML_COMPRESSOR_PATTERNS) {
            stripped = p.matcher(stripped).replaceAll("");
        }

        return stripped;
    }

}
