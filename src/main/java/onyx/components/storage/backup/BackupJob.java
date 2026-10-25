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

package onyx.components.storage.backup;

import onyx.components.aws.dynamodb.OnyxDynamoDbClient;
import onyx.components.config.aws.AwsConfig;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.BackupDetails;
import software.amazon.awssdk.services.dynamodb.model.BackupStatus;
import software.amazon.awssdk.services.dynamodb.model.BackupSummary;
import software.amazon.awssdk.services.dynamodb.model.BackupTypeFilter;
import software.amazon.awssdk.services.dynamodb.model.CreateBackupRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteBackupRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeBackupRequest;
import software.amazon.awssdk.services.dynamodb.model.ListBackupsRequest;
import software.amazon.awssdk.services.dynamodb.model.ListBackupsResponse;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Takes an on-demand snapshot backup of the resource backing store (DynamoDB table), then
 * deletes any backup of that table older than the configured retention duration.
 *
 * <p>Deliberately an on-demand snapshot rather than point-in-time recovery: snapshots are
 * durable artifacts that outlive the table and never silently roll out of a retention window,
 * at the cost of a coarser recovery point (one scheduled interval, rather than PITR's handful
 * of seconds).
 *
 * <p>Pruning is gated on the new backup having been created <em>and</em> reaching
 * {@code AVAILABLE}. Since that backup is necessarily newer than any retention cutoff, the
 * table can never be left without at least one usable backup, even when every older one
 * expires on the same run.
 *
 * <p>Unlike the reaper and the sizer, this job neither throttles its iterations nor wraps its
 * calls in {@code RetryableUtil}. Every call it makes is a control-plane operation which
 * consumes no provisioned capacity and so cannot be capacity-throttled, and the AWS SDK
 * already retries the transient failures these calls do produce (notably
 * {@code LimitExceededException}) under its default retry strategy. A second retry layer on
 * top would only obscure the cause of a genuine failure, such as the
 * {@code AccessDeniedException} raised when the IAM policy is missing the backup permissions.
 */
public final class BackupJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(BackupJob.class);

    /**
     * Timestamps backup names to the second, in UTC. To the second rather than the day so that
     * two runs on the same day cannot collide, and ordered most-significant-first so that a
     * lexical sort of backup names is also a chronological one.
     */
    private static final DateTimeFormatter BACKUP_NAME_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    @Override
    public void execute(
            final JobExecutionContext context) throws JobExecutionException {
        final JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

        final BackupConfig backupConfig =
                (BackupConfig) jobDataMap.get(BackupConfig.class.getSimpleName());
        final AwsConfig awsConfig =
                (AwsConfig) jobDataMap.get(AwsConfig.class.getSimpleName());
        final OnyxDynamoDbClient onyxDynamoDbClient =
                (OnyxDynamoDbClient) jobDataMap.get(OnyxDynamoDbClient.class.getSimpleName());
        final DynamoDbClient dynamoDb = onyxDynamoDbClient.getDbClient();

        final String tableName = awsConfig.getAwsDynamoDbTableName();

        try {
            final long start = System.currentTimeMillis();

            final String backupName = backupConfig.getBackupNamePrefix()
                    + BACKUP_NAME_TIMESTAMP_FORMATTER.format(Instant.now());

            final CreateBackupRequest createRequest = CreateBackupRequest.builder()
                    .tableName(tableName)
                    .backupName(backupName)
                    .build();

            final BackupDetails created = dynamoDb.createBackup(createRequest).backupDetails();

            LOG.info("Created backup of table '{}': {}", tableName, created.backupName());

            if (!awaitBackupAvailable(dynamoDb, created, backupConfig)) {
                LOG.error("Backup did not become available within {}, skipping prune: {}",
                        backupConfig.getBackupAvailableTimeout(),
                        created.backupName());
                return;
            }

            final int deleted = deleteExpiredBackups(dynamoDb, tableName,
                    created.backupArn(),
                    backupConfig);

            final long end = System.currentTimeMillis();
            final String duration = DurationFormatUtils.formatDurationHMS(end - start);
            LOG.info("Backup completed for table '{}' in {}, deleted {} expired backup(s)",
                    tableName,
                    duration,
                    deleted);
        } catch (final Exception e) {
            LOG.error("Backup job failed for table: {}", tableName, e);
        }
    }

    /**
     * Polls until the newly created backup reports {@code AVAILABLE}, returning false if it
     * does not get there within the configured timeout.
     *
     * <p>{@code CreateBackup} returns as soon as the request is accepted, typically with status
     * {@code CREATING}. Deleting expired backups before the replacement is durable could, in
     * the worst case, leave the table with no usable backup at all.
     */
    private static boolean awaitBackupAvailable(
            final DynamoDbClient dynamoDb,
            final BackupDetails created,
            final BackupConfig backupConfig) {
        if (BackupStatus.AVAILABLE.equals(created.backupStatus())) {
            return true;
        }

        final Duration pollInterval = backupConfig.getBackupAvailablePollInterval();
        final Instant deadline = Instant.now().plus(backupConfig.getBackupAvailableTimeout());

        final DescribeBackupRequest describeRequest = DescribeBackupRequest.builder()
                .backupArn(created.backupArn())
                .build();

        while (Instant.now().isBefore(deadline)) {
            sleepQuietly(pollInterval);

            final BackupStatus status = dynamoDb.describeBackup(describeRequest)
                    .backupDescription()
                    .backupDetails()
                    .backupStatus();

            if (BackupStatus.AVAILABLE.equals(status)) {
                return true;
            } else if (BackupStatus.DELETED.equals(status)) {
                LOG.error("Newly created backup was deleted before becoming available: {}",
                        created.backupName());
                return false;
            }
        }

        return false;
    }

    /**
     * Deletes every backup of the table older than the retention cutoff, returning how many
     * were deleted.
     */
    private static int deleteExpiredBackups(
            final DynamoDbClient dynamoDb,
            final String tableName,
            final String createdBackupArn,
            final BackupConfig backupConfig) {
        // Computed once, deliberately, rather than per page: re-deriving it inside the loop
        // would let the cutoff drift later as the walk runs.
        final Instant cutoff = Instant.now().minus(backupConfig.getBackupRetentionDuration());

        String exclusiveStartBackupArn = null;
        boolean morePages = true;
        int deleted = 0;

        while (morePages) {
            // timeRangeUpperBound filters server side, so only backups already older than the
            // cutoff are ever returned. BackupTypeFilter.USER excludes SYSTEM backups, which
            // DynamoDB creates by itself when a table with PITR enabled is deleted, and which
            // DeleteBackup refuses to remove.
            final ListBackupsRequest listRequest = ListBackupsRequest.builder()
                    .tableName(tableName)
                    .timeRangeUpperBound(cutoff)
                    .backupType(BackupTypeFilter.USER)
                    .exclusiveStartBackupArn(exclusiveStartBackupArn)
                    .build();

            final ListBackupsResponse response = dynamoDb.listBackups(listRequest);

            for (final BackupSummary summary : response.backupSummaries()) {
                // Belt and braces: the backup created moments ago cannot be older than the
                // cutoff, so it should never be listed here - but never delete it regardless.
                if (summary.backupArn().equals(createdBackupArn)) {
                    continue;
                }

                // A backup still being created, or already being deleted, cannot be deleted;
                // DeleteBackup rejects it outright. Skipping leaves it for the next run rather
                // than failing the whole prune over one transient entry.
                if (!BackupStatus.AVAILABLE.equals(summary.backupStatus())) {
                    LOG.warn("Skipping expired backup in state {}: {}",
                            summary.backupStatusAsString(),
                            summary.backupName());
                    continue;
                }

                final DeleteBackupRequest deleteRequest = DeleteBackupRequest.builder()
                        .backupArn(summary.backupArn())
                        .build();

                dynamoDb.deleteBackup(deleteRequest);

                LOG.info("Deleted expired backup of table '{}': {} (created {})",
                        tableName,
                        summary.backupName(),
                        summary.backupCreationDateTime());

                deleted++;
            }

            exclusiveStartBackupArn = response.lastEvaluatedBackupArn();
            morePages = exclusiveStartBackupArn != null;
        }

        return deleted;
    }

    private static void sleepQuietly(
            final Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (final InterruptedException e) {
            // Ignored, intentional.
        }
    }

}
