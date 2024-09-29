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
import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.Path;
import curacao.annotations.parameters.Query;
import curacao.annotations.parameters.RequestBody;
import curacao.entities.CuracaoEntity;
import onyx.components.aws.dynamodb.DynamoDbMapper;
import onyx.components.config.OnyxConfig;
import onyx.components.config.aws.AwsConfig;
import onyx.components.storage.AssetManager;
import onyx.components.storage.ResourceManager;
import onyx.controllers.api.AbstractOnyxApiController;
import onyx.entities.api.request.v1.CreateDirectoryRequest;
import onyx.entities.api.request.v1.UpdateDirectoryRequest;
import onyx.entities.authentication.Session;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.api.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.List;

import static curacao.annotations.RequestMapping.Method.DELETE;
import static curacao.annotations.RequestMapping.Method.POST;
import static curacao.annotations.RequestMapping.Method.PUT;
import static onyx.util.PathUtils.normalizePath;
import static onyx.util.PathUtils.splitNormalizedPathToElements;

@Controller
public final class Directory extends AbstractOnyxApiController {

    private final AwsConfig awsConfig_;

    private final AssetManager assetManager_;

    private final ResourceManager resourceManager_;

    private final IDynamoDBMapper dbMapper_;

    @Injectable
    public Directory(
            final OnyxConfig onyxConfig,
            final AwsConfig awsConfig,
            final AssetManager assetManager,
            final ResourceManager resourceManager,
            final DynamoDbMapper dynamoDbMapper) {
        super(onyxConfig);
        awsConfig_ = awsConfig;
        assetManager_ = assetManager;
        resourceManager_ = resourceManager;
        dbMapper_ = dynamoDbMapper.getDbMapper();
    }

    @RequestMapping(value = "^/api/v1/directory/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = POST)
    public CuracaoEntity createDirectory(
            @Path("username") final String username,
            @Path("path") final String path,
            @Query("recursive") final Boolean recursive,
            @RequestBody final CreateDirectoryRequest request,
            final Session session) {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        } else if (!session.getUsername().equals(username)) {
            throw new ApiForbiddenException("User session does not match request.");
        }

        final String normalizedPath = normalizePath(username, path);
        final Resource directory = resourceManager_.getResourceAtPath(normalizedPath);
        if (directory != null) {
            throw new ApiConflictException("Directory or other resource at path already exists: "
                    + normalizedPath);
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
        } else if (!parent.getOwner().equals(session.getUsername())) {
            throw new ApiForbiddenException("Authenticated user is not the owner of parent directory: "
                    + parentPath);
        }

        final Resource newDirectory = new Resource.Builder()
                .setPath(normalizedPath)
                .setParent(parent.getPath())
                .setDescription(StringUtils.trimToEmpty(request.getDescription()))
                .setType(Resource.Type.DIRECTORY)
                .setVisibility(request.getVisibility())
                .setOwner(session.getUsername())
                .setCreatedAt(Instant.now()) // now
                .withS3BucketRegion(Region.fromValue(awsConfig_.getAwsS3Region().getName()))
                .withS3Bucket(awsConfig_.getAwsS3BucketName())
                .withDbMapper(dbMapper_)
                .build();

        resourceManager_.createResource(newDirectory);

        return created();
    }

    @RequestMapping(value = "^/api/v1/directory/(?<username>[a-zA-Z0-9]+)$",
            methods = PUT)
    public CuracaoEntity updateHomeDirectory(
            @Path("username") final String username,
            @RequestBody final UpdateDirectoryRequest request,
            final Session session) {
        return updateDirectory(username, ResourceManager.ROOT_PATH, request, session);
    }

    @RequestMapping(value = "^/api/v1/directory/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = PUT)
    public CuracaoEntity updateDirectory(
            @Path("username") final String username,
            @Path("path") final String path,
            @RequestBody final UpdateDirectoryRequest request,
            final Session session) {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        } else if (!session.getUsername().equals(username)) {
            throw new ApiForbiddenException("User session does not match request.");
        }

        final String normalizedPath = normalizePath(username, path);
        final Resource directory = resourceManager_.getResourceAtPath(normalizedPath);
        if (directory == null) {
            throw new ApiNotFoundException("Found no directory resource at path: "
                    + normalizedPath);
        } else if (!Resource.Type.DIRECTORY.equals(directory.getType())) {
            throw new ApiBadRequestException("Found no directory resource at path: "
                    + normalizedPath);
        } else if (!directory.getOwner().equals(session.getUsername())) {
            throw new ApiForbiddenException("Authenticated user is not the owner of directory resource: "
                    + normalizedPath);
        }

        final String description = request.getDescription();
        if (description != null) {
            directory.setDescription(StringUtils.trimToEmpty(description));
        }

        final Resource.Visibility visibility = request.getVisibility();
        if (visibility != null) {
            directory.setVisibility(visibility);
        }

        final Boolean favorite = request.getFavorite();
        if (favorite != null) {
            directory.setFavorite(favorite);
        }

        resourceManager_.updateResource(directory);

        return noContent();
    }

    @RequestMapping(value = "^/api/v1/directory/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = DELETE)
    public CuracaoEntity deleteDirectory(
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
        final Resource directory = resourceManager_.getResourceAtPath(normalizedPath);
        if (directory == null) {
            throw new ApiNotFoundException("Found no directory resource at path: "
                    + normalizedPath);
        } else if (!Resource.Type.DIRECTORY.equals(directory.getType())) {
            throw new ApiBadRequestException("Found no directory resource at path: "
                    + normalizedPath);
        } else if (!directory.getOwner().equals(session.getUsername())) {
            throw new ApiForbiddenException("Authenticated user is not the owner of directory resource: "
                    + normalizedPath);
        }

        // For safety, users cannot delete any directory or resource that is marked as a "favorite".
        // This ensures that someone does not accidentally delete an important (favorite) directory.
        if (directory.getFavorite()) {
            throw new ApiBadRequestException("Favorite directories/resources cannot be deleted: "
                    + normalizedPath);
        }

        // Recursively delete the directory and all of its children.
        resourceManager_.deleteResource(directory);

        // Recursively delete all assets under the directory, asynchronously.
        final boolean deletePermanently = BooleanUtils.toBooleanDefaultIfNull(permanent, false);
        assetManager_.deleteResourceAsync(directory, deletePermanently);

        return noContent();
    }

}
