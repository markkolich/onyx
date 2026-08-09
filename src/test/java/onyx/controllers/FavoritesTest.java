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
import com.google.common.collect.ImmutableSet;
import onyx.components.storage.ResourceManager;
import onyx.components.storage.filter.OnyxResourceFilter;
import onyx.components.storage.filter.ResourceFilter;
import onyx.entities.authentication.Session;
import onyx.entities.freemarker.FreeMarkerContent;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.resource.ResourceForbiddenException;
import onyx.exceptions.resource.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class FavoritesTest extends AbstractOnyxControllerTest {

    public FavoritesTest() throws Exception {
    }

    @Test
    public void favoritesUserHomeDirectoryTest() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);

        final ResourceFilter resourceFilter = Mockito.mock(ResourceFilter.class);
        Mockito.when(resourceFilter.test(ArgumentMatchers.any())).thenReturn(true);

        final Resource homeDirectory =
                resourceJsonToObject("mock/browse/foobar.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq("/foobar")))
                .thenReturn(homeDirectory);

        final List<Resource> favorites =
                resourceJsonToObject("mock/browse/foobar-favorites-list.json", new TypeReference<>() {});
        Mockito.when(resourceManager.listFavorites(ArgumentMatchers.eq(homeDirectory),
                        ArgumentMatchers.eq(ImmutableSet.of(Resource.Type.FILE))))
                .thenReturn(favorites);

        final Favorites controller = new Favorites(onyxConfig_, resourceManager, resourceFilter);

        final Session session = generateNewSession("foobar");
        final FreeMarkerContent responseEntity =
                controller.favoritesInUserHomeDirectory(session.getUsername(), session);
        assertNotNull(responseEntity);

        final String renderedHtml = fmcRenderer_.contentToString(responseEntity);
        assertTrue(StringUtils.isNotBlank(renderedHtml));
        assertTrue(renderedHtml.contains("/foobar/dog"));
    }

    @Test
    public void favoritesUserHomeDirectoryEmptyResultTest() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);

        final ResourceFilter resourceFilter = Mockito.mock(ResourceFilter.class);
        Mockito.when(resourceFilter.test(ArgumentMatchers.any())).thenReturn(true);

        final Resource homeDirectory =
                resourceJsonToObject("mock/browse/foobar.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq("/foobar")))
                .thenReturn(homeDirectory);
        Mockito.when(resourceManager.listFavorites(ArgumentMatchers.eq(homeDirectory),
                        ArgumentMatchers.eq(ImmutableSet.of(Resource.Type.FILE))))
                .thenReturn(Collections.emptyList());

        final Favorites controller = new Favorites(onyxConfig_, resourceManager, resourceFilter);

        final Session session = generateNewSession("foobar");
        final FreeMarkerContent responseEntity =
                controller.favoritesInUserHomeDirectory(session.getUsername(), session);
        assertNotNull(responseEntity);

        final String renderedHtml = fmcRenderer_.contentToString(responseEntity);
        assertTrue(renderedHtml.contains("No favorites here."));
    }

    @Test
    public void favoritesUserHomeDirectoryWithFilteringTest() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);

        final List<String> excludes = List.of("/foobar/lion");
        final ResourceFilter resourceFilter = new OnyxResourceFilter(excludes);

        final Resource homeDirectory =
                resourceJsonToObject("mock/browse/foobar.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq("/foobar")))
                .thenReturn(homeDirectory);

        final List<Resource> favorites =
                resourceJsonToObject("mock/browse/foobar-favorites-list.json", new TypeReference<>() {});
        Mockito.when(resourceManager.listFavorites(ArgumentMatchers.eq(homeDirectory),
                        ArgumentMatchers.eq(ImmutableSet.of(Resource.Type.FILE))))
                .thenReturn(favorites);

        final Favorites controller = new Favorites(onyxConfig_, resourceManager, resourceFilter);

        final Session session = generateNewSession("foobar");
        final FreeMarkerContent responseEntity =
                controller.favoritesInUserHomeDirectory(session.getUsername(), session);

        final String renderedHtml = fmcRenderer_.contentToString(responseEntity);
        assertTrue(renderedHtml.contains("/foobar/dog"));
        assertFalse(renderedHtml.contains("/foobar/lion"),
                "Favorites listing should not contain filtered resource.");
    }

    @Test
    public void favoritesDirectoryUnauthenticatedTest() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);

        final ResourceFilter resourceFilter = Mockito.mock(ResourceFilter.class);

        final Resource homeDirectory =
                resourceJsonToObject("mock/browse/foobar.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq("/foobar")))
                .thenReturn(homeDirectory);

        final Favorites controller = new Favorites(onyxConfig_, resourceManager, resourceFilter);

        assertThrows(ResourceNotFoundException.class,
                () -> controller.favoritesInUserHomeDirectory("foobar", null));
    }

    @Test
    public void favoritesDirectoryNotOwnerTest() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);

        final ResourceFilter resourceFilter = Mockito.mock(ResourceFilter.class);

        final Resource homeDirectory =
                resourceJsonToObject("mock/browse/foobar.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq("/foobar")))
                .thenReturn(homeDirectory);

        final Favorites controller = new Favorites(onyxConfig_, resourceManager, resourceFilter);

        final Session session = generateNewSession("baz");
        assertThrows(ResourceForbiddenException.class,
                () -> controller.favoritesInUserHomeDirectory("foobar", session));
    }

}
