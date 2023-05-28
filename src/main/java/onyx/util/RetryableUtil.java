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

package onyx.util;

import onyx.exceptions.OnyxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class RetryableUtil {

    private static final Logger LOG = LoggerFactory.getLogger(RetryableUtil.class);

    // Cannot instantiate
    private RetryableUtil() {
    }

    public static <T> T callWithRetry(
            final int maxRetries,
            final Duration backoffThrottle,
            final Callable<T> retryable) {
        checkArgument(maxRetries > 0, "Max retries must be > 0.");
        checkNotNull(backoffThrottle, "Backoff throttle cannot be null.");
        checkNotNull(retryable, "Retry callable cannot be null.");

        for (int i = 1; i <= maxRetries; i++) {
            try {
                return retryable.call();
            } catch (final Exception e) {
                LOG.debug("Retry callable failed, attempt {}/{}", i, maxRetries, e);
                try {
                    Thread.sleep(backoffThrottle.toMillis());
                } catch (final Exception f) {
                    LOG.debug("Exception while sleeping for throttle duration.", f);
                }
            }
        }

        // Would only get here if the retryable failed the max number of times and the
        // operation could not be completed.
        throw new OnyxException(String.format("Failed to retry callable after %s total tries.",
                maxRetries));
    }

    public static void runWithRetry(
            final int maxRetries,
            final Duration backoffThrottle,
            final Runnable retryable) {
        callWithRetry(maxRetries, backoffThrottle, (Callable<Void>) () -> {
            retryable.run();
            return null; // Void
        });
    }

}
