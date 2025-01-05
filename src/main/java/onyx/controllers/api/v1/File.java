/*
 * Copyright (c) 2024 Mark S. Kolich
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

import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.s3.model.Region;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.Path;
import curacao.annotations.parameters.Query;
import curacao.annotations.parameters.RequestBody;
import curacao.core.servlet.HttpResponse;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.aws.dynamodb.DynamoDbMapper;
import onyx.components.config.OnyxConfig;
import onyx.components.config.aws.AwsConfig;
import onyx.components.config.cache.LocalCacheConfig;
import onyx.components.storage.AssetManager;
import onyx.components.storage.CacheManager;
import onyx.components.storage.ResourceManager;
import onyx.controllers.api.AbstractOnyxApiController;
import onyx.entities.api.request.v1.UpdateFileRequest;
import onyx.entities.api.request.v1.UploadFileRequest;
import onyx.entities.api.response.v1.ResourceResponse;
import onyx.entities.api.response.v1.UploadFileResponse;
import onyx.entities.authentication.Session;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.api.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Instant;
import java.util.List;

import static curacao.annotations.RequestMapping.Method.DELETE;
import static curacao.annotations.RequestMapping.Method.GET;
import static curacao.annotations.RequestMapping.Method.POST;
import static curacao.annotations.RequestMapping.Method.PUT;
import static onyx.util.FileUtils.humanReadableByteCountBin;
import static onyx.util.PathUtils.normalizePath;
import static onyx.util.PathUtils.splitNormalizedPathToElements;
import static onyx.util.UserUtils.userIsNotOwner;

@Controller
public final class File extends AbstractOnyxApiController {

    private static final Logger LOG = LoggerFactory.getLogger(File.class);

    private final AwsConfig awsConfig_;
    private final LocalCacheConfig localCacheConfig_;

    private final AssetManager assetManager_;
    private final ResourceManager resourceManager_;
    private final CacheManager cacheManager_;

    private final IDynamoDBMapper dbMapper_;

    private final ObjectMapper objectMapper_;

    @Injectable
    public File(
            final OnyxConfig onyxConfig,
            final AwsConfig awsConfig,
            final LocalCacheConfig localCacheConfig,
            final AssetManager assetManager,
            final ResourceManager resourceManager,
            final CacheManager cacheManager,
            final DynamoDbMapper dynamoDbMapper,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper) {
        super(onyxConfig);
        awsConfig_ = awsConfig;
        localCacheConfig_ = localCacheConfig;
        assetManager_ = assetManager;
        resourceManager_ = resourceManager;
        cacheManager_ = cacheManager;
        dbMapper_ = dynamoDbMapper.getDbMapper();
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();
    }

    @RequestMapping(value = "^/api/v1/file/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = GET)
    public ResourceResponse getFile(
            @Path("username") final String username,
            @Path("path") final String path,
            final Session session) {
        final String normalizedPath = normalizePath(username, path);

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

        return ResourceResponse.Builder.fromResource(objectMapper_, file, session)
                .build();
    }

    @RequestMapping(value = "^/api/v1/file/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = POST)
    public UploadFileResponse uploadFile(
            @Path("username") final String username,
            @Path("path") final String path,
            @Query("recursive") final Boolean recursive,
            @Query("overwrite") final Boolean overwrite,
            @RequestBody final UploadFileRequest request,
            final HttpResponse response,
            final Session session) {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        } else if (!session.getUsername().equals(username)) {
            throw new ApiForbiddenException("User session does not match request.");
        }

        final String normalizedPath = normalizePath(username, path);
        final Resource file = resourceManager_.getResourceAtPath(normalizedPath);
        if (file != null && BooleanUtils.isTrue(overwrite)) {
            LOG.warn("Overwrite is true - skipping existing resource check: {}",
                    file.getPath());
            // Delete the asset before overwriting the resource.
            assetManager_.deleteResource(file, true);
        } else if (file != null) {
            throw new ApiConflictException("File or other resource at path already exists: "
                    + normalizedPath);
        }

        final long uploadRequestSize = request.getSize();
        final long maxUploadRequestSize = awsConfig_.getAwsS3MaxUploadFileSize();
        if (uploadRequestSize > maxUploadRequestSize) {
            LOG.warn("File upload size exceeds allowed maximum: {}-bytes > {}-bytes ({}): {}",
                    uploadRequestSize, maxUploadRequestSize,
                    humanReadableByteCountBin(maxUploadRequestSize),
                    normalizedPath);
            throw new ApiPreconditionFailedException(String.format("File upload size exceeds allowed maximum: "
                    + "%s-bytes (%s)", maxUploadRequestSize, humanReadableByteCountBin(maxUploadRequestSize)));
        }

        final String parentPath = normalizePath(username, FilenameUtils.getPathNoEndSeparator(path));

        // Recursively create the parent directories, only if asked.
        if (BooleanUtils.isTrue(recursive)) {
            final List<Triple<String, String, String>> elements =
                    splitNormalizedPathToElements(parentPath);

            for (final Triple<String, String, String> element : elements) {
                final String elementParentPath = element.getLeft();
                final String elementPath = element.getMiddle();

                final Resource elementParent = resourceManager_.getResourceAtPath(elementPath);
                if (elementParent == null) {
                    final Resource newDirectory = new Resource.Builder()
                            .setPath(elementPath)
                            .setParent(elementParentPath)
                            .setDescription("") // intentional
                            .setType(Resource.Type.DIRECTORY)
                            .setVisibility(request.getVisibility())
                            .setOwner(session.getUsername())
                            .setCreatedAt(Instant.now()) // now
                            .withS3BucketRegion(Region.fromValue(awsConfig_.getAwsS3Region().getName()))
                            .withS3Bucket(awsConfig_.getAwsS3BucketName())
                            .withDbMapper(dbMapper_)
                            .build();

                    resourceManager_.createResource(newDirectory);
                } else if (!Resource.Type.DIRECTORY.equals(elementParent.getType())) {
                    throw new ApiBadRequestException("Found no parent directory resource at path: "
                            + parentPath);
                }
            }
        }

        final Resource parent = resourceManager_.getResourceAtPath(parentPath);
        if (parent == null) {
            throw new ApiNotFoundException("No parent directory resource at path: "
                    + parentPath);
        } else if (!Resource.Type.DIRECTORY.equals(parent.getType())) {
            throw new ApiBadRequestException("Found no parent directory resource at path: "
                    + parentPath);
        } else if (userIsNotOwner(parent, session)) {
            throw new ApiForbiddenException("Authenticated user is not the owner of parent directory: "
                    + parentPath);
        }

        final Resource newFile = new Resource.Builder()
                .setPath(normalizedPath)
                .setParent(parent.getPath())
                .setSize(request.getSize())
                .setDescription(request.getDescription())
                .setType(Resource.Type.FILE)
                .setVisibility(request.getVisibility())
                .setOwner(session.getUsername())
                .setCreatedAt(Instant.now()) // now
                .withS3BucketRegion(Region.fromValue(awsConfig_.getAwsS3Region().getName()))
                .withS3Bucket(awsConfig_.getAwsS3BucketName())
                .withDbMapper(dbMapper_)
                .build();

        resourceManager_.createResource(newFile);

        final URL presignedUploadUrl = assetManager_.getPresignedUploadUrlForResource(newFile);
        final String presignedUploadUrlString = presignedUploadUrl.toString();

        // The Location response header only provides a meaning when served with a
        // 3xx (redirection) or 201 (created) status response. In our case, the Location
        // header represents the location of the newly created resource - the presigned
        // URL of the asset as it exists on S3.
        response.addHeader(HttpHeaders.LOCATION, presignedUploadUrlString);

        return new UploadFileResponse.Builder(objectMapper_)
                .setPresignedUploadUrl(presignedUploadUrlString)
                .build();
    }

    @RequestMapping(value = "^/api/v1/file/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = PUT)
    public ResourceResponse updateFile(
            @Path("username") final String username,
            @Path("path") final String path,
            @RequestBody final UpdateFileRequest request,
            final Session session) {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        } else if (!session.getUsername().equals(username)) {
            throw new ApiForbiddenException("User session does not match request.");
        }

        final String normalizedPath = normalizePath(username, path);
        final Resource file = resourceManager_.getResourceAtPath(normalizedPath);
        if (file == null) {
            throw new ApiNotFoundException("Found no file resource at path: "
                    + normalizedPath);
        } else if (!Resource.Type.FILE.equals(file.getType())) {
            throw new ApiBadRequestException("Found no file resource at path: "
                    + normalizedPath);
        } else if (userIsNotOwner(file, session)) {
            throw new ApiForbiddenException("Authenticated user is not the owner of file resource: "
                    + normalizedPath);
        }

        final String description = request.getDescription();
        if (description != null) {
            file.setDescription(StringUtils.trimToEmpty(description));
        }

        final Resource.Visibility visibility = request.getVisibility();
        if (visibility != null) {
            file.setVisibility(visibility);
        }

        final Boolean favorite = request.getFavorite();
        if (favorite != null) {
            file.setFavorite(favorite);

            // Trigger a download of the file to the cache only if the resource has private visibility.
            final boolean localCacheEnabled = localCacheConfig_.localCacheEnabled();
            if (localCacheEnabled && Resource.Visibility.PRIVATE.equals(file.getVisibility())) {
                if (BooleanUtils.isTrue(favorite)) {
                    // When a file is favorited, trigger a download of the resource to the cache.
                    cacheManager_.downloadResourceToCacheAsync(file);
                } else {
                    // When a file is un-favorited, deleted it from the cache.
                    cacheManager_.deleteResourceFromCacheAsync(file);
                }
            }
        }

        resourceManager_.updateResource(file);

        return ResourceResponse.Builder.fromResource(objectMapper_, file, session)
                .build();
    }

    @RequestMapping(value = "^/api/v1/file/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = DELETE)
    public ResourceResponse deleteFile(
            @Path("username") final String username,
            @Path("path") final String path,
            @Query("permanent") final Boolean permanent,
            final Session session) {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        } else if (!session.getUsername().equals(username)) {
            throw new ApiForbiddenException("User session does not match request.");
        }

        final String normalizedPath = normalizePath(username, path);
        final Resource file = resourceManager_.getResourceAtPath(normalizedPath);
        if (file == null) {
            throw new ApiNotFoundException("Found no file resource at path: "
                    + normalizedPath);
        } else if (!Resource.Type.FILE.equals(file.getType())) {
            throw new ApiBadRequestException("Found no file resource at path: "
                    + normalizedPath);
        } else if (userIsNotOwner(file, session)) {
            throw new ApiForbiddenException("Authenticated user is not the owner of file resource: "
                    + normalizedPath);
        }

        // For safety, users cannot delete any file or resource that is marked as a "favorite".
        // This ensures that someone does not accidentally delete an important (favorite) file.
        if (file.getFavorite()) {
            throw new ApiBadRequestException("Favorite files/resources cannot be deleted: "
                    + normalizedPath);
        }

        // Delete the file.
        resourceManager_.deleteResource(file);

        // Delete the asset asynchronously.
        final boolean deletePermanently = BooleanUtils.toBooleanDefaultIfNull(permanent, false);
        assetManager_.deleteResourceAsync(file, deletePermanently);

        // Delete the asset from the cache asynchronously too.
        final boolean localCacheEnabled = localCacheConfig_.localCacheEnabled();
        if (localCacheEnabled) {
            cacheManager_.deleteResourceFromCacheAsync(file);
        }

        return ResourceResponse.Builder.fromResource(objectMapper_, file, session)
                .build();
    }

}
