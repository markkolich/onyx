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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.Path;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.config.OnyxConfig;
import onyx.components.storage.ResourceManager;
import onyx.controllers.api.AbstractOnyxApiController;
import onyx.entities.api.response.v1.FavoritesResponse;
import onyx.entities.api.response.v1.ResourceResponse;
import onyx.entities.authentication.Session;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.api.ApiForbiddenException;
import onyx.exceptions.api.ApiNotFoundException;

import java.util.List;
import java.util.Set;

import static curacao.annotations.RequestMapping.Method.GET;
import static onyx.util.PathUtils.normalizePath;
import static onyx.util.UserUtils.userIsNotOwner;

/**
 * Recursively browse a user's favorited resources under a given directory.
 * Unlike {@link Browse}, favorites are never conditionally public — only the
 * authenticated owner of a directory may ever list favorites under it.
 */
@Controller
public final class Favorites extends AbstractOnyxApiController {

    private final ResourceManager resourceManager_;

    private final ObjectMapper objectMapper_;

    @Injectable
    public Favorites(
            final OnyxConfig onyxConfig,
            final ResourceManager resourceManager,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper) {
        super(onyxConfig);
        resourceManager_ = resourceManager;
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();
    }

    @RequestMapping(value = "^/api/v1/favorites/(?<username>[a-zA-Z0-9]+)$",
            methods = GET)
    public FavoritesResponse favoritesInUserHomeDirectory(
            @Path("username") final String username,
            final Session session) {
        return favoritesInDirectory(username, ResourceManager.ROOT_PATH, session);
    }

    @RequestMapping(value = "^/api/v1/favorites/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = GET)
    public FavoritesResponse favoritesInDirectory(
            @Path("username") final String username,
            @Path("path") final String path,
            final Session session) {
        final String normalizedPath = normalizePath(username, path);

        final Resource directory = resourceManager_.getResourceAtPath(normalizedPath);
        if (directory == null) {
            throw new ApiNotFoundException("Found no directory resource at path: "
                    + normalizedPath);
        }

        if (!Resource.Type.DIRECTORY.equals(directory.getType())) {
            throw new ApiNotFoundException("Found no directory resource at path: "
                    + normalizedPath);
        } else if (session == null) {
            // Favorites are never conditionally public - unauthenticated requests
            // are treated the same as a resource that does not exist.
            throw new ApiNotFoundException("Found no directory resource at path: "
                    + normalizedPath);
        } else if (userIsNotOwner(directory, session)) {
            throw new ApiForbiddenException("Authenticated user is not the owner of directory resource: "
                    + normalizedPath);
        }

        final Set<Resource.Type> types = ImmutableSet.of(Resource.Type.FILE);
        final List<Resource> favorites = resourceManager_.listFavorites(directory, types);
        final List<ResourceResponse> children = favorites.stream()
                .map(c -> ResourceResponse.Builder.fromResource(objectMapper_, c, session).build())
                .collect(ImmutableList.toImmutableList());

        return FavoritesResponse.Builder.fromResource(objectMapper_, directory)
                .setChildren(children)
                .build();
    }

}
