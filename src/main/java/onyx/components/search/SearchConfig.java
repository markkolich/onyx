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

import java.nio.file.Path;
import java.time.Duration;

public interface SearchConfig {

    String SEARCH_CONFIG_PATH = "search";

    String SOLR_HOME_DIR_PROP = "solr.home-dir";
    String SOLR_CONFIG_DIR_PROP = "solr.config-dir";
    String SOLR_CORE_NAME_PROP = "solr.core-name";
    String SOLR_NODE_NAME_PROP = "solr.node-name";

    String INDEXER_RUN_ON_APP_STARTUP_PROP = "indexer.run-on-app-startup";
    String INDEXER_REBUILD_ON_SCHEDULE_PROP = "indexer.rebuild-on-schedule";
    String INDEXER_REBUILD_CRON_EXPRESSION_PROP = "indexer.rebuild-cron-expression";
    String INDEXER_REBUILD_DELETE_INDEX_FIRST_PROP = "indexer.rebuild-delete-index-first";
    String INDEXER_BACKOFF_MAX_RETRIES_PROP = "indexer.backoff-max-retries";
    String INDEXER_BACKOFF_THROTTLE_DURATION_PROP = "indexer.backoff-throttle-duration";

    // Solr config

    Path getSolrHomeDirectory();

    Path getSolrConfigDirectory();

    String getSolrCoreName();

    String getSolrNodeName();

    // Indexer config

    boolean getIndexerRunOnAppStartup();

    boolean getIndexerRebuildOnSchedule();

    String getIndexerRebuildCronExpression();

    boolean getIndexerRebuildDeleteIndexFirst();

    int getIndexerBackoffMaxRetries();

    Duration getIndexerBackoffThrottleDuration();

}
