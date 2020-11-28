/*
 * Copyright (c) 2020 Mark S. Kolich
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
import onyx.components.config.cache.LocalCacheConfig;
import onyx.components.storage.AssetManager;
import onyx.components.storage.AsynchronousResourcePool;
import onyx.components.storage.CacheManager;
import onyx.components.storage.ResourceManager;
import onyx.controllers.api.AbstractOnyxApiController;
import onyx.entities.api.request.UpdateFileRequest;
import onyx.entities.api.request.UploadFileRequest;
import onyx.entities.api.response.UploadFileResponse;
import onyx.entities.authentication.Session;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.api.ApiBadRequestException;
import onyx.exceptions.api.ApiConflictException;
import onyx.exceptions.api.ApiForbiddenException;
import onyx.exceptions.api.ApiNotFoundException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Triple;

import java.net.URL;
import java.util.Date;
import java.util.List;

import static curacao.annotations.RequestMapping.Method.*;
import static onyx.util.PathUtils.normalizePath;
import static onyx.util.PathUtils.splitNormalizedPathToElements;

@Controller
public final class File extends AbstractOnyxApiController {

    private final AwsConfig awsConfig_;
    private final LocalCacheConfig localCacheConfig_;

    private final AssetManager assetManager_;
    private final ResourceManager resourceManager_;
    private final CacheManager cacheManager_;

    private final IDynamoDBMapper dbMapper_;

    @Injectable
    public File(
            final OnyxConfig onyxConfig,
            final AsynchronousResourcePool asynchronousResourcePool,
            final AwsConfig awsConfig,
            final LocalCacheConfig localCacheConfig,
            final AssetManager assetManager,
            final ResourceManager resourceManager,
            final CacheManager cacheManager,
            final DynamoDbMapper dynamoDbMapper) {
        super(onyxConfig, asynchronousResourcePool);
        awsConfig_ = awsConfig;
        localCacheConfig_ = localCacheConfig;
        assetManager_ = assetManager;
        resourceManager_ = resourceManager;
        cacheManager_ = cacheManager;
        dbMapper_ = dynamoDbMapper.getDbMapper();
    }

    @RequestMapping(value = "^/api/v1/file/(?<username>[a-zA-Z0-9]*)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = POST)
    public UploadFileResponse uploadFile(
            @Path("username") final String username,
            @Path("path") final String path,
            @Query("recursive") final Boolean recursive,
            @RequestBody final UploadFileRequest request,
            final Session session) {
        if (session == null) {
            throw new ApiForbiddenException("User not authenticated.");
        }

        final String normalizedPath = normalizePath(username, path);
        final Resource file = resourceManager_.getResourceAtPath(normalizedPath);
        if (file != null) {
            throw new ApiConflictException("File or other resource at path already exists: "
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
                            .setCreatedAt(new Date()) // now
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

        final Resource newFile = new Resource.Builder()
                .setPath(normalizedPath)
                .setParent(parent.getPath())
                .setSize(request.getSize())
                .setDescription(request.getDescription())
                .setType(Resource.Type.FILE)
                .setVisibility(request.getVisibility())
                .setOwner(session.getUsername())
                .setCreatedAt(new Date()) // now
                .withS3BucketRegion(Region.fromValue(awsConfig_.getAwsS3Region().getName()))
                .withS3Bucket(awsConfig_.getAwsS3BucketName())
                .withDbMapper(dbMapper_)
                .build();

        resourceManager_.createResource(newFile);

        final URL presignedUploadUrl = assetManager_.getPresignedUploadUrlForResource(newFile);

        return new UploadFileResponse.Builder()
                .setPresignedUploadUrl(presignedUploadUrl.toString())
                .build();
    }

    @RequestMapping(value = "^/api/v1/file/(?<username>[a-zA-Z0-9]*)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = PUT)
    public CuracaoEntity updateFile(
            @Path("username") final String username,
            @Path("path") final String path,
            @RequestBody final UpdateFileRequest request,
            final Session session) {
        if (session == null) {
            throw new ApiForbiddenException("User not authenticated.");
        }

        final String normalizedPath = normalizePath(username, path);
        final Resource file = resourceManager_.getResourceAtPath(normalizedPath);
        if (file == null) {
            throw new ApiNotFoundException("Found no file resource at path: "
                    + normalizedPath);
        } else if (!Resource.Type.FILE.equals(file.getType())) {
            throw new ApiBadRequestException("Found no file resource at path: "
                    + normalizedPath);
        } else if (!file.getOwner().equals(session.getUsername())) {
            throw new ApiForbiddenException("Authenticated user is not the owner of file resource: "
                    + normalizedPath);
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
                    cacheManager_.downloadResourceToCacheAsync(file, executorService_);
                } else {
                    // When a file is un-favorited, deleted it from the cache.
                    cacheManager_.deleteResourceFromCacheAsync(file, executorService_);
                }
            }
        }

        resourceManager_.updateResource(file);

        return noContent();
    }

    @RequestMapping(value = "^/api/v1/file/(?<username>[a-zA-Z0-9]*)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = DELETE)
    public CuracaoEntity deleteFile(
            @Path("username") final String username,
            @Path("path") final String path,
            final Session session) {
        if (session == null) {
            throw new ApiForbiddenException("User not authenticated.");
        }

        final String normalizedPath = normalizePath(username, path);
        final Resource file = resourceManager_.getResourceAtPath(normalizedPath);
        if (file == null) {
            throw new ApiNotFoundException("Found no file resource at path: "
                    + normalizedPath);
        } else if (!Resource.Type.FILE.equals(file.getType())) {
            throw new ApiBadRequestException("Found no file resource at path: "
                    + normalizedPath);
        } else if (!file.getOwner().equals(session.getUsername())) {
            throw new ApiForbiddenException("Authenticated user is not the owner of file resource: "
                    + normalizedPath);
        }

        // Delete the file.
        resourceManager_.deleteResource(file);

        // Delete the asset asynchronously.
        assetManager_.deleteResourceAsync(file, executorService_);

        // Delete the asset from the cache asynchronously too.
        final boolean localCacheEnabled = localCacheConfig_.localCacheEnabled();
        if (localCacheEnabled) {
            cacheManager_.deleteResourceFromCacheAsync(file, executorService_);
        }

        return noContent();
    }

}
