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

package onyx.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableSet;
import onyx.components.storage.ResourceManager;
import onyx.entities.authentication.Session;
import onyx.entities.freemarker.FreeMarkerContent;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.resource.ResourceForbiddenException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public final class BrowseTest extends AbstractOnyxControllerTest {

    public BrowseTest() throws Exception {
    }

    @Test
    @SuppressWarnings("unchecked")
    public void browseUserHomeDirectoryTest() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);

        final Resource homeDirectory =
                resourceJsonToObject("mock/browse/foobar.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq("/foobar")))
                .thenReturn(homeDirectory);

        final ArgumentCaptor<Set<Resource.Visibility>> visibilityCaptor =
                ArgumentCaptor.forClass(Set.class);

        final List<Resource> directoryList =
                resourceJsonToObject("mock/browse/foobar-dir-list.json", new TypeReference<>() {});
        Mockito.when(resourceManager.listDirectory(ArgumentMatchers.eq(homeDirectory),
                visibilityCaptor.capture(), ArgumentMatchers.any())).thenReturn(directoryList);

        final Browse controller = new Browse(onyxConfig_, resourceManager);

        final Session session = generateNewSession("foobar");
        final FreeMarkerContent responseEntity = controller.browseUserHomeDirectory(session.getUsername(), session);
        assertNotNull(responseEntity);

        assertEquals(ImmutableSet.of(Resource.Visibility.PUBLIC, Resource.Visibility.PRIVATE),
                visibilityCaptor.getValue());

        final String renderedHtml = fmcRenderer_.contentToString(responseEntity);
        assertTrue(StringUtils.isNotBlank(renderedHtml));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void browseUserRootTestUnauthenticated() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);

        final Resource homeDirectory =
                resourceJsonToObject("mock/browse/foobar.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq("/foobar")))
                .thenReturn(homeDirectory);

        final ArgumentCaptor<Set<Resource.Visibility>> visibilityCaptor =
                ArgumentCaptor.forClass(Set.class);
        final ArgumentCaptor<ResourceManager.Extensions.Sort> sortCapture =
                ArgumentCaptor.forClass(ResourceManager.Extensions.Sort.class);

        final List<Resource> directoryList =
                resourceJsonToObject("mock/browse/foobar-dir-list.json", new TypeReference<>() {});
        Mockito.when(resourceManager.listDirectory(ArgumentMatchers.eq(homeDirectory),
                visibilityCaptor.capture(), sortCapture.capture())).thenReturn(directoryList);

        final Browse controller = new Browse(onyxConfig_, resourceManager);

        final FreeMarkerContent responseEntity = controller.browseUserHomeDirectory("foobar", null);
        assertNotNull(responseEntity);

        assertEquals(ImmutableSet.of(Resource.Visibility.PUBLIC),
                visibilityCaptor.getValue());
        assertNull(sortCapture.getValue());

        final String renderedHtml = fmcRenderer_.contentToString(responseEntity);
        assertTrue(StringUtils.isNotBlank(renderedHtml));
    }

    @Test
    public void browseDirectoryTestPrivateDirectory() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);

        final Resource privateDirectory =
                resourceJsonToObject("mock/browse/foobar-private-dir.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq("/foobar/secret-stuff")))
                .thenReturn(privateDirectory);

        final Browse controller = new Browse(onyxConfig_, resourceManager);

        final Session session = generateNewSession("baz");
        assertThrows(ResourceForbiddenException.class,
                () -> controller.browseDirectory("foobar", "secret-stuff", session));
    }

}
