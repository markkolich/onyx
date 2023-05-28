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

package onyx.components.security;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A <code>SHA256withRSA</code> signer implementation that uses Java's built-in
 * {@link Signature} classes to digitally sign a sequence of bytes using a provided
 * public-private key pair. The signature and the signed message bytes are embedded
 * in the resulting byte array as part of the payload, so the consumer of this class
 * does not need to pass around the signature and message separately.
 */
public final class SHA256withRSASigner {

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private static final int HASH_LENGTH = 256;

    private PublicKey publicKey_;
    private PrivateKey privateKey_;

    private byte[] message_;

    public SHA256withRSASigner setPublicKey(
            final PublicKey publicKey) {
        publicKey_ = checkNotNull(publicKey, "Public key cannot be null.");
        return this;
    }

    public SHA256withRSASigner setPrivateKey(
            final PrivateKey privateKey) {
        privateKey_ = checkNotNull(privateKey, "Private key cannot be null.");
        return this;
    }

    public SHA256withRSASigner setMessage(
            final byte [] message) {
        message_ = checkNotNull(message, "Message cannot be null.");
        return this;
    }

    public byte[] sign() throws Exception {
        checkNotNull(privateKey_, "Private key cannot be null.");
        checkNotNull(message_, "Message cannot be null.");

        final Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey_);
        signature.update(message_);

        final byte[] signed = signature.sign();

        return ByteBuffer.allocate(signed.length + message_.length)
                .put(signed)
                .put(message_)
                .array();
    }

    public boolean verify() throws Exception {
        checkNotNull(publicKey_, "Public key cannot be null.");
        checkNotNull(message_, "Message cannot be null.");

        // The incoming message to verify has to be at least as long as the
        // signature hash itself - if it is not, then it cannot be valid.
        if (message_.length <= HASH_LENGTH) {
            return false;
        }

        final Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(publicKey_);
        signature.update(message_, HASH_LENGTH, message_.length - HASH_LENGTH);

        return signature.verify(message_, 0, HASH_LENGTH);
    }

    /**
     * Verifies if the signature on the payload is valid, and if so, extracts
     * and returns the signed message body. Otherwise, this method returns null.
     */
    @Nullable
    public byte[] extract() throws Exception {
        checkNotNull(publicKey_, "Public key cannot be null.");
        checkNotNull(message_, "Message cannot be null.");

        final boolean signatureVerified = verify();
        if (!signatureVerified) {
            return null;
        }

        return ByteBuffer.allocate(message_.length - HASH_LENGTH)
                .put(message_, HASH_LENGTH, message_.length - HASH_LENGTH)
                .array();
    }

}
