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

import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.s3.model.Region;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.aws.dynamodb.DynamoDbMapper;
import onyx.components.config.aws.AwsConfig;
import onyx.components.search.SearchManager;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.search.SearchException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public final class EmbeddedSolrSearchManager implements SearchManager {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedSolrSearchManager.class);

    private static final int DEFAULT_ROW_COUNT = 25;

    private final AwsConfig awsConfig_;

    private final SolrClient solrClient_;

    private final IDynamoDBMapper dbMapper_;

    @Injectable
    public EmbeddedSolrSearchManager(
            final AwsConfig awsConfig,
            final SolrClientProvider solrClientProvider,
            final DynamoDbMapper dynamoDbMapper) {
        awsConfig_ = awsConfig;

        solrClient_ = solrClientProvider.getSolrClient();
        dbMapper_ = dynamoDbMapper.getDbMapper();
    }

    @Override
    public void addResourceToIndex(
            final Resource resource) {
        checkNotNull(resource, "Resource to add to index cannot be null.");

        addResourcesToIndex(ImmutableList.of(resource));
    }

    @Override
    public void addResourcesToIndex(
            final Collection<Resource> resources) {
        checkNotNull(resources, "Resources to add to index cannot be null.");
        if (Iterables.isEmpty(resources)) {
            return;
        }

        final List<SolrInputDocument> documents = resources.stream()
                .map(EmbeddedSolrSearchManager::mapResourceToSolrInputDocument)
                .collect(ImmutableList.toImmutableList());

        try {
            solrClient_.add(documents);
        } catch (final Exception e) {
            throw new SearchException("Failed to add documents to search index.", e);
        }
    }

    @Override
    public void deleteResourceFromIndex(
            final Resource resource) {
        checkNotNull(resource, "Resources to delete from index cannot be null.");

        try {
            solrClient_.deleteById(resource.getPath());
        } catch (final Exception e) {
            throw new SearchException("Failed to delete document from search index: "
                    + resource.getPath(), e);
        }
    }

    @Override
    public void deleteResourcesFromIndex(
            final Collection<Resource> resources) {
        checkNotNull(resources, "Resources to delete from index cannot be null.");
        if (Iterables.isEmpty(resources)) {
            return;
        }

        final List<String> resourcePaths = resources.stream()
                .map(Resource::getPath)
                .collect(ImmutableList.toImmutableList());

        try {
            solrClient_.deleteById(resourcePaths);
        } catch (final Exception e) {
            throw new SearchException("Failed to delete documents from search index.", e);
        }
    }

    @Override
    public void deleteIndex() {
        try {
            // https://solr.apache.org/guide/8_0/reindexing.html#delete-all-documents
            // The best approach is to first delete everything from the index, and then index
            // your data again. Here, we delete all documents with a "delete-by-query":
            solrClient_.deleteByQuery("*:*");
        } catch (final Exception e) {
            throw new SearchException("Failed to delete search index.", e);
        }
    }

    @Override
    public List<Resource> searchIndex(
            final String owner,
            final String query) {
        if (StringUtils.isBlank(query)) {
            return ImmutableList.of();
        }

        final StringBuilder sb = new StringBuilder();
        if (query.startsWith(":")) {
            final String cleanedQuery = cleanQuery(query.substring(1));
            sb.append("type:").append(Resource.Type.FILE);
            sb.append(" AND ");
            sb.append("owner:").append(owner);
            sb.append(" AND ")
                    .append("nameLower:*")
                    .append(cleanedQuery)
                    .append("*");
        } else if (query.startsWith("/")) {
            final String cleanedQuery = cleanQuery(query.substring(1));
            sb.append("type:").append(Resource.Type.DIRECTORY);
            sb.append(" AND ");
            sb.append("owner:").append(owner);
            sb.append(" AND (");
            sb.append("nameLower:*")
                    .append(cleanedQuery)
                    .append("*");
            sb.append(" OR ");
            sb.append("pathLower:*")
                    .append(cleanedQuery)
                    .append("*");
            sb.append(")");
        } else {
            final String cleanedQuery = cleanQuery(query);
            sb.append("owner:").append(owner);
            sb.append(" AND ");
            sb.append("(")
                    .append("nameLower:").append("*").append(cleanedQuery).append("*")
                    .append(" OR ")
                    .append("descriptionLower:").append("*").append(cleanedQuery).append("*")
                    .append(")");
        }

        try {
            final SolrQuery solrQuery = new SolrQuery(sb.toString())
                    .addSort(SearchManager.QUERY_FIELD_SCORE, SolrQuery.ORDER.desc)
                    .setRows(DEFAULT_ROW_COUNT);
            final QueryResponse response = solrClient_.query(solrQuery);
            final SolrDocumentList documents = response.getResults();

            return documents.stream()
                    .map(document -> mapSolrDocumentToResource(document, awsConfig_, dbMapper_))
                    .collect(ImmutableList.toImmutableList());
        } catch (final Exception e) {
            LOG.error("Failed to search index for query: {}", query, e);
            return ImmutableList.of();
        }
    }

    private static String cleanQuery(
            final String query) {
        checkNotNull(query, "Query to clean cannot be null.");

        return ClientUtils.escapeQueryChars(StringUtils.trim(query));
    }

    private static SolrInputDocument mapResourceToSolrInputDocument(
            final Resource resource) {
        checkNotNull(resource, "Resource to map cannot be null.");

        final SolrInputDocument doc = new SolrInputDocument();
        doc.addField(INDEX_FIELD_PATH, resource.getPath());
        doc.addField(INDEX_FIELD_PATH_LOWER, resource.getPath());
        doc.addField(INDEX_FIELD_PARENT, resource.getParent());
        doc.addField(INDEX_FIELD_DESCRIPTION, resource.getDescription());
        doc.addField(INDEX_FIELD_DESCRIPTION_LOWER, resource.getDescription());
        doc.addField(INDEX_FIELD_SIZE, resource.getSize());
        doc.addField(INDEX_FIELD_TYPE, resource.getType().toString());
        doc.addField(INDEX_FIELD_VISIBILITY, resource.getVisibility().toString());
        doc.addField(INDEX_FIELD_OWNER, resource.getOwner());
        doc.addField(INDEX_FIELD_CREATED, resource.getCreatedAt().toString());
        doc.addField(INDEX_FIELD_FAVORITE, resource.getFavorite());

        // Derived fields
        doc.addField(INDEX_FIELD_NAME, resource.getName());
        doc.addField(INDEX_FIELD_NAME_LOWER, resource.getName());

        return doc;
    }

    private static Resource mapSolrDocumentToResource(
            final SolrDocument document,
            final AwsConfig awsConfig,
            final IDynamoDBMapper dbMapper) {
        checkNotNull(document, "Solr document to map cannot be null.");
        checkNotNull(awsConfig, "AWS config cannot be null.");
        checkNotNull(dbMapper, "DB mapper cannot be null.");

        final Resource.Type type =
                Resource.Type.valueOf((String) document.get(INDEX_FIELD_TYPE));
        final Resource.Visibility visibility =
                Resource.Visibility.valueOf((String) document.get(INDEX_FIELD_VISIBILITY));
        // Descriptions are optional.
        final String description =
                StringUtils.defaultIfBlank((String) document.get(INDEX_FIELD_DESCRIPTION), "");

        return new Resource.Builder()
                .setPath((String) document.get(INDEX_FIELD_PATH))
                .setParent((String) document.get(INDEX_FIELD_PARENT))
                .setDescription(description)
                .setSize((Long) document.get(INDEX_FIELD_SIZE))
                .setType(type)
                .setVisibility(visibility)
                .setOwner((String) document.get(INDEX_FIELD_OWNER))
                .setCreatedAt(((Date) document.get(INDEX_FIELD_CREATED)).toInstant())
                .setFavorite((Boolean) document.get(INDEX_FIELD_FAVORITE))
                .withS3BucketRegion(Region.fromValue(awsConfig.getAwsS3Region().getName()))
                .withS3Bucket(awsConfig.getAwsS3BucketName())
                .withDbMapper(dbMapper)
                .build();
    }

}
