/*
 * Copyright (c) 2024 Mark S. Kolich
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

package onyx.components.storage.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.security.StringSigner;
import onyx.entities.storage.cache.CachedResourceToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Instant;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public final class LocalCachedResourceSigner implements CachedResourceSigner {

    private static final Logger LOG = LoggerFactory.getLogger(LocalCachedResourceSigner.class);

    private final StringSigner stringSigner_;

    private final ObjectMapper objectMapper_;

    @Injectable
    public LocalCachedResourceSigner(
            final StringSigner stringSigner,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper) {
        stringSigner_ = stringSigner;
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();
    }

    @Nullable
    @Override
    public String signCachedResourceToken(
            final CachedResourceToken cachedResourceToken) {
        checkNotNull(cachedResourceToken, "Cached resource token cannot be null.");

        try {
            final String serializedCachedResource = objectMapper_.writeValueAsString(cachedResourceToken);
            return stringSigner_.sign(serializedCachedResource);
        } catch (final Exception e) {
            LOG.warn("Failed to sign cached resource token: {}", cachedResourceToken.getPath(), e);
            return null;
        }
    }

    @Nullable
    @Override
    public CachedResourceToken extractSignedCachedResourceToken(
            final String signedToken) {
        checkNotNull(signedToken, "Signed cached resource token string cannot be null.");

        try {
            final String tokenString = stringSigner_.verifyAndGet(signedToken);
            final CachedResourceToken cachedResource = objectMapper_.readValue(tokenString,
                    CachedResourceToken.class);

            final Instant now = Instant.now();
            if (now.isAfter(cachedResource.getExpiry())) {
                LOG.debug("Cached resource token expired: {}", cachedResource.getPath());
                return null;
            }

            return cachedResource;
        } catch (final Exception e) {
            LOG.warn("Failed to get cached resource from signed token: {}", signedToken, e);
            return null;
        }
    }

}
