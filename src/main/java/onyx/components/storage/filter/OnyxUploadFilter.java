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

package onyx.components.storage.filter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.util.helpers.WildcardMatchHelper;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public final class OnyxUploadFilter implements UploadFilter {

    private final List<String> filtered_;

    @Injectable
    public OnyxUploadFilter(
            final FilterConfig filterConfig) {
        this(filterConfig.getUploadFilter());
    }

    @VisibleForTesting
    public OnyxUploadFilter(
            final List<String> filtered) {
        checkNotNull(filtered, "Upload filter list cannot be null.");
        filtered_ = filtered.stream()
                .map(String::toLowerCase)
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public boolean test(
            final String path) {
        if (path == null) {
            return false;
        }

        return WildcardMatchHelper.matchesAny(filtered_, path.toLowerCase());
    }

}
