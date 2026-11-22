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

package onyx.components.search.solr;

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.components.CuracaoComponent;
import onyx.components.search.SearchConfig;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.api.model.CoreStatusResponse;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.MetricsConfig;
import org.apache.solr.core.NodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public final class EmbeddedSolrServerManager implements SolrClientProvider, CuracaoComponent {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedSolrServerManager.class);

    private static final String CORE_DIR_NAME = "core";

    private final SearchConfig searchConfig_;

    private final SolrClient solrClient_;

    @Injectable
    public EmbeddedSolrServerManager(
            final SearchConfig searchConfig) throws Exception {
        searchConfig_ = searchConfig;

        final Path solrHomeDir = searchConfig_.getSolrHomeDirectory();
        if (Files.notExists(solrHomeDir)) {
            Files.createDirectories(solrHomeDir);
        }

        final Path coreRootDir = solrHomeDir.resolve(CORE_DIR_NAME).toAbsolutePath();
        if (Files.notExists(coreRootDir)) {
            Files.createDirectories(coreRootDir);
        }

        final Path solrConfigDir = searchConfig_.getSolrConfigDirectory();
        if (Files.notExists(solrConfigDir)) {
            Files.createDirectories(solrConfigDir);
        }

        final String coreName = searchConfig_.getSolrCoreName();
        final String nodeName = searchConfig_.getSolrNodeName();

        // Intentionally disable metrics - it's not needed here.
        final MetricsConfig metricsConfig = new MetricsConfig.MetricsConfigBuilder()
                .setEnabled(false)
                .build();

        final NodeConfig config = new NodeConfig.NodeConfigBuilder(nodeName, solrHomeDir)
                .setCoreRootDirectory(coreRootDir.toString())
                .setConfigSetBaseDirectory(solrConfigDir.toString())
                .setMetricsConfig(metricsConfig)
                .build();

        solrClient_ = new EmbeddedSolrServer(config, coreName);
    }

    @Override
    public SolrClient getSolrClient() {
        return solrClient_;
    }

    @Override
    public void initialize() throws Exception {
        final String coreName = searchConfig_.getSolrCoreName();

        // Ask the core admin handler whether the core is already there, and create it on
        // app-startup when it isn't. This used to probe with a ping() and treat the failure as
        // "absent", which Solr 10 deprecates; asking for the core's status says the same thing
        // directly, and does not confuse a core that exists but is unwell with one that was
        // never created.
        if (!coreExists(coreName)) {
            LOG.debug("Found no existing Solr core - creating core: {}", coreName);

            final CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName(coreName);
            createRequest.setConfigSet(coreName);
            solrClient_.request(createRequest);
            solrClient_.commit();
        }
    }

    @Override
    public void destroy() throws Exception {
        solrClient_.close();
    }

    /**
     * Whether a core of the given name already exists. The core admin handler answers for a core it
     * has never heard of with an entry carrying no name, rather than with an error, so the name is
     * what gets checked here.
     */
    private boolean coreExists(
            final String coreName) throws Exception {
        final CoreStatusResponse.SingleCoreData coreStatus =
                CoreAdminRequest.getCoreStatus(coreName, solrClient_);

        return coreStatus != null && coreStatus.name != null;
    }

}
