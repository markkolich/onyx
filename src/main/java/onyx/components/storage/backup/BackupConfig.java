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

import java.time.Duration;

public interface BackupConfig {

    String BACKUP_CONFIG_PATH = "backup";

    String BACKUP_RUN_ON_APP_STARTUP_PROP = "run-on-app-startup";
    String BACKUP_RUN_ON_SCHEDULE_PROP = "run-on-schedule";
    String BACKUP_RUN_CRON_EXPRESSION_PROP = "run-cron-expression";

    String BACKUP_NAME_PREFIX_PROP = "name-prefix";
    String BACKUP_RETENTION_DURATION_PROP = "retention-duration";
    String BACKUP_AVAILABLE_POLL_INTERVAL_PROP = "available-poll-interval";
    String BACKUP_AVAILABLE_TIMEOUT_PROP = "available-timeout";

    boolean getBackupRunOnAppStartup();

    boolean getBackupRunOnSchedule();

    String getBackupRunCronExpression();

    /**
     * Prefix for the names of backups this job creates, e.g. {@code "onyx-auto-"}. A UTC
     * timestamp is appended to produce the full backup name.
     */
    String getBackupNamePrefix();

    /**
     * How long a backup is kept before this job deletes it. Pruning only ever runs after a
     * new backup has been created and confirmed available, so the freshest backup is always
     * retained no matter how small this value is.
     */
    Duration getBackupRetentionDuration();

    /**
     * How long to wait between polls while a newly created backup is still reporting
     * {@code CREATING}.
     */
    Duration getBackupAvailablePollInterval();

    /**
     * How long to wait for a newly created backup to reach {@code AVAILABLE} before giving
     * up and skipping the prune for this run.
     */
    Duration getBackupAvailableTimeout();

}
