/*
 * Copyright (c) 2021 Mark S. Kolich
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

package onyx.controllers;

import onyx.components.config.cache.LocalCacheConfig;
import onyx.components.storage.AssetManager;
import onyx.components.storage.AsynchronousResourcePool;
import onyx.components.storage.CacheManager;
import onyx.components.storage.ResourceManager;
import onyx.entities.authentication.Session;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class FileTest extends AbstractOnyxControllerTest {

    private final AsynchronousResourcePool asyncResourcePool_;

    public FileTest() throws Exception {
        final ExecutorService executorService = Mockito.mock(ExecutorService.class);
        asyncResourcePool_ = new AsynchronousResourcePool(executorService);
    }

    @Test
    public void redirectToFileDownloadTest() throws Exception {
        final LocalCacheConfig localCacheConfig = Mockito.mock(LocalCacheConfig.class);
        Mockito.when(localCacheConfig.localCacheEnabled()).thenReturn(true);

        final AssetManager assetManager = Mockito.mock(AssetManager.class);
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        final CacheManager cacheManager = Mockito.mock(CacheManager.class);

        final String privateFileName = "/foobar/secret-stuff/cool.txt";
        final Resource privateFile =
                resourceJsonToObject("mock/browse/foobar-private-file.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq(privateFileName)))
                .thenReturn(privateFile);

        final URL redirectUrl = new URL(UNIT_TEST_BASE_URI + UNIT_TEST_CONTEXT_PATH + privateFileName);
        Mockito.when(cacheManager.getCachedDownloadUrlForResource(ArgumentMatchers.eq(privateFile)))
                .thenReturn(redirectUrl);

        final HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
        final AsyncContext asyncContext = Mockito.mock(AsyncContext.class);

        final ArgumentCaptor<String> redirectLocation = ArgumentCaptor.forClass(String.class);
        Mockito.doNothing().when(httpServletResponse).sendRedirect(redirectLocation.capture());

        final File controller = new File(onyxConfig_, asyncResourcePool_, resourceManager,
                localCacheConfig, assetManager, cacheManager);

        final Session session = generateNewSession("foobar");
        controller.redirectToFileDownload("foobar", "secret-stuff/cool.txt",
                session, httpServletResponse, asyncContext);

        assertEquals(redirectUrl.toString(), redirectLocation.getValue());
        Mockito.verify(asyncContext).complete();
    }

    @Test
    public void redirectToFileDownloadTestNotInCachePrivateFile() throws Exception {
        final LocalCacheConfig localCacheConfig = Mockito.mock(LocalCacheConfig.class);
        Mockito.when(localCacheConfig.localCacheEnabled()).thenReturn(true);

        final AssetManager assetManager = Mockito.mock(AssetManager.class);
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        final CacheManager cacheManager = Mockito.mock(CacheManager.class);

        final String privateFileName = "/foobar/secret-stuff/cool.txt";
        final Resource privateFile =
                resourceJsonToObject("mock/browse/foobar-private-file.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq(privateFileName)))
                .thenReturn(privateFile);

        final URL redirectUrl = new URL(UNIT_TEST_BASE_URI + UNIT_TEST_CONTEXT_PATH + privateFileName);
        Mockito.when(assetManager.getPresignedDownloadUrlForResource(privateFile))
                .thenReturn(redirectUrl);

        final ArgumentCaptor<Resource> privateFileCaptor = ArgumentCaptor.forClass(Resource.class);

        Mockito.when(cacheManager.getCachedDownloadUrlForResource(ArgumentMatchers.eq(privateFile)))
                .thenReturn(null); // File is not in cache
        Mockito.doNothing().when(cacheManager)
                .downloadResourceToCacheAsync(privateFileCaptor.capture(), ArgumentMatchers.any());

        final HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
        final AsyncContext asyncContext = Mockito.mock(AsyncContext.class);

        final ArgumentCaptor<String> redirectLocation = ArgumentCaptor.forClass(String.class);
        Mockito.doNothing().when(httpServletResponse).sendRedirect(redirectLocation.capture());

        final File controller = new File(onyxConfig_, asyncResourcePool_, resourceManager,
                localCacheConfig, assetManager, cacheManager);

        final Session session = generateNewSession("foobar");
        controller.redirectToFileDownload("foobar", "secret-stuff/cool.txt",
                session, httpServletResponse, asyncContext);

        assertEquals(privateFile, privateFileCaptor.getValue());
        assertEquals(redirectUrl.toString(), redirectLocation.getValue());
        Mockito.verify(asyncContext).complete();
    }

    @Test
    public void redirectToFileDownloadTestLocalCacheNotEnabled() throws Exception {
        final LocalCacheConfig localCacheConfig = Mockito.mock(LocalCacheConfig.class);
        Mockito.when(localCacheConfig.localCacheEnabled()).thenReturn(false);

        final AssetManager assetManager = Mockito.mock(AssetManager.class);
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        final CacheManager cacheManager = Mockito.mock(CacheManager.class);

        final String privateFileName = "/foobar/secret-stuff/cool.txt";
        final Resource privateFile =
                resourceJsonToObject("mock/browse/foobar-private-file.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq(privateFileName)))
                .thenReturn(privateFile);

        final URL redirectUrl = new URL(UNIT_TEST_BASE_URI + UNIT_TEST_CONTEXT_PATH + privateFileName);
        Mockito.when(assetManager.getPresignedDownloadUrlForResource(privateFile))
                .thenReturn(redirectUrl);

        final HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
        final AsyncContext asyncContext = Mockito.mock(AsyncContext.class);

        final ArgumentCaptor<String> redirectLocation = ArgumentCaptor.forClass(String.class);
        Mockito.doNothing().when(httpServletResponse).sendRedirect(redirectLocation.capture());

        final File controller = new File(onyxConfig_, asyncResourcePool_, resourceManager,
                localCacheConfig, assetManager, cacheManager);

        final Session session = generateNewSession("foobar");
        controller.redirectToFileDownload("foobar", "secret-stuff/cool.txt",
                session, httpServletResponse, asyncContext);

        Mockito.verifyNoInteractions(cacheManager);
        assertEquals(redirectUrl.toString(), redirectLocation.getValue());
        Mockito.verify(asyncContext).complete();
    }

    @Test
    public void redirectToFileDownloadTestNotFavorite() throws Exception {
        final LocalCacheConfig localCacheConfig = Mockito.mock(LocalCacheConfig.class);
        Mockito.when(localCacheConfig.localCacheEnabled()).thenReturn(true);

        final AssetManager assetManager = Mockito.mock(AssetManager.class);
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        final CacheManager cacheManager = Mockito.mock(CacheManager.class);

        final String privateFileName = "/foobar/secret-stuff/awesome.txt";
        final Resource privateFileNotFavorite =
                resourceJsonToObject("mock/browse/foobar-private-file-not-favorite.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq(privateFileName)))
                .thenReturn(privateFileNotFavorite);

        final URL redirectUrl = new URL(UNIT_TEST_BASE_URI + UNIT_TEST_CONTEXT_PATH + privateFileName);
        Mockito.when(assetManager.getPresignedDownloadUrlForResource(privateFileNotFavorite))
                .thenReturn(redirectUrl);

        final HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
        final AsyncContext asyncContext = Mockito.mock(AsyncContext.class);

        final ArgumentCaptor<String> redirectLocation = ArgumentCaptor.forClass(String.class);
        Mockito.doNothing().when(httpServletResponse).sendRedirect(redirectLocation.capture());

        final File controller = new File(onyxConfig_, asyncResourcePool_, resourceManager,
                localCacheConfig, assetManager, cacheManager);

        final Session session = generateNewSession("foobar");
        controller.redirectToFileDownload("foobar", "secret-stuff/awesome.txt",
                session, httpServletResponse, asyncContext);

        // Non-favorite files are never downloaded to the local cache.
        Mockito.verifyNoInteractions(cacheManager);
        assertEquals(redirectUrl.toString(), redirectLocation.getValue());
        Mockito.verify(asyncContext).complete();
    }

    @Test
    public void redirectToFileDownloadTestPublicFile() throws Exception {
        final LocalCacheConfig localCacheConfig = Mockito.mock(LocalCacheConfig.class);
        Mockito.when(localCacheConfig.localCacheEnabled()).thenReturn(true);

        final AssetManager assetManager = Mockito.mock(AssetManager.class);
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        final CacheManager cacheManager = Mockito.mock(CacheManager.class);

        final String publicFileName = "/foobar/secret-stuff/kewl.txt";
        final Resource publicFile =
                resourceJsonToObject("mock/browse/foobar-public-file.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq(publicFileName)))
                .thenReturn(publicFile);

        final URL redirectUrl = new URL(UNIT_TEST_BASE_URI + UNIT_TEST_CONTEXT_PATH + publicFileName);
        Mockito.when(assetManager.getPresignedDownloadUrlForResource(publicFile))
                .thenReturn(redirectUrl);

        final HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
        final AsyncContext asyncContext = Mockito.mock(AsyncContext.class);

        final ArgumentCaptor<String> redirectLocation = ArgumentCaptor.forClass(String.class);
        Mockito.doNothing().when(httpServletResponse).sendRedirect(redirectLocation.capture());

        final File controller = new File(onyxConfig_, asyncResourcePool_, resourceManager,
                localCacheConfig, assetManager, cacheManager);

        final Session session = generateNewSession("foobar");
        controller.redirectToFileDownload("foobar", "secret-stuff/kewl.txt",
                session, httpServletResponse, asyncContext);

        // Public files are never downloaded to the local cache.
        Mockito.verify(cacheManager, Mockito.never())
                .downloadResourceToCacheAsync(ArgumentMatchers.any(), ArgumentMatchers.any());
        assertEquals(redirectUrl.toString(), redirectLocation.getValue());
        Mockito.verify(asyncContext).complete();
    }

}
