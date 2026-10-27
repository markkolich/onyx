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

package onyx.components.security;

import com.kolich.security.sign.KeyDecoder;
import com.typesafe.config.Config;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.OnyxConfig;

import java.security.PrivateKey;
import java.security.PublicKey;

@Component
public final class OnyxSecurityConfig implements SecurityConfig {

    private final Config config_;

    @Injectable
    public OnyxSecurityConfig(
            final OnyxConfig onyxConfig) {
        config_ = onyxConfig.getOnyxConfig().getConfig(SECURITY_CONFIG_PATH);
    }

    @Override
    public String getSignerKeyAlgorithm() {
        return config_.getString(SIGNER_KEY_ALGORITHM_PROP);
    }

    @Override
    public PublicKey getSignerPublicKey() {
        return KeyDecoder.publicKey(config_.getString(SIGNER_PUBLIC_KEY_PROP),
                getSignerKeyAlgorithm());
    }

    @Override
    public PrivateKey getSignerPrivateKey() {
        return KeyDecoder.privateKey(config_.getString(SIGNER_PRIVATE_KEY_PROP),
                getSignerKeyAlgorithm());
    }

}
