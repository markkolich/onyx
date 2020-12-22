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

package onyx;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.io.Resources;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.config.OnyxConfig;
import org.eclipse.jetty.util.resource.Resource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public abstract class AbstractOnyxTest {

    private static final int MIN_UNIT_TEST_PORT = 20000;
    private static final int MAX_UNIT_TEST_PORT = 30000;

    protected static final int UNIT_TEST_RANDOM_PORT =
            ThreadLocalRandom.current().nextInt(MIN_UNIT_TEST_PORT, MAX_UNIT_TEST_PORT);
    protected static final String UNIT_TEST_BASE_URI = "http://localhost:" + UNIT_TEST_RANDOM_PORT;
    protected static final String UNIT_TEST_CONTEXT_PATH = "/unit-test";
    protected static final String UNIT_TEST_WEB_APP_ROOT_PATH = "src/main/webapp";

    protected final ServletContext servletContext_;

    protected final OnyxConfig onyxConfig_;

    protected final ObjectMapper objectMapper_;

    public AbstractOnyxTest() throws Exception {
        this(UNIT_TEST_CONTEXT_PATH);
    }

    public AbstractOnyxTest(
            @Nonnull final String testContextPath) throws Exception {
        this(testContextPath, UNIT_TEST_WEB_APP_ROOT_PATH);
    }

    public AbstractOnyxTest(
            @Nonnull final String testContextPath,
            @Nonnull final String webAppRootPath) throws Exception {
        checkNotNull(testContextPath, "Test context path cannot be null.");
        checkNotNull(webAppRootPath, "Web-application root path cannot be null.");

        checkState(testContextPath.startsWith("/"), "Test context path must start with a '/' slash.");

        servletContext_ = Mockito.mock(ServletContext.class);

        final Resource baseResource = Resource.newResource(webAppRootPath);
        if (!baseResource.exists()) {
            throw new IllegalArgumentException("Test base resource does not exist: " + webAppRootPath);
        }
        Mockito.when(servletContext_.getAttribute(ArgumentMatchers.eq(Application.CONTEXT_ATTRIBUTE_BASE_RESOURCE)))
                .thenReturn(baseResource);

        Mockito.when(servletContext_.getContextPath()).thenReturn(testContextPath);
        Mockito.when(servletContext_.getRealPath(Mockito.anyString())).thenAnswer(invocation -> {
            final String arg = invocation.getArgument(0);
            return webAppRootPath + arg;
        });

        onyxConfig_ = Mockito.mock(OnyxConfig.class);
        Mockito.when(onyxConfig_.getContextPath()).thenReturn(testContextPath);
        Mockito.when(onyxConfig_.getViewSafeContentPath()).thenCallRealMethod();
        Mockito.when(onyxConfig_.getBaseUri()).thenReturn(UNIT_TEST_BASE_URI);
        Mockito.when(onyxConfig_.getFullUri()).thenReturn(testContextPath);
        Mockito.when(onyxConfig_.getViewSafeFullUri()).thenCallRealMethod();
        Mockito.when(onyxConfig_.isDevMode()).thenReturn(true);

        objectMapper_ = new OnyxJacksonObjectMapper().getObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(onyx.entities.storage.aws.dynamodb.Resource.class, new ResourceDeserializer());
        objectMapper_.registerModule(module);
    }

    @Nonnull
    protected <T> T resourceJsonToObject(
            final String resource,
            final TypeReference<T> typeReference) throws Exception {
        final String resourceJson = resourceToString(resource);
        return objectMapper_.readValue(resourceJson, typeReference);
    }

    @Nonnull
    protected <T> T resourceJsonToObject(
            final String resource,
            final Class<T> clazz) throws Exception {
        final String resourceJson = resourceToString(resource);
        return objectMapper_.readValue(resourceJson, clazz);
    }

    @Nonnull
    protected static String resourceToString(
            final String resource) throws Exception {
        final URL url = Resources.getResource(resource);
        return Resources.toString(url, StandardCharsets.UTF_8);
    }

    @Nonnull
    protected static byte[] resourceToByteArray(
            final String resource) throws Exception {
        final URL url = Resources.getResource(resource);
        return Resources.toByteArray(url);
    }

}
