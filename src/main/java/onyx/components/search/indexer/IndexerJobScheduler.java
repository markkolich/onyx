/*
 * Copyright (c) 2023 Mark S. Kolich
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

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.quartz.QuartzSchedulerFactory;
import onyx.components.search.SearchConfig;
import onyx.components.search.SearchManager;
import onyx.components.storage.AssetManager;
import onyx.components.storage.ResourceManager;
import org.quartz.*;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Component
public final class IndexerJobScheduler {

    private final Scheduler quartzScheduler_;

    @Injectable
    public IndexerJobScheduler(
            final SearchConfig searchConfig,
            final QuartzSchedulerFactory quartzSchedulerFactory,
            final ResourceManager resourceManager,
            final SearchManager searchManager,
            final AssetManager assetManager) throws Exception {
        quartzScheduler_ = quartzSchedulerFactory.getScheduler();

        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(SearchConfig.class.getSimpleName(), searchConfig);
        jobDataMap.put(ResourceManager.class.getSimpleName(), resourceManager);
        jobDataMap.put(SearchManager.class.getSimpleName(), searchManager);
        jobDataMap.put(AssetManager.class.getSimpleName(), assetManager);

        final JobDetail job = newJob(IndexerJob.class)
                .withIdentity(IndexerJob.class.getSimpleName())
                .storeDurably()
                .setJobData(jobDataMap)
                .build();

        final boolean rebuildIndexOnSchedule = searchConfig.getIndexerRebuildOnSchedule();
        if (rebuildIndexOnSchedule) {
            final Trigger trigger = newTrigger()
                    .withSchedule(cronSchedule(searchConfig.getIndexerRebuildCronExpression()))
                    .build();

            quartzScheduler_.scheduleJob(job, trigger);
        }

        final boolean runIndexerOnAppStartup = searchConfig.getIndexerRunOnAppStartup();
        if (runIndexerOnAppStartup) {
            quartzScheduler_.addJob(job, true);
            quartzScheduler_.triggerJob(JobKey.jobKey(IndexerJob.class.getSimpleName())); // Fire now!
        }
    }

}
