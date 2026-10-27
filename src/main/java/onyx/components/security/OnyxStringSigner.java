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

import com.kolich.security.sign.StringSigner;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Util component for digitally signing vanilla strings, wrapping the {@link StringSigner}
 * implementation in kolich-security.
 *
 * <p>The library signer throws on failure and deliberately does no logging, since it cannot
 * know whether the values flowing through it may be written to a log file. This component
 * makes that call for Onyx: a failure is logged and {@code null} is returned, which is what
 * the callers here already expect.
 */
@Component
public final class OnyxStringSigner {

    private static final Logger LOG = LoggerFactory.getLogger(OnyxStringSigner.class);

    private final StringSigner signer_;

    @Injectable
    public OnyxStringSigner(
            final SecurityConfig securityConfig) throws Exception {
        signer_ = new StringSigner.Builder()
                .setPublicKey(securityConfig.getSignerPublicKey())
                .setPrivateKey(securityConfig.getSignerPrivateKey())
                .build();
    }

    @Nullable
    public String sign(
            @Nonnull final String toSign) {
        checkNotNull(toSign, "String to sign cannot be null.");

        try {
            return signer_.sign(toSign);
        } catch (final Exception e) {
            LOG.warn("Failed to sign string: {}", toSign, e);
            return null;
        }
    }

    @Nullable
    public String verifyAndGet(
            @Nonnull final String signed) {
        checkNotNull(signed, "Signed string cannot be null.");

        try {
            final String verified = signer_.verifyAndGet(signed);
            if (verified == null) {
                LOG.warn("Signed string signature validation failed: {}", signed);
            }

            return verified;
        } catch (final Exception e) {
            LOG.warn("Failed to verify & get signed string: {}", signed, e);
            return null;
        }
    }

}
