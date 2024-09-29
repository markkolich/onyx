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

package onyx.components.storage.filter;

import com.google.common.annotations.VisibleForTesting;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.util.helpers.WildcardMatchHelper;
import onyx.entities.storage.aws.dynamodb.Resource;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public final class OnyxResourceFilter implements ResourceFilter {

    private final List<String> excludes_;

    @Injectable
    public OnyxResourceFilter(
            final FilterConfig filterConfig) {
        this(filterConfig.getExcludes());
    }

    @VisibleForTesting
    public OnyxResourceFilter(
            final List<String> excludes) {
        excludes_ = checkNotNull(excludes, "Excludes list cannot be null.");
    }

    @Override
    public boolean test(
            final Resource resource) {
        if (resource == null) {
            return false;
        }

        return !WildcardMatchHelper.matchesAny(excludes_, resource.getPath());
    }

}
