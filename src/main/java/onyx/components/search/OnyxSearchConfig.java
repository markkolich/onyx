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

package onyx.components.search;

import com.typesafe.config.Config;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.OnyxConfig;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Component
public final class OnyxSearchConfig implements SearchConfig {

    private final Config config_;

    @Injectable
    public OnyxSearchConfig(
            final OnyxConfig onyxConfig) {
        config_ = onyxConfig.getOnyxConfig().getConfig(SEARCH_CONFIG_PATH);
    }

    // Solr config

    @Override
    public Path getSolrHomeDirectory() {
        return Paths.get(config_.getString(SOLR_HOME_DIR_PROP));
    }

    @Override
    public Path getSolrConfigDirectory() {
        return Paths.get(config_.getString(SOLR_CONFIG_DIR_PROP));
    }

    @Override
    public String getSolrCoreName() {
        return config_.getString(SOLR_CORE_NAME_PROP);
    }

    @Override
    public String getSolrNodeName() {
        return config_.getString(SOLR_NODE_NAME_PROP);
    }

    @Override
    public int getMaxResultsPerSearch() {
        return config_.getInt(SOLR_MAX_ROWS_PER_SEARCH_PROP);
    }

    // Indexer config

    @Override
    public boolean getIndexerRunOnAppStartup() {
        return config_.getBoolean(INDEXER_RUN_ON_APP_STARTUP_PROP);
    }

    @Override
    public boolean getIndexerRebuildOnSchedule() {
        return config_.getBoolean(INDEXER_REBUILD_ON_SCHEDULE_PROP);
    }

    @Override
    public String getIndexerRebuildCronExpression() {
        return config_.getString(INDEXER_REBUILD_CRON_EXPRESSION_PROP);
    }

    @Override
    public boolean getIndexerRebuildDeleteIndexFirst() {
        return config_.getBoolean(INDEXER_REBUILD_DELETE_INDEX_FIRST_PROP);
    }

    @Override
    public int getIndexerBackoffMaxRetries() {
        return config_.getInt(INDEXER_BACKOFF_MAX_RETRIES_PROP);
    }

    @Override
    public Duration getIndexerBackoffThrottleDuration() {
        return config_.getDuration(INDEXER_BACKOFF_THROTTLE_DURATION_PROP);
    }

}
