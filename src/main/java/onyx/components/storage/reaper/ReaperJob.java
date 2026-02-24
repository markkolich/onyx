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

package onyx.components.storage.reaper;

import onyx.components.aws.s3.OnyxS3Client;
import onyx.components.storage.AssetManager;
import onyx.components.config.aws.AwsConfig;
import onyx.components.storage.ResourceManager;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static onyx.util.RetryableUtil.callWithRetry;

/**
 * Walks through all objects in the asset backing store (S3 bucket) and checks that the objects
 * also exist in the resource backing store (DynamoDB). If the file exists in S3 but does not
 * exist in Dynamo, then the object is considered a dead dangling pointer, and has no chance of
 * ever being accessed. Therefore, this reaper hard-deletes those dangling objects in S3. This
 * helps ensure that the object tree as managed by the {@link ResourceManager} stays in sync
 * with the objects accessible by the {@link AssetManager}.
 */
public final class ReaperJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(ReaperJob.class);

    @Override
    public void execute(
            final JobExecutionContext context) throws JobExecutionException {
        final JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

        final ReaperConfig reaperConfig =
                (ReaperConfig) jobDataMap.get(ReaperConfig.class.getSimpleName());
        final AwsConfig awsConfig =
                (AwsConfig) jobDataMap.get(AwsConfig.class.getSimpleName());
        final ResourceManager resourceManager =
                (ResourceManager) jobDataMap.get(ResourceManager.class.getSimpleName());
        final OnyxS3Client onyxS3Client =
                (OnyxS3Client) jobDataMap.get(OnyxS3Client.class.getSimpleName());
        final S3Client s3 = onyxS3Client.getS3Client();

        final int backoffMaxRetries = reaperConfig.getBackoffMaxRetries();
        final Duration backoffThrottle = reaperConfig.getBackoffThrottleDuration();

        final Duration iterationThrottle = reaperConfig.getIterationThrottleDuration();

        final AtomicLong counter = new AtomicLong(0L);

        final String bucketName = awsConfig.getAwsS3BucketName();

        try {
            final long start = System.currentTimeMillis();

            final ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            s3.listObjectsV2Paginator(listRequest).contents().stream()
                    .filter(objSummary -> !objSummary.key().startsWith(AssetManager.ONYX_METADATA_PATH_PREFIX))
                    .forEach((S3Object objSummary) -> {
                        final String resourcePath = ResourceManager.ROOT_PATH + objSummary.key();

                        final Resource resource = callWithRetry(backoffMaxRetries, backoffThrottle,
                                () -> resourceManager.getResourceAtPath(resourcePath));
                        if (resource == null) {
                            // If we got here, the resource was found in S3 but not Dynamo.
                            try {
                                s3.headObject(HeadObjectRequest.builder()
                                        .bucket(bucketName)
                                        .key(objSummary.key())
                                        .build());
                            } catch (final NoSuchKeyException e) {
                                LOG.error("Failed to load object metadata from S3 for key: {}",
                                        resourcePath);
                                return;
                            }

                            // Very intentionally not deleting the versioned object, only the object
                            // itself. In the case that the object was accidentally deleted on the
                            // reaper cleanup, it will be deleted and replaced with a delete marker
                            // so the object can be recovered later if needed. S3 lifecycle rules
                            // within the bucket itself can be configured to permanently delete the
                            // object and its delete marker if desired.
                            s3.deleteObject(DeleteObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(objSummary.key())
                                    .build());

                            LOG.info("Successfully deleted dangling resource in S3: {}",
                                    resourcePath);
                        }

                        counter.incrementAndGet();

                        try {
                            // Micro throttle (sleep) on each iteration to avoid pummeling S3
                            // and/or DynamoDB.
                            Thread.sleep(iterationThrottle.toMillis());
                        } catch (final InterruptedException e) {
                            // Ignored, intentional.
                        }
                    });

            final long end = System.currentTimeMillis();
            final String duration = DurationFormatUtils.formatDurationHMS(end - start);
            LOG.info("Reaper completed successful scan of {} total resources in {}",
                    counter.get(),
                    duration);
        } catch (final Exception e) {
            LOG.error("Reaper job failed after processing {} resources.", counter.get(), e);
        }
    }

}
