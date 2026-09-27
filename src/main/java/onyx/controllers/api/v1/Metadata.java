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
import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.Path;
import curacao.annotations.parameters.Query;
import curacao.core.servlet.AsyncContext;
import curacao.core.servlet.HttpResponse;
import curacao.entities.empty.StatusCodeOnlyCuracaoEntity;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.config.OnyxConfig;
import onyx.components.storage.MetadataManager;
import onyx.components.storage.ResourceManager;
import onyx.controllers.api.AbstractOnyxApiController;
import onyx.entities.api.response.v1.ResourceMetadataListResponse;
import onyx.entities.api.response.v1.ResourceMetadataResponse;
import onyx.entities.api.response.v1.ResourceMetadataUploadResponse;
import onyx.entities.authentication.Session;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.entities.storage.metadata.ResourceMetadata;
import onyx.exceptions.api.ApiBadRequestException;
import onyx.exceptions.api.ApiForbiddenException;
import onyx.exceptions.api.ApiNotFoundException;
import onyx.exceptions.api.ApiUnauthorizedException;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.util.List;

import static curacao.annotations.RequestMapping.Method.DELETE;
import static curacao.annotations.RequestMapping.Method.GET;
import static curacao.annotations.RequestMapping.Method.PUT;
import static onyx.util.PathUtils.SLASH_STRING;
import static onyx.util.PathUtils.normalizePath;
import static onyx.util.UserUtils.userIsNotOwner;

@Controller
public final class Metadata extends AbstractOnyxApiController {

    private final ResourceManager resourceManager_;
    private final MetadataManager metadataManager_;

    private final ObjectMapper objectMapper_;

    @Injectable
    public Metadata(
            final OnyxConfig onyxConfig,
            final ResourceManager resourceManager,
            final MetadataManager metadataManager,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper) {
        super(onyxConfig);
        resourceManager_ = resourceManager;
        metadataManager_ = metadataManager;
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();
    }

    @RequestMapping(value = "^/api/v1/metadata/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = GET)
    public ResourceMetadataListResponse getMetadata(
            @Path("username") final String username,
            @Path("path") final String path,
            @Query("type") final String type,
            final Session session) {
        final String normalizedPath = normalizePath(username, path);
        final Resource file = resolveReadableFile(normalizedPath, session);

        // Lists metadata keys only, optionally scoped to a type/kind. No presigned URLs
        // here - see downloadMetadata for fetching one by key, generated fresh at the
        // point it's actually needed rather than up front for every listed item. An
        // empty result (e.g., not yet processed) is a normal, successful outcome, not a
        // 404 - the resource itself exists, there's just nothing under this type/prefix yet.
        final List<ResourceMetadata> metadata = metadataManager_.listMetadataForResource(file, type);

        final List<ResourceMetadataResponse> items = metadata.stream()
                .map(m -> ResourceMetadataResponse.Builder.fromResourceMetadata(objectMapper_, m).build())
                .collect(ImmutableList.toImmutableList());

        return new ResourceMetadataListResponse.Builder(objectMapper_)
                .setItems(items)
                .build();
    }

    @RequestMapping(value = "^/api/v1/metadata-download/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = GET)
    public void downloadMetadata(
            @Path("username") final String username,
            @Path("path") final String path,
            @Query("key") final String key,
            final Session session,
            final HttpResponse response,
            final AsyncContext context) throws Exception {
        if (StringUtils.isBlank(key)) {
            throw new ApiBadRequestException("Query parameter 'key' is required.");
        }

        final String normalizedPath = normalizePath(username, path);
        final Resource file = resolveReadableFile(normalizedPath, session);

        final URL presignedDownloadUrl = metadataManager_.getMetadataDownloadUrlForResource(file, key);
        if (presignedDownloadUrl == null) {
            throw new ApiNotFoundException("No metadata found for resource: "
                    + normalizedPath + " (key=" + key + ")");
        }

        response.sendRedirect(presignedDownloadUrl.toString());
        context.complete();
    }

    @RequestMapping(value = "^/api/v1/metadata/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = PUT)
    public ResourceMetadataUploadResponse putMetadata(
            @Path("username") final String username,
            @Path("path") final String path,
            @Query("key") final String key,
            final Session session) {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        } else if (!session.getUsername().equals(username)) {
            throw new ApiForbiddenException("User session does not match request.");
        } else if (StringUtils.isBlank(key)) {
            throw new ApiBadRequestException("Query parameter 'key' is required.");
        }

        final String normalizedPath = normalizePath(username, path);

        final Resource file = resourceManager_.getResourceAtPath(normalizedPath);
        if (file == null) {
            throw new ApiNotFoundException("Found no file resource at path: "
                    + normalizedPath);
        } else if (!Resource.Type.FILE.equals(file.getType())) {
            throw new ApiBadRequestException("Found no file resource at path: "
                    + normalizedPath);
        }

        final URL presignedUploadUrl = metadataManager_.getMetadataUploadUrlForResource(file, key);

        // Unlike the list route's "type" - a prefix the caller uses to discover what exists -
        // "key" here is the exact relative key being written, since a writer already knows the
        // name of what it's producing. Normalize away any stray leading/trailing slashes so the
        // echoed key matches the canonical form GET would return. The manager normalizes
        // independently when building the absolute S3 key, so this is about the response body
        // only, not about where the bytes land.
        final String normalizedKey = StringUtils.strip(key, SLASH_STRING);

        return new ResourceMetadataUploadResponse.Builder(objectMapper_)
                .setKey(normalizedKey)
                .setPresignedUploadUrl(presignedUploadUrl.toString())
                .build();
    }

    @RequestMapping(value = "^/api/v1/metadata/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = DELETE)
    public StatusCodeOnlyCuracaoEntity deleteMetadata(
            @Path("username") final String username,
            @Path("path") final String path,
            @Query("key") final String key,
            final Session session) {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        } else if (!session.getUsername().equals(username)) {
            throw new ApiForbiddenException("User session does not match request.");
        } else if (StringUtils.isBlank(key)) {
            throw new ApiBadRequestException("Query parameter 'key' is required.");
        }

        final String normalizedPath = normalizePath(username, path);

        final Resource file = resourceManager_.getResourceAtPath(normalizedPath);
        if (file == null) {
            throw new ApiNotFoundException("Found no file resource at path: "
                    + normalizedPath);
        } else if (!Resource.Type.FILE.equals(file.getType())) {
            throw new ApiBadRequestException("Found no file resource at path: "
                    + normalizedPath);
        }

        metadataManager_.deleteMetadataForResource(file, key);

        return noContent();
    }

    /**
     * Resolves the file resource at the given path, enforcing the same read-visibility
     * rule used by {@code Download.java} for the primary asset: public resources are
     * open to anyone, private resources require the authenticated session to be the
     * owner (a missing session reports as 404, not 401/403, to avoid revealing that a
     * private resource exists at all).
     */
    private Resource resolveReadableFile(
            final String normalizedPath,
            final Session session) {
        final Resource file = resourceManager_.getResourceAtPath(normalizedPath);
        if (file == null) {
            throw new ApiNotFoundException("Found no file resource at path: "
                    + normalizedPath);
        }

        if (!Resource.Type.FILE.equals(file.getType())) {
            throw new ApiNotFoundException("Found no file resource at path: "
                    + normalizedPath);
        } else if (Resource.Visibility.PRIVATE.equals(file.getVisibility())) {
            // If the file is a private file, we have to ensure that the authenticated user is the owner.
            if (session == null) {
                throw new ApiNotFoundException("Found no file resource at path: "
                        + normalizedPath);
            } else if (userIsNotOwner(file, session)) {
                throw new ApiForbiddenException("Private file not visible to authenticated user: "
                        + normalizedPath);
            }
        }

        return file;
    }

}
