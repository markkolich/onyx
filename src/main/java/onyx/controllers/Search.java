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

package onyx.controllers;

import com.google.common.collect.ImmutableList;
import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.Query;
import onyx.components.config.OnyxConfig;
import onyx.components.search.SearchManager;
import onyx.components.storage.AsynchronousResourcePool;
import onyx.components.storage.ResourceManager;
import onyx.entities.authentication.Session;
import onyx.entities.freemarker.FreeMarkerContent;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.resource.ResourceNotFoundException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.kolich.common.util.URLEncodingUtils.urlDecode;
import static onyx.util.PathUtils.splitNormalizedPathToElements;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

@Controller
public final class Search extends AbstractOnyxFreeMarkerController {

    private static final Logger LOG = LoggerFactory.getLogger(Search.class);

    private final SearchManager searchManager_;

    @Injectable
    public Search(
            final OnyxConfig onyxConfig,
            final AsynchronousResourcePool asynchronousResourcePool,
            final ResourceManager resourceManager,
            final SearchManager searchManager) {
        super(onyxConfig, asynchronousResourcePool, resourceManager);
        searchManager_ = searchManager;
    }

    @RequestMapping(value = "^/search$")
    public FreeMarkerContent doSearch(
            @Query("query") final String query,
            final Session session) throws Exception {
        if (session == null) {
            throw new ResourceNotFoundException("User not authenticated.");
        }

        final List<Resource> resultsFromIndex =
                searchManager_.searchIndex(session.getUsername(), query);

        final List<Pair<Resource, List<Triple<String, String, String>>>> results = resultsFromIndex.stream()
                .map(r -> Pair.of(r, splitNormalizedPathToElements(r.getPath())))
                .collect(ImmutableList.toImmutableList());

        LOG.debug("Found {}-results for query: {}", results.size(), query);

        // Note we HTML escape the URL decode the query for safe inclusion in an
        // HTML-template at render time.
        final String escapedQuery = escapeHtml4(urlDecode(query));

        return new FreeMarkerContent.Builder("templates/search.ftl")
                .withSession(session)
                .withAttr("query", escapedQuery)
                .withAttr("results", results)
                .build();
    }

}
