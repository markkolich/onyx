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

package onyx.components.storage.cache;

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import io.netty.handler.codec.http.HttpHeaders;
import onyx.components.config.OnyxConfig;
import onyx.components.config.cache.LocalCacheConfig;
import onyx.components.storage.AssetManager;
import onyx.components.storage.CacheManager;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.entities.storage.cache.CachedResourceToken;
import org.apache.commons.codec.digest.DigestUtils;
import org.asynchttpclient.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.asynchttpclient.Dsl.asyncHttpClient;

@Component
public final class LocalCacheManager implements CacheManager {

    private static final Logger LOG = LoggerFactory.getLogger(LocalCacheManager.class);

    private final OnyxConfig onyxConfig_;
    private final LocalCacheConfig localCacheConfig_;

    private final AssetManager assetManager_;

    private final CachedResourceSigner cachedResourceSigner_;

    @Injectable
    public LocalCacheManager(
            final OnyxConfig onyxConfig,
            final LocalCacheConfig localCacheConfig,
            final AssetManager assetManager,
            final CachedResourceSigner cachedResourceSigner) {
        onyxConfig_ = onyxConfig;
        localCacheConfig_ = localCacheConfig;
        assetManager_ = assetManager;
        cachedResourceSigner_ = cachedResourceSigner;
    }

    @Nullable
    @Override
    public URL getCachedDownloadUrlForResource(
            final Resource resource) throws Exception {
        final Path cachedResource = getCachedResourceFileForPath(resource.getPath());
        if (cachedResource == null) {
            return null;
        }

        final long tokenValidityDurationInSeconds =
                localCacheConfig_.getLocalCacheTokenValidityDuration(TimeUnit.SECONDS);

        final CachedResourceToken cachedResourceToken = new CachedResourceToken.Builder()
                .setPath(resource.getPath())
                .setExpiry(new Date(Instant.now().plusSeconds(tokenValidityDurationInSeconds).toEpochMilli()))
                .build();

        final String urlSafeSignedToken = cachedResourceSigner_.signCachedResourceToken(cachedResourceToken);

        final String signedTokenUrl = String.format("%s/static/cache/%s/%s", onyxConfig_.getViewSafeFullUri(),
                urlSafeSignedToken, resource.getName());
        return new URL(signedTokenUrl);
    }

    @Nullable
    @Override
    public Path getCachedResourceFileForPath(
            final String resourcePath) {
        final Path cachedResource = generateCachedResourcePath(resourcePath);
        if (!Files.exists(cachedResource)) {
            LOG.debug("Found no cached resource for path: {}: {}", resourcePath, cachedResource);
            return null;
        }

        return cachedResource;
    }

    @Override
    public void downloadResourceToCache(
            final Resource resource) {
        final URL downloadUrl = assetManager_.getPresignedDownloadUrlForResource(resource);

        final DefaultAsyncHttpClientConfig clientConfig = new DefaultAsyncHttpClientConfig.Builder()
                // No read or download timeout; intentional.
                .setReadTimeout(-1)
                .setRequestTimeout(-1)
                .build();

        final Path cachedResource = generateCachedResourcePath(resource.getPath());

        try (AsyncHttpClient asyncHttpClient = asyncHttpClient(clientConfig)) {
            final ListenableFuture<Path> future = asyncHttpClient.prepareGet(downloadUrl.toString())
                    .execute(new StreamedFileDownloadAsyncHandler(cachedResource));
            future.toCompletableFuture()
                .exceptionally(t -> {
                    LOG.error("Failed to download resource cache file: {}", cachedResource, t);
                    return null;
                }).join();

            LOG.info("Successfully downloaded file to cache: {} -> {}", resource.getPath(),
                    cachedResource);
        } catch (final Exception e) {
            LOG.error("Failed to download resource to cache from pre-signed "
                    + "download URL: {}", downloadUrl, e);
        }
    }

    @Override
    public void downloadResourceToCacheAsync(
            final Resource resource,
            final ExecutorService executorService) {
        executorService.submit(() -> downloadResourceToCache(resource));
    }

    @Override
    public void deleteResourceFromCache(
            final Resource resource) {
        final Path cachedResource = generateCachedResourcePath(resource.getPath());

        try {
            // Delete the file in the cache if it exists.
            if (Files.exists(cachedResource)) {
                Files.delete(cachedResource);

                LOG.info("Successfully deleted file from cache: {} -> {}", resource.getPath(),
                        cachedResource);
            }
        } catch (final Exception e) {
            LOG.warn("Failed to delete cached resource: {}: {}", resource.getPath(),
                    cachedResource, e);
        }
    }

    @Override
    public void deleteResourceFromCacheAsync(
            final Resource resource,
            final ExecutorService executorService) {
        executorService.submit(() -> deleteResourceFromCache(resource));
    }

    /**
     * Builds & resolves a resource path into its hashed cache file equivalent.
     * This method does not check if the resulting {@link Path} exists.
     */
    private Path generateCachedResourcePath(
            final String resourcePath) {
        final Path cacheDirectory = localCacheConfig_.getLocalCacheDirectory();
        // SHA-256 hash the resource path; this should provide enough URL-safe
        // uniqueness that there should not be any per-path conflicts when
        // used in a URL or in the name of a file on disk.
        final String hashedResourceName = DigestUtils.sha256Hex(resourcePath);
        return cacheDirectory.resolve(hashedResourceName);
    }

    /**
     * An {@link AsyncHandler} implementation that streams a file download directly to disk,
     * buffering very little in memory.
     */
    private static final class StreamedFileDownloadAsyncHandler implements AsyncHandler<Path> {

        private static final Logger LOG = LoggerFactory.getLogger(StreamedFileDownloadAsyncHandler.class);

        private final Path filePath_;
        private final OutputStream os_;

        private boolean failed_ = false;

        StreamedFileDownloadAsyncHandler(
                final Path filePath) throws Exception {
            filePath_ = filePath;
            os_ = Files.newOutputStream(filePath);
        }

        @Override
        public State onStatusReceived(
                final HttpResponseStatus responseStatus) throws Exception {
            if (responseStatus.getStatusCode() != HttpServletResponse.SC_OK) {
                return State.ABORT;
            }

            return State.CONTINUE;
        }

        @Override
        public State onHeadersReceived(
                final HttpHeaders headers) throws Exception {
            return State.CONTINUE;
        }

        @Override
        public State onBodyPartReceived(
                final HttpResponseBodyPart bodyPart) throws Exception {
            if (failed_) {
                return State.ABORT;
            }

            os_.write(bodyPart.getBodyPartBytes());
            return State.CONTINUE;
        }

        @Override
        public void onThrowable(
                final Throwable t) {
            failed_ = true;
            LOG.error("Failed to write local cache file from async stream: {}", filePath_, t);
        }

        @Override
        public Path onCompleted() throws Exception {
            if (failed_) {
                os_.close();
                Files.delete(filePath_);
                return null;
            }

            os_.close();
            return filePath_;
        }

    }

}
