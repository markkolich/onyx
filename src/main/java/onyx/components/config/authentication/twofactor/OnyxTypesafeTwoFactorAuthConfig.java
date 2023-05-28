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

package onyx.components.config.authentication.twofactor;

import com.typesafe.config.Config;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.OnyxConfig;

import java.util.concurrent.TimeUnit;

@Component
public final class OnyxTypesafeTwoFactorAuthConfig implements TwoFactorAuthConfig {

    private final Config config_;

    @Injectable
    public OnyxTypesafeTwoFactorAuthConfig(
            final OnyxConfig onyxConfig) {
        config_ = onyxConfig.getOnyxConfig().getConfig(TWO_FACTOR_AUTH_CONFIG_PATH);
    }

    @Override
    public boolean twoFactorAuthEnabled() {
        return config_.getBoolean(ENABLED_PROP);
    }

    @Override
    public int getRandomCodeLength() {
        return config_.getInt(RANDOM_CODE_LENGTH_PROP);
    }

    @Override
    public long getTokenDuration(
            final TimeUnit timeUnit) {
        return config_.getDuration(TOKEN_DURATION_PROP, timeUnit);
    }

    @Override
    public long getTrustedDeviceTokenDuration(
            final TimeUnit timeUnit) {
        return config_.getDuration(TRUSTED_DEVICE_TOKEN_DURATION_PROP, timeUnit);
    }

    @Override
    public long getTrustedDeviceTokenCookieMaxAge(
            final TimeUnit timeUnit) {
        return config_.getDuration(TRUSTED_DEVICE_TOKEN_COOKIE_MAX_AGE_PROP, timeUnit);
    }

}
