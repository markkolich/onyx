/*
 * Copyright (c) 2022 Mark S. Kolich
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

package onyx.components.search.indexer;

import com.google.common.collect.ImmutableSet;
import onyx.components.search.SearchConfig;
import onyx.components.search.SearchManager;
import onyx.components.storage.AssetManager;
import onyx.components.storage.ResourceManager;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.util.TreeNode;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static onyx.util.FileUtils.humanReadableByteCountBin;
import static onyx.util.PathUtils.normalizePath;
import static onyx.util.RetryableUtil.callWithRetry;
import static onyx.util.RetryableUtil.runWithRetry;

/**
 * Recursively indexes all resources/content found under each home directory.
 */
public final class IndexerJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerJob.class);

    @Override
    public void execute(
            final JobExecutionContext context) throws JobExecutionException {
        final JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

        final SearchConfig searchConfig =
                (SearchConfig) jobDataMap.get(SearchConfig.class.getSimpleName());
        final ResourceManager resourceManager =
                (ResourceManager) jobDataMap.get(ResourceManager.class.getSimpleName());
        final SearchManager searchManager =
                (SearchManager) jobDataMap.get(SearchManager.class.getSimpleName());
        final AssetManager assetManager =
                (AssetManager) jobDataMap.get(AssetManager.class.getSimpleName());

        final int backoffMaxRetries = searchConfig.getIndexerBackoffMaxRetries();
        final Duration backoffThrottle = searchConfig.getIndexerBackoffThrottleDuration();

        // Optionally, delete all documents in the index before a rebuild.
        final boolean indexerRebuildDeleteIndexFirst =
                searchConfig.getIndexerRebuildDeleteIndexFirst();
        if (indexerRebuildDeleteIndexFirst) {
            searchManager.deleteIndex();
        }

        // Then, rebuild the entire index based on current resource data.
        final List<Resource> homeDirectories =
                callWithRetry(backoffMaxRetries, backoffThrottle, resourceManager::listHomeDirectories);
        for (final Resource homeDirectory : homeDirectories) {
            final TreeNode rootNode = TreeNode.of();
            final long start = System.currentTimeMillis();

            final String normalizedPath = normalizePath(homeDirectory.getOwner(), ResourceManager.ROOT_PATH);

            final Resource resource = callWithRetry(backoffMaxRetries, backoffThrottle,
                    () -> resourceManager.getResourceAtPath(normalizedPath));
            if (resource != null) {
                rootNode.plus(indexResource(backoffMaxRetries, backoffThrottle,
                        resourceManager, searchManager, assetManager, resource));
            }

            final long end = System.currentTimeMillis();
            final String duration = DurationFormatUtils.formatDurationHMS(end - start);
            LOG.info("Successfully indexed {} ({}) resources under home directory {} in {}",
                    rootNode.getResources(),
                    humanReadableByteCountBin(rootNode.getSize()),
                    homeDirectory.getPath(),
                    duration);
        }
    }

    private static TreeNode indexResource(
            final int backoffMaxRetries,
            final Duration backoffThrottle,
            final ResourceManager resourceManager,
            final SearchManager searchManager,
            final AssetManager assetManager,
            final Resource resource) {
        final TreeNode treeNode = TreeNode.of();

        if (Resource.Type.FILE.equals(resource.getType())) {
            final long resourceObjectSizeFromS3 = assetManager.getResourceObjectSize(resource);
            if (resourceObjectSizeFromS3 < 0L) {
                // If the file resource exists but does not resolve to a valid asset in S3
                // then log a warning and skip indexing of the file.
                LOG.warn("Indexer skipping non-existent resource in S3: {}", resource.getPath());
                return TreeNode.of();
            }

            treeNode.plus(TreeNode.of(1L, resource.getSize()));
        } else if (Resource.Type.DIRECTORY.equals(resource.getType())) {
            // Recursively index the contents of the directory.
            final Set<Resource.Visibility> visibility =
                    ImmutableSet.of(Resource.Visibility.PUBLIC, Resource.Visibility.PRIVATE);
            final List<Resource> directoryContents = callWithRetry(backoffMaxRetries, backoffThrottle,
                    () -> resourceManager.listDirectory(resource, visibility, null));
            for (final Resource child : directoryContents) {
                try {
                    // Recursive!
                    treeNode.plus(indexResource(backoffMaxRetries, backoffThrottle,
                            resourceManager, searchManager, assetManager, child));
                } catch (final Exception e) {
                    LOG.warn("Skipping resource - failed to index: {}", child.getPath(), e);
                }
            }
        }

        runWithRetry(backoffMaxRetries, backoffThrottle,
                () -> searchManager.addResourceToIndex(resource));

        if (LOG.isDebugEnabled()) {
            LOG.debug("Successfully indexed resource: {} ({})", resource.getPath(),
                    humanReadableByteCountBin(resource.getSize()));
        }

        return treeNode;
    }

}
