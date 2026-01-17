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

package onyx.components.shortlink.bitly;

import java.util.concurrent.TimeUnit;

public interface BitlyShortLinkGeneratorConfig {

    String BITLY_SHORTLINK_GENERATOR_CONFIG_PATH = "bitly";

    String BITLY_API_BASE_URL_PROP = "api-base-url";
    String BITLY_API_ACCESS_TOKEN_PROP = "api-access-token";
    String BITLY_API_CLIENT_TIMEOUT_PROP = "api-client-timeout";

    String BITLY_VISIBLE_BASE_APP_URL_PROP = "visible-base-app-url";

    String getApiBaseUrl();

    String getApiAccessToken();

    long getApiClientTimeout(
            final TimeUnit timeUnit);

    /**
     * The bit.ly API does not support shortening <code>localhost</code> URLs,
     * so when running Onyx in dev mode an optional configuration property can
     * be set here to explicitly override the "visible base app URL" as seen by
     * bit.ly.
     */
    String getVisibleBaseAppUrl();

}
