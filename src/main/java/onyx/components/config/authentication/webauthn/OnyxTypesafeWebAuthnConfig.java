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

package onyx.components.config.authentication.webauthn;

import com.typesafe.config.Config;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.OnyxConfig;

import java.util.concurrent.TimeUnit;

@Component
public final class OnyxTypesafeWebAuthnConfig implements WebAuthnConfig {

    private final Config config_;

    @Injectable
    public OnyxTypesafeWebAuthnConfig(
            final OnyxConfig onyxConfig) {
        config_ = onyxConfig.getOnyxConfig().getConfig(WEBAUTHN_CONFIG_PATH);
    }

    @Override
    public boolean isWebAuthnEnabled() {
        return config_.getBoolean(ENABLED_PROP);
    }

    @Override
    public String getRpId() {
        return config_.getString(RP_ID_PROP);
    }

    @Override
    public String getRpName() {
        return config_.getString(RP_NAME_PROP);
    }

    @Override
    public String getRpOrigin() {
        return config_.getString(RP_ORIGIN_PROP);
    }

    @Override
    public long getChallengeTimeout(
            final TimeUnit timeUnit) {
        return config_.getDuration(CHALLENGE_TIMEOUT_PROP, timeUnit);
    }

}
