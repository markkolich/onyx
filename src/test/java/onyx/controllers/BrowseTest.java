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

package onyx.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import onyx.components.storage.ResourceManager;
import onyx.components.storage.filter.OnyxResourceFilter;
import onyx.components.storage.filter.ResourceFilter;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class BrowseTest extends AbstractOnyxControllerTest {

    public BrowseTest() throws Exception {
    }

    @Test
    @SuppressWarnings("unchecked")
    public void browseUserHomeDirectoryTest() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);

        final ResourceFilter resourceFilter = Mockito.mock(ResourceFilter.class);
        Mockito.when(resourceFilter.test(ArgumentMatchers.any())).thenReturn(true);

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

        final Browse controller = new Browse(onyxConfig_, resourceManager, resourceFilter);

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
    public void browseUserRootUnauthenticatedTest() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);

        final ResourceFilter resourceFilter = Mockito.mock(ResourceFilter.class);
        Mockito.when(resourceFilter.test(ArgumentMatchers.any())).thenReturn(true);

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

        final Browse controller = new Browse(onyxConfig_, resourceManager, resourceFilter);

        final FreeMarkerContent responseEntity = controller.browseUserHomeDirectory("foobar", null);
        assertNotNull(responseEntity);

        assertEquals(ImmutableSet.of(Resource.Visibility.PUBLIC),
                visibilityCaptor.getValue());
        assertNull(sortCapture.getValue());

        final String renderedHtml = fmcRenderer_.contentToString(responseEntity);
        assertTrue(StringUtils.isNotBlank(renderedHtml));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void browseUserRootUnauthenticatedWithFilteringTest() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);

        final List<String> excludes = ImmutableList.of("/foobar/cat", "/foobar/cat/*");
        final ResourceFilter resourceFilter = new OnyxResourceFilter(excludes);

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

        final Browse controller = new Browse(onyxConfig_, resourceManager, resourceFilter);

        final FreeMarkerContent responseEntity = controller.browseUserHomeDirectory("foobar", null);
        assertNotNull(responseEntity);

        assertEquals(ImmutableSet.of(Resource.Visibility.PUBLIC),
                visibilityCaptor.getValue());
        assertNull(sortCapture.getValue());

        final String renderedHtml = fmcRenderer_.contentToString(responseEntity);
        assertTrue(StringUtils.isNotBlank(renderedHtml));
        assertFalse(renderedHtml.contains("/foobar/cat"),
                "Directory listing should not contain filtered resource.");
    }

    @Test
    public void browseDirectoryPrivateDirectoryTest() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);

        final ResourceFilter resourceFilter = Mockito.mock(ResourceFilter.class);
        Mockito.when(resourceFilter.test(ArgumentMatchers.any())).thenReturn(true);

        final Resource privateDirectory =
                resourceJsonToObject("mock/browse/foobar-private-dir.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq("/foobar/secret-stuff")))
                .thenReturn(privateDirectory);

        final Browse controller = new Browse(onyxConfig_, resourceManager, resourceFilter);

        final Session session = generateNewSession("baz");
        assertThrows(ResourceForbiddenException.class,
                () -> controller.browseDirectory("foobar", "secret-stuff", session));
    }

}
