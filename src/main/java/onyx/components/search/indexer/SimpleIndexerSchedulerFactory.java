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

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.search.SearchConfig;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import javax.annotation.Nonnull;
import java.util.Properties;

import static org.quartz.impl.StdSchedulerFactory.PROP_THREAD_POOL_PREFIX;

@Component
public final class SimpleIndexerSchedulerFactory implements IndexerSchedulerFactory {

    private static final String PROP_THREAD_POOL_COUNT = PROP_THREAD_POOL_PREFIX + ".threadCount";
    private static final String PROP_THREAD_POOL_USE_DAEMONS = PROP_THREAD_POOL_PREFIX + ".makeThreadsDaemons";

    private final SearchConfig searchConfig_;

    @Injectable
    public SimpleIndexerSchedulerFactory(
            final SearchConfig searchConfig) {
        searchConfig_ = searchConfig;
    }

    @Nonnull
    @Override
    public Scheduler getNewScheduler() throws SchedulerException {
        final Properties p = new Properties();
        p.put(PROP_THREAD_POOL_COUNT, Integer.toString(searchConfig_.getIndexerThreadPoolSize()));
        p.put(PROP_THREAD_POOL_USE_DAEMONS, Boolean.toString(searchConfig_.getIndexerUseDaemonThreads()));

        return new StdSchedulerFactory(p).getScheduler();
    }

}
