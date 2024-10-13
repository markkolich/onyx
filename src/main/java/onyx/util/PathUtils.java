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

package onyx.util;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;

import static com.amazonaws.util.SdkHttpUtils.urlDecode;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

public final class PathUtils {

    private static final Splitter SLASH_SPLITTER = Splitter.on("/").omitEmptyStrings().trimResults();

    // Cannot instantiate
    private PathUtils() {
    }

    /**
     * Given a username and a path, normalizes the path eliminating any unnecessary leading
     * or trailing slashes.
     */
    public static String normalizePath(
            final String username,
            final String path) {
        final String normalizedPath;
        if (path.endsWith("/") && !"/".equals(path)) {
            normalizedPath = String.format("/%s/%s", username, path.substring(0, path.length() - 1));
        } else if ("".equals(path) || "/".equals(path)) {
            normalizedPath = String.format("/%s", username);
        } else {
            normalizedPath = String.format("/%s/%s", username, path);
        }

        return normalizedPath;
    }

    /**
     * Given a normalized path such as "/x/y/z" this method returns a list of triples
     * representing the unique paths to each element in the path, including the parents. E.g.,
     * <code>[("/", "/x", "x"), ("/x", "/x/y", "y"), ("/x/y", "/x/y/z", "z")]</code>
     */
    public static List<Triple<String, String, String>> splitNormalizedPathToElements(
            final String normalizedPath) {
        final ImmutableList.Builder<Triple<String, String, String>> builder =
                ImmutableList.builder();

        final StringBuilder parentPathBuilder = new StringBuilder("/");
        final List<String> elements = SLASH_SPLITTER.splitToList(normalizedPath);
        for (int i = 0, l = elements.size(); i < l; i++) {
            final StringBuilder pathBuilder = new StringBuilder();
            for (int j = 0; j < i + 1; j++) {
                pathBuilder.append("/").append(elements.get(j));
                if (i < j) {
                    pathBuilder.append("/");
                }
            }

            final String parentPath = parentPathBuilder.toString();
            final String path = pathBuilder.toString();
            // IMPORTANT: note the URL decoding followed by HTML escaping to safely
            // decode and escape any non-HTML safe characters in a resource name.
            final String name = escapeHtml4(urlDecode(elements.get(i)));

            builder.add(Triple.of(parentPath, path, name));

            if (i > 0) {
                parentPathBuilder.append("/");
            }
            parentPathBuilder.append(elements.get(i));
        }

        return builder.build();
    }

}
