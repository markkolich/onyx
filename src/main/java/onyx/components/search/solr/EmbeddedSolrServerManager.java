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

package onyx.components.search.solr;

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.components.CuracaoComponent;
import onyx.components.search.SearchConfig;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
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
        try {
            // Check if the core exists; send a ping which will fail with an
            // exception if the core hasn't been created yet, which will then be
            // our trigger to create it on app-startup.
            solrClient_.ping();
        } catch (final Exception e) {
            LOG.debug("Initialize ping failed (as likely expected) - creating core!", e);
            final String coreName = searchConfig_.getSolrCoreName();

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

}
