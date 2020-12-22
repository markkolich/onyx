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

package onyx.components.config;

import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;

public interface OnyxConfig {

    String CONTEXT_PATH_PROP = "context-path";
    String BASE_URI_PROP = "base-uri";
    String FULL_URI_PROP = "full-uri";
    String DEV_MODE_PROP = "dev-mode";

    Config getOnyxConfig();

    String getContextPath();

    /**
     * Returns the "view" safe context path.
     *
     * Special method for views to account for when the visible context path
     * is "/" (the root context). With this method views & templates should not
     * worry about concatenating two slashes together resulting in a malformed
     * resource path such as <code>//static/img.png</code> when the app is deployed
     * under the root context. If the visible context path is just a single slash
     * <code>/</code> this methods returns <code>""</code> (empty string) to
     * avoid this double slash concatenation problem, so the return value from this
     * method can be used safely in templates that need the context path to fetch resources:
     *
     * <code>&lt;img src="${contextPath}/some/image.png"&gt;</code>
     */
    default String getViewSafeContentPath() {
        if ("/".equals(getContextPath())) {
            return "";
        }

        return getContextPath();
    }

    String getBaseUri();

    String getFullUri();

    /**
     * Returns the "view" safe full URI.
     *
     * Similar to {@link #getViewSafeContentPath()} in that this method returns
     * the full application URI as visible, just without the trailing "/".
     */
    default String getViewSafeFullUri() {
        final String fullUri = getFullUri();
        if (fullUri.endsWith("/")) {
            return StringUtils.chop(fullUri);
        }

        return fullUri;
    }

    boolean isDevMode();

}
