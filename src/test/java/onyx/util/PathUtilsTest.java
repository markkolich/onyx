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

import com.google.common.collect.ImmutableList;
import onyx.AbstractOnyxTest;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

import static onyx.util.PathUtils.normalizePath;
import static onyx.util.PathUtils.splitNormalizedPathToElements;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class PathUtilsTest extends AbstractOnyxTest {

    public PathUtilsTest() throws Exception {
    }

    @Test
    public void normalizePathTest() {
        assertEquals("/foobar", normalizePath("foobar", ""));
        assertEquals("/foobar", normalizePath("foobar", "/"));

        assertEquals("/foobar/baz", normalizePath("foobar", "baz"));
        assertEquals("/foobar/baz", normalizePath("foobar", "baz/"));

        assertEquals("/foobar/baz/cat", normalizePath("foobar", "baz/cat"));
    }

    @Test
    public void splitNormalizedPathToElementsTest() {
        assertEquals(ImmutableList.of(), splitNormalizedPathToElements("/"));
        assertEquals(ImmutableList.of(), splitNormalizedPathToElements(""));

        assertEquals(ImmutableList.of(Triple.of("/", "/foo", "foo")),
                splitNormalizedPathToElements("/foo"));
        assertEquals(ImmutableList.of(
                Triple.of("/", "/foo", "foo"),
                Triple.of("/foo", "/foo/bar", "bar")),
                splitNormalizedPathToElements("/foo/bar"));
    }

}
