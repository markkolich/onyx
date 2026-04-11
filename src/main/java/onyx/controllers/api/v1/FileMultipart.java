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
import curacao.annotations.parameters.Query;
import curacao.annotations.parameters.RequestBody;
import curacao.entities.empty.StatusCodeOnlyCuracaoEntity;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.config.OnyxConfig;
import onyx.components.config.aws.AwsConfig;
import onyx.components.storage.AssetManager;
import onyx.components.storage.CacheManager;
import onyx.components.storage.ResourceManager;
import onyx.components.storage.sizer.cost.CostAnalyzer;
import onyx.entities.api.request.v1.CompleteMultipartUploadRequest;
import onyx.entities.api.request.v1.UploadFileRequest;
import onyx.entities.api.response.v1.InitiateMultipartUploadResponse;
import onyx.entities.api.response.v1.ResourceResponse;
import onyx.entities.authentication.Session;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.api.ApiBadRequestException;
import onyx.exceptions.api.ApiForbiddenException;
import onyx.exceptions.api.ApiNotFoundException;
import onyx.exceptions.api.ApiPreconditionFailedException;
import onyx.exceptions.api.ApiUnauthorizedException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static curacao.annotations.RequestMapping.Method.DELETE;
import static curacao.annotations.RequestMapping.Method.POST;
import static curacao.annotations.RequestMapping.Method.PUT;
import static onyx.util.FileUtils.humanReadableByteCountBin;
import static onyx.util.PathUtils.normalizePath;
import static onyx.util.UserUtils.userIsNotOwner;

@Controller
public final class FileMultipart extends AbstractOnyxFileApiController {

    private static final Logger LOG = LoggerFactory.getLogger(FileMultipart.class);

    private static final int MULTIPART_MAX_PART_COUNT = 10_000;
    private static final long MULTIPART_MIN_PART_SIZE = 5L * 1024L * 1024L; // 5 MiB

    private final AwsConfig awsConfig_;

    private final ObjectMapper objectMapper_;

    @Injectable
    public FileMultipart(
            final OnyxConfig onyxConfig,
            final AwsConfig awsConfig,
            final AssetManager assetManager,
            final CacheManager cacheManager,
            final ResourceManager resourceManager,
            final CostAnalyzer costAnalyzer,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper) {
        super(onyxConfig, assetManager, cacheManager, resourceManager, costAnalyzer);
        awsConfig_ = awsConfig;
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();
    }

    @RequestMapping(value = "^/api/v1/file-multipart/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = POST)
    public InitiateMultipartUploadResponse initiateMultipartUpload(
            @Path("username") final String username,
            @Path("path") final String path,
            @Query("recursive") final Boolean recursive,
            @Query("overwrite") final Boolean overwrite,
            @RequestBody final UploadFileRequest request,
            final Session session) {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        } else if (!session.getUsername().equals(username)) {
            throw new ApiForbiddenException("User session does not match request.");
        }

        final long partSize = awsConfig_.getAwsS3MultipartUploadPartSize();
        if (partSize < MULTIPART_MIN_PART_SIZE) {
            throw new ApiPreconditionFailedException(String.format(
                    "Configured multipart-upload-part-size %d bytes is below the AWS minimum of %d bytes (%s).",
                    partSize, MULTIPART_MIN_PART_SIZE, humanReadableByteCountBin(MULTIPART_MIN_PART_SIZE)));
        }

        final long uploadRequestSize = request.getSize();
        final long maxUploadRequestSize = awsConfig_.getAwsS3MultipartUploadMaxFileSize();
        if (uploadRequestSize > maxUploadRequestSize) {
            LOG.warn("Multipart upload size exceeds allowed maximum: {}-bytes > {}-bytes ({}): {}/{}",
                    uploadRequestSize, maxUploadRequestSize,
                    humanReadableByteCountBin(maxUploadRequestSize),
                    username, path);
            throw new ApiPreconditionFailedException(String.format(
                    "File upload size exceeds allowed maximum: %s-bytes (%s)",
                    maxUploadRequestSize, humanReadableByteCountBin(maxUploadRequestSize)));
        }

        final int partCount = Math.max(1, (int) Math.ceil((double) uploadRequestSize / partSize));
        if (partCount > MULTIPART_MAX_PART_COUNT) {
            throw new ApiPreconditionFailedException(String.format(
                    "File size %s requires %d parts at the configured part size of %s, "
                            + "which exceeds the AWS maximum of %d parts.",
                    humanReadableByteCountBin(uploadRequestSize),
                    partCount,
                    humanReadableByteCountBin(partSize),
                    MULTIPART_MAX_PART_COUNT));
        }

        final String normalizedPath = normalizePath(username, path);
        checkAndHandleExistingFile(normalizedPath, overwrite);

        final String parentPath = normalizePath(username, FilenameUtils.getPathNoEndSeparator(path));

        if (BooleanUtils.isTrue(recursive)) {
            createParentDirectoriesIfNeeded(parentPath, request, session);
        }

        final Resource parent = validateAndGetParentDirectory(parentPath, session);
        final Resource newFile = buildNewFileResource(normalizedPath, parent, request, session);

        resourceManager_.createResource(newFile);

        final String uploadId = assetManager_.initiateMultipartUpload(newFile);

        // Build the per-part presigned URLs. For all parts except the last, the part size is the
        // configured partSize. The final part is whatever bytes remain after the preceding parts.
        final ImmutableList.Builder<InitiateMultipartUploadResponse.Part> partsBuilder =
                ImmutableList.builder();
        for (int i = 0; i < partCount; i++) {
            final int partNumber = i + 1;
            final long offset = (long) i * partSize;
            final long thisPartSize = Math.min(partSize, uploadRequestSize - offset);
            final URL presignedUrl =
                    assetManager_.getPresignedUploadUrlForPart(newFile, uploadId, partNumber, thisPartSize);

            partsBuilder.add(new InitiateMultipartUploadResponse.Part.Builder()
                    .setPartNumber(partNumber)
                    .setPresignedUrl(presignedUrl.toString())
                    .build());
        }
        final List<InitiateMultipartUploadResponse.Part> parts = partsBuilder.build();

        return new InitiateMultipartUploadResponse.Builder(objectMapper_)
                .setUploadId(uploadId)
                .setPartSize(partSize)
                .setParts(parts)
                .build();
    }

    @RequestMapping(value = "^/api/v1/file-multipart/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = PUT)
    public ResourceResponse completeMultipartUpload(
            @Path("username") final String username,
            @Path("path") final String path,
            @Query("uploadId") final String uploadId,
            @RequestBody final CompleteMultipartUploadRequest request,
            final Session session) {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        } else if (!session.getUsername().equals(username)) {
            throw new ApiForbiddenException("User session does not match request.");
        }

        if (StringUtils.isBlank(uploadId)) {
            throw new ApiBadRequestException("uploadId query parameter is required.");
        } else if (request == null) {
            throw new ApiBadRequestException("Request body is required.");
        }

        final String normalizedPath = normalizePath(username, path);
        final Resource file = resourceManager_.getResourceAtPath(normalizedPath);
        if (file == null) {
            throw new ApiNotFoundException("Found no file resource at path: " + normalizedPath);
        } else if (!Resource.Type.FILE.equals(file.getType())) {
            throw new ApiBadRequestException("Found no file resource at path: " + normalizedPath);
        } else if (userIsNotOwner(file, session)) {
            throw new ApiForbiddenException("Authenticated user is not the owner of file resource: "
                    + normalizedPath);
        }

        assetManager_.completeMultipartUpload(file, uploadId, request.getParts());

        return ResourceResponse.Builder.fromResource(objectMapper_, file, session)
                .build();
    }

    @RequestMapping(value = "^/api/v1/file-multipart/(?<username>[a-zA-Z0-9]+)/(?<path>[a-zA-Z0-9\\-._~%!$&'()*+,;=:@/]*)$",
            methods = DELETE)
    public StatusCodeOnlyCuracaoEntity abortMultipartUpload(
            @Path("username") final String username,
            @Path("path") final String path,
            @Query("uploadId") final String uploadId,
            final Session session) {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        } else if (!session.getUsername().equals(username)) {
            throw new ApiForbiddenException("User session does not match request.");
        }

        if (StringUtils.isBlank(uploadId)) {
            throw new ApiBadRequestException("uploadId query parameter is required.");
        }

        final String normalizedPath = normalizePath(username, path);
        final Resource file = resourceManager_.getResourceAtPath(normalizedPath);
        if (file == null) {
            throw new ApiNotFoundException("Found no file resource at path: " + normalizedPath);
        } else if (!Resource.Type.FILE.equals(file.getType())) {
            throw new ApiBadRequestException("Found no file resource at path: " + normalizedPath);
        } else if (userIsNotOwner(file, session)) {
            throw new ApiForbiddenException("Authenticated user is not the owner of file resource: "
                    + normalizedPath);
        }

        assetManager_.abortMultipartUpload(file, uploadId);
        resourceManager_.deleteResource(file);

        return noContent();
    }

}
