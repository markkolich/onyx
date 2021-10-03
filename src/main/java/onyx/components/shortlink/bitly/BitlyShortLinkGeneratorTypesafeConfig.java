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

package onyx.components.shortlink.bitly;

import com.typesafe.config.Config;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.OnyxConfig;

import java.util.concurrent.TimeUnit;

@Component
public final class BitlyShortLinkGeneratorTypesafeConfig implements BitlyShortLinkGeneratorConfig {

    private final OnyxConfig onyxConfig_;

    private final Config config_;

    @Injectable
    public BitlyShortLinkGeneratorTypesafeConfig(
            final OnyxConfig onyxConfig) {
        onyxConfig_ = onyxConfig;
        config_ = onyxConfig.getOnyxConfig().getConfig(BITLY_SHORTLINK_GENERATOR_CONFIG_PATH);
    }

    @Override
    public String getApiBaseUrl() {
        return config_.getString(BITLY_API_BASE_URL_PROP);
    }

    @Override
    public String getApiAccessToken() {
        return config_.getString(BITLY_API_ACCESS_TOKEN_PROP);
    }

    @Override
    public long getApiClientTimeout(
            final TimeUnit timeUnit) {
        return config_.getDuration(BITLY_API_CLIENT_TIMEOUT_PROP, timeUnit);
    }

    @Override
    public String getVisibleBaseAppUrl() {
        if (config_.hasPath(BITLY_VISIBLE_BASE_APP_URL_PROP)) {
            return config_.getString(BITLY_VISIBLE_BASE_APP_URL_PROP);
        } else {
            return onyxConfig_.getViewSafeFullUri();
        }
    }

}
