/*
 * Copyright (c) 2020 Mark S. Kolich
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

package onyx.components.aws.dynamodb.queries;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.collect.*;
import onyx.components.config.aws.AwsConfig;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static onyx.components.storage.ResourceManager.ROOT_PATH;

public final class DeleteResource {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteResource.class);

    private final AwsConfig awsConfig_;

    private final Resource resource_;

    private final String parentIndexName_;

    public DeleteResource(
            final AwsConfig awsConfig,
            final Resource resource) {
        awsConfig_ = checkNotNull(awsConfig, "AWS config cannot be null.");
        resource_ = checkNotNull(resource, "Resource cannot be null.");

        parentIndexName_ = awsConfig_.getAwsDynamoDbParentIndexName();
    }

    public void run(
            final IDynamoDBMapper dbMapper) {
        final List<FailedBatch> failedBatches = deleteResource(resource_, dbMapper);
        if (CollectionUtils.isNotEmpty(failedBatches)) {
            LOG.error("Failed to delete one or more resource batches in backing store: {}",
                    failedBatches.size());
        }
    }

    private List<FailedBatch> deleteResource(
            final Resource resource,
            final IDynamoDBMapper dbMapper) {
        final ImmutableList.Builder<FailedBatch> failedBatches = ImmutableList.builder();

        final Resource.Type resourceType = resource.getType();
        if (Resource.Type.FILE.equals(resourceType)) {
            dbMapper.delete(resource);
        } else if (Resource.Type.DIRECTORY.equals(resourceType)) {
            // First, delete the parent directory itself.
            dbMapper.delete(resource);

            final DynamoDBQueryExpression<Resource> qe = new DynamoDBQueryExpression<Resource>()
                    .withIndexName(parentIndexName_)
                    .withConsistentRead(false)
                    .withExpressionAttributeNames(buildExpressionAttributes())
                    .withExpressionAttributeValues(buildExpressionAttributeValues(resource.getPath()))
                    .withKeyConditionExpression(buildKeyConditionExpression());
            final PaginatedQueryList<Resource> queryResult = dbMapper.query(Resource.class, qe);

            final ListMultimap<Resource.Type, Resource> resources = queryResult.stream()
                    // Intentionally always skip the root "/" directory.
                    .filter(r -> !ROOT_PATH.equals(r.getPath()))
                    // Sort the results alphabetically based on path.
                    .sorted(Comparator.comparing(Resource::getPath))
                    .collect(Multimaps.toMultimap(Resource::getType, r -> r,
                    MultimapBuilder.ListMultimapBuilder.treeKeys().arrayListValues()::build));

            // Then, batch delete any files.
            final List<Resource> files = resources.get(Resource.Type.FILE);
            if (CollectionUtils.isNotEmpty(files)) {
                failedBatches.addAll(dbMapper.batchDelete(files));
            }

            // Lastly, recursively delete any directories.
            final List<Resource> directories = resources.get(Resource.Type.DIRECTORY);
            if (CollectionUtils.isNotEmpty(directories)) {
                for (final Resource directory : directories) {
                    failedBatches.addAll(deleteResource(directory, dbMapper));
                }
            }
        }

        return failedBatches.build();
    }

    private static Map<String, String> buildExpressionAttributes() {
        return ImmutableMap.of("#name0", "parent");
    }

    private static Map<String, AttributeValue> buildExpressionAttributeValues(
            final String path) {
        return ImmutableMap.of(":value0", new AttributeValue().withS(path));
    }

    private static String buildKeyConditionExpression() {
        return "#name0 = :value0";
    }

}
