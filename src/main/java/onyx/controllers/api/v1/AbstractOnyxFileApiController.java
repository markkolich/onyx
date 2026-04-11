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

import onyx.components.config.OnyxConfig;
import onyx.components.storage.AssetManager;
import onyx.components.storage.CacheManager;
import onyx.components.storage.ResourceManager;
import onyx.components.storage.sizer.cost.CostAnalyzer;
import onyx.controllers.api.AbstractOnyxApiController;
import onyx.entities.api.request.v1.UploadFileRequest;
import onyx.entities.authentication.Session;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.api.ApiBadRequestException;
import onyx.exceptions.api.ApiConflictException;
import onyx.exceptions.api.ApiForbiddenException;
import onyx.exceptions.api.ApiNotFoundException;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static onyx.util.PathUtils.splitNormalizedPathToElements;
import static onyx.util.UserUtils.userIsNotOwner;

public abstract class AbstractOnyxFileApiController extends AbstractOnyxApiController {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractOnyxFileApiController.class);

    protected final AssetManager assetManager_;
    protected final CacheManager cacheManager_;
    protected final ResourceManager resourceManager_;

    protected final CostAnalyzer costAnalyzer_;

    protected AbstractOnyxFileApiController(
            final OnyxConfig onyxConfig,
            final AssetManager assetManager,
            final CacheManager cacheManager,
            final ResourceManager resourceManager,
            final CostAnalyzer costAnalyzer) {
        super(onyxConfig);
        assetManager_ = assetManager;
        cacheManager_ = cacheManager;
        resourceManager_ = resourceManager;
        costAnalyzer_ = costAnalyzer;
    }

    /**
     * Checks for an existing resource at the normalized path. If one exists and overwrite is true,
     * deletes it (and its backing S3 object) to make way for the new upload. If one exists and
     * overwrite is false, throws a 409 Conflict.
     */
    protected void checkAndHandleExistingFile(
            final String normalizedPath,
            final Boolean overwrite) {
        final Resource file = resourceManager_.getResourceAtPath(normalizedPath);
        if (file != null && BooleanUtils.isTrue(overwrite)) {
            LOG.warn("Overwrite is true - skipping existing resource check: {}",
                    file.getPath());
            assetManager_.deleteResource(file, true);
        } else if (file != null) {
            throw new ApiConflictException("File or other resource at path already exists: "
                    + normalizedPath);
        }
    }

    /**
     * Recursively creates any missing parent directories along the given parent path. Directories
     * that already exist are left untouched. Throws a 400 if a path element exists but is not a
     * directory. Callers are responsible for only invoking this when recursive creation is desired.
     */
    protected void createParentDirectoriesIfNeeded(
            final String parentPath,
            final UploadFileRequest request,
            final Session session) {
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
                        .setDescription(StringUtils.EMPTY) // intentional
                        .setType(Resource.Type.DIRECTORY)
                        .setVisibility(request.getVisibility())
                        .setOwner(session.getUsername())
                        .setCreatedAt(Instant.now()) // now
                        .build();

                resourceManager_.createResource(newDirectory);
            } else if (!Resource.Type.DIRECTORY.equals(elementParent.getType())) {
                throw new ApiBadRequestException("Found no parent directory resource at path: "
                        + parentPath);
            }
        }
    }

    /**
     * Validates that the parent directory at the given path exists, is a directory, and is owned
     * by the authenticated user. Returns the parent {@link Resource} on success.
     */
    protected Resource validateAndGetParentDirectory(
            final String parentPath,
            final Session session) {
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
        return parent;
    }

    /**
     * Constructs a new file {@link Resource} from the given upload request, parent directory,
     * and owner session. The creation timestamp and cost are computed internally.
     */
    protected Resource buildNewFileResource(
            final String normalizedPath,
            final Resource parent,
            final UploadFileRequest request,
            final Session session) {
        final Instant now = Instant.now();
        final BigDecimal cost = costAnalyzer_.computeResourceCost(request.getSize(), now);

        return new Resource.Builder()
                .setPath(normalizedPath)
                .setParent(parent.getPath())
                .setSize(request.getSize())
                .setDescription(StringUtils.trimToEmpty(request.getDescription()))
                .setType(Resource.Type.FILE)
                .setVisibility(request.getVisibility())
                .setOwner(session.getUsername())
                .setCreatedAt(now)
                .setLastAccessedAt(now)
                .setCost(cost)
                .build();
    }

}
