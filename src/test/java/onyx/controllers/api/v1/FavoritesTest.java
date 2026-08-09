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

package onyx.controllers.api.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableSet;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.storage.ResourceManager;
import onyx.controllers.AbstractOnyxControllerTest;
import onyx.entities.api.response.v1.FavoritesResponse;
import onyx.entities.authentication.Session;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.api.ApiForbiddenException;
import onyx.exceptions.api.ApiNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class FavoritesTest extends AbstractOnyxControllerTest {

    public FavoritesTest() throws Exception {
    }

    @Test
    public void favoritesUserHomeDirectoryTest() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);

        final Resource homeDirectory =
                resourceJsonToObject("mock/browse/foobar.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq("/foobar")))
                .thenReturn(homeDirectory);

        final List<Resource> favorites =
                resourceJsonToObject("mock/browse/foobar-favorites-list.json", new TypeReference<>() {});
        Mockito.when(resourceManager.listFavorites(ArgumentMatchers.eq(homeDirectory),
                        ArgumentMatchers.eq(ImmutableSet.of(Resource.Type.FILE))))
                .thenReturn(favorites);

        final Favorites controller = new Favorites(onyxConfig_, resourceManager,
                new OnyxJacksonObjectMapper());

        final Session session = generateNewSession("foobar");
        final FavoritesResponse response =
                controller.favoritesInUserHomeDirectory(session.getUsername(), session);

        assertEquals("/foobar", response.getPath());
        assertEquals(2, response.getChildren().size());
    }

    @Test
    public void favoritesDirectoryEmptyResultTest() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);

        final Resource homeDirectory =
                resourceJsonToObject("mock/browse/foobar.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq("/foobar")))
                .thenReturn(homeDirectory);
        Mockito.when(resourceManager.listFavorites(ArgumentMatchers.eq(homeDirectory),
                        ArgumentMatchers.eq(ImmutableSet.of(Resource.Type.FILE))))
                .thenReturn(Collections.emptyList());

        final Favorites controller = new Favorites(onyxConfig_, resourceManager,
                new OnyxJacksonObjectMapper());

        final Session session = generateNewSession("foobar");
        final FavoritesResponse response =
                controller.favoritesInUserHomeDirectory(session.getUsername(), session);

        assertTrue(response.getChildren().isEmpty());
    }

    @Test
    public void favoritesDirectoryUnauthenticatedTest() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);

        final Resource homeDirectory =
                resourceJsonToObject("mock/browse/foobar.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq("/foobar")))
                .thenReturn(homeDirectory);

        final Favorites controller = new Favorites(onyxConfig_, resourceManager,
                new OnyxJacksonObjectMapper());

        assertThrows(ApiNotFoundException.class,
                () -> controller.favoritesInUserHomeDirectory("foobar", null));
    }

    @Test
    public void favoritesDirectoryNotOwnerTest() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);

        final Resource homeDirectory =
                resourceJsonToObject("mock/browse/foobar.json", Resource.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.eq("/foobar")))
                .thenReturn(homeDirectory);

        final Favorites controller = new Favorites(onyxConfig_, resourceManager,
                new OnyxJacksonObjectMapper());

        final Session session = generateNewSession("baz");
        assertThrows(ApiForbiddenException.class,
                () -> controller.favoritesInUserHomeDirectory("foobar", session));
    }

    @Test
    public void favoritesDirectoryNotFoundTest() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        Mockito.when(resourceManager.getResourceAtPath(ArgumentMatchers.anyString()))
                .thenReturn(null);

        final Favorites controller = new Favorites(onyxConfig_, resourceManager,
                new OnyxJacksonObjectMapper());

        final Session session = generateNewSession("foobar");
        assertThrows(ApiNotFoundException.class,
                () -> controller.favoritesInUserHomeDirectory("foobar", session));
    }

}
