/*
 * Copyright (c) 2021 Mark S. Kolich
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
import onyx.components.search.SearchManager;
import onyx.components.storage.ResourceManager;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static onyx.util.PathUtils.normalizePath;

/**
 * Recursively indexes all resources/content found under each home directory.
 */
public final class IndexerJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerJob.class);

    @Override
    public void execute(
            final JobExecutionContext context) throws JobExecutionException {
        final JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

        final ResourceManager resourceManager =
                (ResourceManager) jobDataMap.get(ResourceManager.class.getSimpleName());
        final SearchManager searchManager =
                (SearchManager) jobDataMap.get(SearchManager.class.getSimpleName());

        final long start = System.currentTimeMillis();

        // First, nuke everything in the index.
        searchManager.deleteIndex();

        // Then, rebuild the entire index based on current resource data.
        int documentsIndexed = 0;
        final List<Resource> homeDirectories = resourceManager.listHomeDirectories();
        for (final Resource homeDirectory : homeDirectories) {
            final String normalizedPath = normalizePath(homeDirectory.getOwner(), ResourceManager.ROOT_PATH);

            final Resource resource = resourceManager.getResourceAtPath(normalizedPath);
            if (resource != null) {
                documentsIndexed += indexResource(resourceManager, searchManager, resource);
            }
        }

        final long end = System.currentTimeMillis();
        LOG.info("Successfully indexed {} resources in {}", documentsIndexed,
                DurationFormatUtils.formatDurationHMS(end - start));
    }

    private int indexResource(
            final ResourceManager resourceManager,
            final SearchManager searchManager,
            final Resource resource) {
        int documentsIndexed = 0;

        searchManager.addResourceToIndex(resource);
        documentsIndexed++;
        LOG.debug("Successfully indexed resource: {}", resource.getPath());

        if (Resource.Type.DIRECTORY.equals(resource.getType())) {
            final Set<Resource.Visibility> visibility =
                    ImmutableSet.of(Resource.Visibility.PUBLIC, Resource.Visibility.PRIVATE);
            final List<Resource> directoryContents =
                    resourceManager.listDirectory(resource, visibility, null);
            for (final Resource child : directoryContents) {
                documentsIndexed += indexResource(resourceManager, searchManager, child); // Recursive!
            }
        }

        return documentsIndexed;
    }

}
