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

import onyx.components.search.SearchConfig;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots a real embedded Solr against the config set this repository ships, through the same
 * components the application wires up, and then indexes and searches through them.
 *
 * <p>This is here because nothing else in the suite starts Solr, and so nothing else notices when
 * Solr can no longer start. An excluded transitive dependency, a Solr or Lucene upgrade that moves
 * a class, or a {@code solrconfig.xml} whose {@code luceneMatchVersion} no longer matches the
 * Lucene on the classpath all leave the build green and the compiler happy, and then fail on the
 * first request after a deploy. Two of those have already happened: a Jetty exclusion that looked
 * safe because Onyx never opens a Solr connection in fact broke
 * {@link org.apache.solr.client.solrj.embedded.EmbeddedSolrServer}'s construction, because its
 * bootstrap loads Solr's HTTP client classes regardless.
 *
 * <p>Everything runs out of a temporary Solr home, so no AWS service and no deployed index is
 * touched. The config set, however, is deliberately the real one on disk rather than a fixture:
 * a copy would stop being evidence the moment the two drifted apart.
 */
public final class EmbeddedSolrIntegrationTest {

    private static final String CORE_NAME = "onyx";
    private static final String NODE_NAME = "onyx-test";

    private static final String OWNER = "mark";

    /**
     * The config set the application deploys with: {@code schema.xml} and {@code solrconfig.xml}.
     * The core name is the directory Solr looks for underneath it.
     */
    private static final Path CONFIG_SET_DIRECTORY = Paths.get("solr", "config").toAbsolutePath();

    @TempDir
    private Path solrHomeDirectory_;

    private EmbeddedSolrServerManager serverManager_;
    private EmbeddedSolrSearchManager searchManager_;

    @BeforeEach
    public void startSolr() throws Exception {
        assertTrue(Files.isDirectory(CONFIG_SET_DIRECTORY.resolve(CORE_NAME).resolve("conf")),
                "Found no config set to boot Solr with: " + CONFIG_SET_DIRECTORY);

        final SearchConfig searchConfig = Mockito.mock(SearchConfig.class);
        Mockito.when(searchConfig.getSolrHomeDirectory()).thenReturn(solrHomeDirectory_);
        Mockito.when(searchConfig.getSolrConfigDirectory()).thenReturn(CONFIG_SET_DIRECTORY);
        Mockito.when(searchConfig.getSolrCoreName()).thenReturn(CORE_NAME);
        Mockito.when(searchConfig.getSolrNodeName()).thenReturn(NODE_NAME);
        Mockito.when(searchConfig.getMaxResultsPerSearch()).thenReturn(100);

        serverManager_ = new EmbeddedSolrServerManager(searchConfig);
        searchManager_ = new EmbeddedSolrSearchManager(searchConfig, serverManager_);
    }

    @AfterEach
    public void stopSolr() throws Exception {
        if (serverManager_ != null) {
            serverManager_.destroy();
        }
    }

    /**
     * The whole reason this test exists: that an embedded Solr starts at all, with the dependencies
     * the POM resolves and the config set the repository ships.
     */
    @Test
    public void embeddedSolrStartsAndCreatesItsCoreTest() throws Exception {
        // initialize() is what the application calls on startup, and it creates the core when it
        // finds none. Running it twice has to be safe, since every restart runs it again.
        serverManager_.initialize();
        serverManager_.initialize();

        // The core answers queries, which it cannot do unless it was created and loaded.
        assertEquals(0L, searchManager_.searchIndex(OWNER, "anything").size());
    }

    @Test
    public void indexesAndFindsAResourceTest() throws Exception {
        serverManager_.initialize();

        index(newFile("/mark/videos/holiday-clip.mp4", "A holiday clip", true),
                newFile("/mark/docs/taxes.pdf", "Tax return", false));

        // Found by name, and by description.
        assertEquals(List.of("/mark/videos/holiday-clip.mp4"), pathsOf(searchManager_.searchIndex(OWNER, "holiday")));
        assertEquals(List.of("/mark/docs/taxes.pdf"), pathsOf(searchManager_.searchIndex(OWNER, "tax return")));

        // A quoted query is an exact phrase match, which takes a different branch of the query builder.
        assertEquals(List.of("/mark/docs/taxes.pdf"), pathsOf(searchManager_.searchIndex(OWNER, "\"taxes.pdf\"")));

        assertTrue(searchManager_.searchIndex(OWNER, "nothingmatchesthis").isEmpty());
    }

    /**
     * The index holds private resources, so the owner filter is the only thing keeping one user's
     * files out of another's search results.
     */
    @Test
    public void searchIsScopedToTheOwnerTest() throws Exception {
        serverManager_.initialize();

        index(newFile("/mark/videos/holiday-clip.mp4", "A holiday clip", true));

        assertFalse(searchManager_.searchIndex(OWNER, "holiday").isEmpty());
        assertTrue(searchManager_.searchIndex("someone-else", "holiday").isEmpty());
    }

    /**
     * A query is user input, and it reaches Solr's query parser. Anything that would break out of
     * the owner clause, or blow up the parser, has to come back as no results rather than as an
     * exception or as somebody else's files.
     */
    @Test
    public void hostileQueriesAreEscapedTest() throws Exception {
        serverManager_.initialize();

        index(newFile("/mark/videos/holiday-clip.mp4", "A holiday clip", true));

        for (final String query : List.of(
                "holiday) OR (owner:someone-else",
                "*:*",
                "owner:someone-else",
                "holiday\" OR \"",
                "+-&&||!(){}[]^~*?:\\/")) {
            assertTrue(searchManager_.searchIndex("someone-else", query).isEmpty(),
                    "query leaked another owner's resources: " + query);
        }
    }

    @Test
    public void deletesAResourceFromTheIndexTest() throws Exception {
        serverManager_.initialize();

        final Resource resource = newFile("/mark/videos/holiday-clip.mp4", "A holiday clip", true);
        index(resource);
        assertFalse(searchManager_.searchIndex(OWNER, "holiday").isEmpty());

        deleteFromIndex(resource);
        assertTrue(searchManager_.searchIndex(OWNER, "holiday").isEmpty());
    }

    /**
     * Indexes and then commits. The production path deliberately does not commit - it leaves that
     * to the {@code autoSoftCommit} in solrconfig.xml, so a newly indexed resource becomes
     * searchable within that window rather than on the request that wrote it - and a test should
     * not sit and wait for the window to pass.
     */
    private void index(
            final Resource... resources) throws Exception {
        for (final Resource resource : resources) {
            searchManager_.addResourceToIndex(resource);
        }

        serverManager_.getSolrClient().commit();
    }

    private void deleteFromIndex(
            final Resource resource) throws Exception {
        searchManager_.deleteResourceFromIndex(resource);

        serverManager_.getSolrClient().commit();
    }

    private static List<String> pathsOf(
            final List<Resource> resources) {
        return resources.stream()
                .map(Resource::getPath)
                .toList();
    }

    private static Resource newFile(
            final String path,
            final String description,
            final boolean favorite) {
        final String parent = path.substring(0, path.lastIndexOf('/'));

        return new Resource.Builder()
                .setPath(path)
                .setParent(parent)
                .setSize(1234L)
                .setDescription(description)
                .setType(Resource.Type.FILE)
                .setVisibility(Resource.Visibility.PRIVATE)
                .setOwner(OWNER)
                .setCreatedAt(Instant.now())
                .setFavorite(favorite)
                .setCost(BigDecimal.ZERO)
                .build();
    }

}
