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

import com.typesafe.config.Config;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.OnyxConfig;

import java.time.Duration;

@Component
public final class OnyxBackupConfig implements BackupConfig {

    private final Config config_;

    @Injectable
    public OnyxBackupConfig(
            final OnyxConfig onyxConfig) {
        config_ = onyxConfig.getOnyxConfig().getConfig(BACKUP_CONFIG_PATH);
    }

    @Override
    public boolean getBackupRunOnAppStartup() {
        return config_.getBoolean(BACKUP_RUN_ON_APP_STARTUP_PROP);
    }

    @Override
    public boolean getBackupRunOnSchedule() {
        return config_.getBoolean(BACKUP_RUN_ON_SCHEDULE_PROP);
    }

    @Override
    public String getBackupRunCronExpression() {
        return config_.getString(BACKUP_RUN_CRON_EXPRESSION_PROP);
    }

    @Override
    public String getBackupNamePrefix() {
        return config_.getString(BACKUP_NAME_PREFIX_PROP);
    }

    @Override
    public Duration getBackupRetentionDuration() {
        return config_.getDuration(BACKUP_RETENTION_DURATION_PROP);
    }

    @Override
    public Duration getBackupAvailablePollInterval() {
        return config_.getDuration(BACKUP_AVAILABLE_POLL_INTERVAL_PROP);
    }

    @Override
    public Duration getBackupAvailableTimeout() {
        return config_.getDuration(BACKUP_AVAILABLE_TIMEOUT_PROP);
    }

}
