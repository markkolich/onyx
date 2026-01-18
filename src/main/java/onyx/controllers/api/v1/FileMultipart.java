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

import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.Region;
import com.fasterxml.jackson.databind.ObjectMapper;
import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.Path;
import curacao.annotations.parameters.Query;
import curacao.annotations.parameters.RequestBody;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.aws.dynamodb.DynamoDbMapper;
import onyx.components.config.OnyxConfig;
import onyx.components.config.aws.AwsConfig;
import onyx.components.storage.AssetManager;
import onyx.components.storage.ResourceManager;
import onyx.controllers.api.AbstractOnyxApiController;
import onyx.entities.api.request.v1.CompleteMultipartUploadRequest;
import onyx.entities.api.request.v1.InitiateMultipartUploadRequest;
import onyx.entities.api.response.v1.InitiateMultipartUploadResponse;
import onyx.entities.api.response.v1.MultipartUploadPartUrlResponse;
import onyx.entities.api.response.v1.ResourceResponse;
import onyx.entities.authentication.Session;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.api.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static curacao.annotations.RequestMapping.Method.DELETE;
import static curacao.annotations.RequestMapping.Method.GET;
import static curacao.annotations.RequestMapping.Method.POST;
import static onyx.util.PathUtils.normalizePath;
import static onyx.util.PathUtils.splitNormalizedPathToElements;
import static onyx.util.UserUtils.userIsNotOwner;

@Controller
public final class FileMultipart extends AbstractOnyxApiController {

    private static final Logger LOG = LoggerFactory.getLogger(FileMultipart.class);

    private final AwsConfig awsConfig_;

    private final AssetManager assetManager_;
    private final ResourceManager resourceManager_;

    private final IDynamoDBMapper dbMapper_;

    private final ObjectMapper objectMapper_;

    @Injectable
    public FileMultipart(
            final OnyxConfig onyxConfig,
            final AwsConfig awsConfig,
            final AssetManager assetManager,
            final ResourceManager resourceManager,
            final DynamoDbMapper dynamoDbMapper,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper) {
        super(onyxConfig);
        awsConfig_ = awsConfig;
        assetManager_ = assetManager;
        resourceManager_ = resourceManager;
        dbMapper_ = dynamoDbMapper.getDbMapper();
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();
    }

    @RequestMapping(value = "^/api/v1/file-multipart/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = POST)
    public InitiateMultipartUploadResponse initiateMultipartUpload(
            @Path("username") final String username,
            @Path("path") final String path,
            @Query("recursive") final Boolean recursive,
            @Query("overwrite") final Boolean overwrite,
            @RequestBody final InitiateMultipartUploadRequest request,
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
                            .withS3BucketRegion(Region.fromValue(awsConfig_.getAwsS3Region()))
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
                .withS3BucketRegion(Region.fromValue(awsConfig_.getAwsS3Region()))
                .withS3Bucket(awsConfig_.getAwsS3BucketName())
                .withDbMapper(dbMapper_)
                .build();

        resourceManager_.createResource(newFile);

        final String uploadId = assetManager_.initiateMultipartUpload(newFile);

        return new InitiateMultipartUploadResponse.Builder(objectMapper_)
                .setUploadId(uploadId)
                .build();
    }

    @RequestMapping(value = "^/api/v1/file-multipart/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)/(?<uploadId>[a-zA-Z0-9\\-._]+)/part/(?<partNumber>\\d+)$",
            methods = GET)
    public MultipartUploadPartUrlResponse getMultipartUploadPartUrl(
            @Path("username") final String username,
            @Path("path") final String path,
            @Path("uploadId") final String uploadId,
            @Path("partNumber") final Integer partNumber,
            final Session session) {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        } else if (!session.getUsername().equals(username)) {
            throw new ApiForbiddenException("User session does not match request.");
        }

        if (partNumber == null || partNumber < 1 || partNumber > 10000) {
            throw new ApiBadRequestException("Part number must be between 1 and 10000.");
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

        final URL presignedUploadUrl = assetManager_.getPresignedUploadUrlForPart(
                file, uploadId, partNumber);

        return new MultipartUploadPartUrlResponse.Builder(objectMapper_)
                .setPartNumber(partNumber)
                .setPresignedUploadUrl(presignedUploadUrl.toString())
                .build();
    }

    @RequestMapping(value = "^/api/v1/file-multipart/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)/(?<uploadId>[a-zA-Z0-9\\-._]+)/complete$",
            methods = POST)
    public ResourceResponse completeMultipartUpload(
            @Path("username") final String username,
            @Path("path") final String path,
            @Path("uploadId") final String uploadId,
            @RequestBody final CompleteMultipartUploadRequest request,
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

        final List<PartETag> partETags = request.getParts().stream()
                .map(part -> new PartETag(part.getPartNumber(), part.getETag()))
                .collect(Collectors.toList());

        assetManager_.completeMultipartUpload(file, uploadId, partETags);

        return ResourceResponse.Builder.fromResource(objectMapper_, file, session)
                .build();
    }

    @RequestMapping(value = "^/api/v1/file-multipart/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)/(?<uploadId>[a-zA-Z0-9\\-._]+)$",
            methods = DELETE)
    public ResourceResponse abortMultipartUpload(
            @Path("username") final String username,
            @Path("path") final String path,
            @Path("uploadId") final String uploadId,
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

        assetManager_.abortMultipartUpload(file, uploadId);

        // Also delete the resource from DynamoDB since the upload was aborted.
        resourceManager_.deleteResource(file);

        return ResourceResponse.Builder.fromResource(objectMapper_, file, session)
                .build();
    }

}
