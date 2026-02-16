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

package onyx.util.security;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A signer implementation that uses Java's built-in {@link Signature} classes to digitally
 * sign a sequence of bytes using a provided public-private key pair. The signature and the
 * signed message bytes are embedded in the resulting byte array as part of the payload, so
 * the consumer of this class does not need to pass around the signature and message separately.
 */
public final class ByteSigner {

    public enum Algorithm {

        // The hash length is the RSA signature size in bytes, determined by the RSA key size
        // (e.g., 256 bytes for a 2048-bit key, 512 bytes for a 4096-bit key) - not the SHA
        // digest size.
        SHA256_WITH_RSA("SHA256withRSA", 256),
        SHA512_WITH_RSA("SHA512withRSA", 512);

        private final String algorithmName_;
        private final int hashLength_;

        Algorithm(
                final String algorithmName,
                final int hashLength) {
            algorithmName_ = algorithmName;
            hashLength_ = hashLength;
        }

        public String getAlgorithmName() {
            return algorithmName_;
        }

        public int getHashLength() {
            return hashLength_;
        }

    }

    private final Algorithm algorithm_;

    private final PublicKey publicKey_;
    private final PrivateKey privateKey_;

    private final byte[] message_;

    private ByteSigner(
            final Algorithm algorithm,
            @Nullable final PublicKey publicKey,
            @Nullable final PrivateKey privateKey,
            final byte[] message) {
        algorithm_ = checkNotNull(algorithm, "Algorithm cannot be null.");
        publicKey_ = publicKey;
        privateKey_ = privateKey;
        message_ = checkNotNull(message, "Message cannot be null.");
    }

    public byte[] sign() throws Exception {
        checkNotNull(privateKey_, "Private key cannot be null - cannot sign without a private key.");

        final Signature signature = Signature.getInstance(algorithm_.getAlgorithmName());
        signature.initSign(privateKey_);
        signature.update(message_);

        final byte[] signed = signature.sign();

        return ByteBuffer.allocate(signed.length + message_.length)
                .put(signed)
                .put(message_)
                .array();
    }

    public boolean verify() throws Exception {
        checkNotNull(publicKey_, "Public key cannot be null - cannot verify without a public key.");

        final int hashLength = algorithm_.getHashLength();

        // The incoming message to verify has to be at least as long as the
        // signature hash itself - if it is not, then it cannot be valid.
        if (message_.length <= hashLength) {
            return false;
        }

        final Signature signature = Signature.getInstance(algorithm_.getAlgorithmName());
        signature.initVerify(publicKey_);
        signature.update(message_, hashLength, message_.length - hashLength);

        return signature.verify(message_, 0, hashLength);
    }

    /**
     * Verifies if the signature on the payload is valid, and if so, extracts
     * and returns the signed message body. Otherwise, this method returns null.
     */
    @Nullable
    public byte[] extract() throws Exception {
        checkNotNull(publicKey_, "Public key cannot be null - cannot extract without a public key.");

        final int hashLength = algorithm_.getHashLength();

        final boolean signatureVerified = verify();
        if (!signatureVerified) {
            return null;
        }

        return ByteBuffer.allocate(message_.length - hashLength)
                .put(message_, hashLength, message_.length - hashLength)
                .array();
    }

    public static final class Builder {

        private Algorithm algorithm_ = Algorithm.SHA512_WITH_RSA; // default

        private PublicKey publicKey_;
        private PrivateKey privateKey_;

        private byte[] message_;

        public Builder setAlgorithm(
                final Algorithm algorithm) {
            algorithm_ = checkNotNull(algorithm, "Algorithm cannot be null.");
            return this;
        }

        public Builder setPublicKey(
                final PublicKey publicKey) {
            publicKey_ = publicKey;
            return this;
        }

        public Builder setPrivateKey(
                final PrivateKey privateKey) {
            privateKey_ = privateKey;
            return this;
        }

        public Builder setMessage(
                final byte[] message) {
            message_ = checkNotNull(message, "Message cannot be null.");
            return this;
        }

        public ByteSigner build() {
            checkNotNull(algorithm_, "Algorithm cannot be null.");
            checkNotNull(message_, "Message cannot be null.");

            return new ByteSigner(algorithm_, publicKey_, privateKey_, message_);
        }

    }

}
