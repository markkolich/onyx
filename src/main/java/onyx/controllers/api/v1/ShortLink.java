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
import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.Path;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.config.OnyxConfig;
import onyx.components.shortlink.ShortLinkGenerator;
import onyx.components.storage.ResourceManager;
import onyx.controllers.api.AbstractOnyxApiController;
import onyx.entities.api.response.v1.CreateShortLinkResponse;
import onyx.entities.authentication.Session;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.api.*;

import java.net.URL;

import static curacao.annotations.RequestMapping.Method.POST;
import static onyx.util.UserUtils.userIsNotOwner;
import static onyx.util.PathUtils.normalizePath;

@Controller
public final class ShortLink extends AbstractOnyxApiController {

    private final ResourceManager resourceManager_;

    private final ShortLinkGenerator shortLinkManager_;

    private final ObjectMapper objectMapper_;

    @Injectable
    public ShortLink(
            final OnyxConfig onyxConfig,
            final ResourceManager resourceManager,
            final ShortLinkGenerator shortLinkManager,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper) {
        super(onyxConfig);
        resourceManager_ = resourceManager;
        shortLinkManager_ = shortLinkManager;
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();
    }

    @RequestMapping(value = "^/api/v1/shortlink/(?<username>[a-zA-Z0-9]+)$",
            methods = POST)
    public CreateShortLinkResponse createShortLinkHomeDirectory(
            @Path("username") final String username,
            final Session session) {
        return createShortLink(username, ResourceManager.ROOT_PATH, session);
    }

    @RequestMapping(value = "^/api/v1/shortlink/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = POST)
    public CreateShortLinkResponse createShortLink(
            @Path("username") final String username,
            @Path("path") final String path,
            final Session session) {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        } else if (!session.getUsername().equals(username)) {
            throw new ApiForbiddenException("User session does not match request.");
        }

        final String normalizedPath = normalizePath(username, path);
        final Resource resource = resourceManager_.getResourceAtPath(normalizedPath);
        if (resource == null) {
            throw new ApiNotFoundException("Found no resource at path: "
                    + normalizedPath);
        } else if (userIsNotOwner(resource, session)) {
            throw new ApiForbiddenException("Authenticated user is not the owner of resource: "
                    + normalizedPath);
        } else if (Resource.Visibility.PRIVATE.equals(resource.getVisibility())) {
            throw new ApiBadRequestException("Short links cannot be generated for private resources: "
                    + normalizedPath);
        }

        final URL shortLinkUrl = shortLinkManager_.createShortLinkForResource(resource);
        if (shortLinkUrl == null) {
            throw new ApiServiceUnavailableException("Failed to generate short link for resource: "
                    + resource.getPath());
        }

        return new CreateShortLinkResponse.Builder(objectMapper_)
                .setShortLinkUrl(shortLinkUrl.toString())
                .build();
    }

}
