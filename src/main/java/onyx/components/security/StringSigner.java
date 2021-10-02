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

package onyx.components.security;

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.exceptions.OnyxException;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Util component for digitally signing vanilla strings using the default
 * {@link SHA256withRSASigner} signing implementation.
 */
@Component
public final class StringSigner {

    private static final Logger LOG = LoggerFactory.getLogger(StringSigner.class);

    private final PublicKey publicKey_;
    private final PrivateKey privateKey_;

    @Injectable
    public StringSigner(
            final SecurityConfig securityConfig) throws Exception {
        publicKey_ = securityConfig.getSignerPublicKey();
        privateKey_ = securityConfig.getSignerPrivateKey();
    }

    @Nullable
    public String sign(
            @Nonnull final String toSign) {
        checkNotNull(toSign, "String to sign cannot be null.");

        try {
            final byte[] message = StringUtils.getBytesUtf8(toSign);
            final byte[] signed = new SHA256withRSASigner()
                    .setPrivateKey(privateKey_)
                    .setMessage(message)
                    .sign();

            return Base64.getUrlEncoder().encodeToString(signed);
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
            final byte[] decoded = Base64.getUrlDecoder().decode(signed);
            final byte[] verified = new SHA256withRSASigner()
                    .setPublicKey(publicKey_)
                    .setMessage(decoded)
                    .extract();

            if (verified == null) {
                throw new OnyxException("Signed string signature validation failed: " + signed);
            }

            return StringUtils.newStringUtf8(verified);
        } catch (final Exception e) {
            LOG.warn("Failed to verify & get signed string: {}", signed, e);
            return null;
        }
    }

}
