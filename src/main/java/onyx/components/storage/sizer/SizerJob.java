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

package onyx.components.storage.sizer;

import com.google.common.collect.ImmutableSet;
import onyx.components.storage.AssetManager;
import onyx.components.storage.ResourceManager;
import onyx.components.storage.sizer.cost.CostAnalyzer;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.util.TreeNode;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static onyx.util.CurrencyUtils.humanReadableCost;
import static onyx.util.FileUtils.humanReadableByteCountBin;
import static onyx.util.PathUtils.normalizePath;
import static onyx.util.RetryableUtil.callWithRetry;
import static onyx.util.RetryableUtil.runWithRetry;

/**
 * Recursively, depth-first-search, scans all resources/content found under
 * each home directory and updates each parent resource directory with the
 * aggregate size and cost of all of its children.
 */
public final class SizerJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(SizerJob.class);

    @Override
    public void execute(
            final JobExecutionContext context) throws JobExecutionException {
        final JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

        final SizerConfig sizerConfig =
                (SizerConfig) jobDataMap.get(SizerConfig.class.getSimpleName());
        final ResourceManager resourceManager =
                (ResourceManager) jobDataMap.get(ResourceManager.class.getSimpleName());
        final AssetManager assetManager =
                (AssetManager) jobDataMap.get(AssetManager.class.getSimpleName());
        final CostAnalyzer costAnalyzer =
                (CostAnalyzer) jobDataMap.get(CostAnalyzer.class.getSimpleName());

        final int backoffMaxRetries = sizerConfig.getBackoffMaxRetries();
        final Duration backoffThrottle = sizerConfig.getBackoffThrottleDuration();

        final List<Resource> homeDirectories =
                callWithRetry(backoffMaxRetries, backoffThrottle, resourceManager::listHomeDirectories);
        for (final Resource homeDirectory : homeDirectories) {
            final TreeNode rootNode = TreeNode.of();
            final long start = System.currentTimeMillis();

            final String normalizedPath = normalizePath(homeDirectory.getOwner(), ResourceManager.ROOT_PATH);

            final Resource resource = callWithRetry(backoffMaxRetries, backoffThrottle,
                    () -> resourceManager.getResourceAtPath(normalizedPath));
            if (resource != null) {
                rootNode.plus(sizeResource(backoffMaxRetries, backoffThrottle,
                        resourceManager, assetManager, costAnalyzer, resource));
            }

            final long end = System.currentTimeMillis();
            final String duration = DurationFormatUtils.formatDurationHMS(end - start);
            LOG.info("Successfully sized ({}) & cost-analyzed ({}) {} resources under home directory {} in {}",
                    humanReadableByteCountBin(rootNode.getSize()),
                    humanReadableCost(rootNode.getCost()),
                    rootNode.getResources(),
                    homeDirectory.getPath(),
                    duration);
        }
    }

    private static TreeNode sizeResource(
            final int backoffMaxRetries,
            final Duration backoffThrottle,
            final ResourceManager resourceManager,
            final AssetManager assetManager,
            final CostAnalyzer costAnalyzer,
            final Resource resource) {
        final TreeNode treeNode = TreeNode.of();

        if (Resource.Type.FILE.equals(resource.getType())) {
            final long resourceObjectSizeFromS3 = assetManager.getResourceObjectSize(resource);
            if (resourceObjectSizeFromS3 < 0L) {
                // If the file resource exists but does not resolve to a valid asset in S3
                // then log a warning and skip indexing of the file.
                LOG.warn("Sizer skipping non-existent resource in S3: {}", resource.getPath());
                return TreeNode.of();
            }

            boolean resourceUpdated = false;

            if (resourceObjectSizeFromS3 != resource.getSize()) {
                // If the resource exists but the size metadata does not match the size in S3,
                // then update accordingly to keep the worlds in sync.
                LOG.warn("Resource metadata does not match object size in S3 ({} != {}): {}",
                        resource.getSize(), resourceObjectSizeFromS3, resource.getPath());
                resource.setSize(resourceObjectSizeFromS3);
                resourceUpdated = true;
            }

            // Compute the cost based on the resource's size and access tier.
            final BigDecimal computedCost = costAnalyzer.computeResourceCost(resource);
            if (computedCost.compareTo(resource.getCost()) != 0) {
                resource.setCost(computedCost);
                resourceUpdated = true;
            }

            if (resourceUpdated) {
                runWithRetry(backoffMaxRetries, backoffThrottle,
                        () -> resourceManager.updateResource(resource));
            }

            treeNode.plus(TreeNode.of(1L, resource.getSize(), resource.getCost()));
        } else if (Resource.Type.DIRECTORY.equals(resource.getType())) {
            // Recursively index the contents of the directory.
            final Set<Resource.Visibility> visibility =
                    ImmutableSet.of(Resource.Visibility.PUBLIC, Resource.Visibility.PRIVATE);
            final List<Resource> directoryContents = callWithRetry(backoffMaxRetries, backoffThrottle,
                    () -> resourceManager.listDirectory(resource, visibility, null));
            for (final Resource child : directoryContents) {
                try {
                    // Recursive!
                    treeNode.plus(sizeResource(backoffMaxRetries, backoffThrottle,
                            resourceManager, assetManager, costAnalyzer, child));
                } catch (final Exception e) {
                    LOG.warn("Skipping resource - failed to size or cost: {}", child.getPath(), e);
                }
            }

            // Set the total size and cost of the directory (including all of its children)
            // only if what we have in hand from the underlying data store does not match
            // the new computed values.
            final boolean sizeChanged = treeNode.getSize() != resource.getSize();
            final boolean costChanged = treeNode.getCost().compareTo(resource.getCost()) != 0;
            if (sizeChanged || costChanged) {
                resource.setSize(treeNode.getSize());
                resource.setCost(treeNode.getCost());
                runWithRetry(backoffMaxRetries, backoffThrottle, () -> resourceManager.updateResource(resource));
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Successfully sized resource: {} ({}, {})",
                        resource.getPath(),
                        humanReadableByteCountBin(resource.getSize()),
                        humanReadableCost(resource.getCost()));
            }
        }

        return treeNode;
    }

}
