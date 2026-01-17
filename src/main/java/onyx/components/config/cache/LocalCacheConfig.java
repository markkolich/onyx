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

package onyx.components.config.cache;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public interface LocalCacheConfig {

    String LOCAL_CACHE_CONFIG_PATH = "local-cache";

    String LOCAL_CACHE_ENABLED_PROP = "enabled";
    String LOCAL_CACHE_DIRECTORY_PROP = "directory";
    String LOCAL_CACHE_TOKEN_VALIDITY_DURATION_PROP = "token-validity-duration";

    String LOCAL_CACHE_DOWNLOADER_READ_TIMEOUT_DURATION_PROP = "cache-downloader-read-timeout-duration";
    String LOCAL_CACHE_DOWNLOADER_REQUEST_TIMEOUT_DURATION_PROP = "cache-downloader-request-timeout-duration";

    boolean localCacheEnabled();

    Path getLocalCacheDirectory();

    long getLocalCacheTokenValidityDuration(
            final TimeUnit timeUnit);

    long getLocalCacheDownloaderReadTimeout(
            final TimeUnit timeUnit);

    long getLocalCacheDownloaderRequestTimeout(
            final TimeUnit timeUnit);

}
