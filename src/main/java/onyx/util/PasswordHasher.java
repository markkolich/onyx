/*
 * Copyright (c) 2020 Mark S. Kolich
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

package onyx.util;

import org.mindrot.jbcrypt.BCrypt;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public final class PasswordHasher {

    private static final int DEFAULT_ITERATIONS = 12;

    private static final class LazyHolder {
        private static final PasswordHasher INSTANCE = new PasswordHasher();
    }

    public static PasswordHasher getInstance() {
        return LazyHolder.INSTANCE;
    }

    // Cannot instantiate
    private PasswordHasher() {
    }

    public String hash(
            @Nonnull final String password) {
        return hash(password, DEFAULT_ITERATIONS);
    }

    public String hash(
            @Nonnull final String password,
            @Nonnegative final int iterations) {
        checkNotNull(password, "Password cannot be null.");
        checkArgument(iterations >= 0, "Iterations must be positive.");
        return BCrypt.hashpw(password, BCrypt.gensalt(iterations));
    }

    public boolean verify(
            @Nonnull final String password,
            @Nonnull final String hash) {
        checkNotNull(password, "Password cannot be null.");
        checkArgument(!isNullOrEmpty(hash));
        return BCrypt.checkpw(password, hash);
    }

}
