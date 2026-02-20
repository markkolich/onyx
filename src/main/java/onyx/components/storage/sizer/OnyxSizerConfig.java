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

package onyx.components.storage.sizer;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.OnyxConfig;
import onyx.components.storage.sizer.cost.StorageTier;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@Component
public final class OnyxSizerConfig implements SizerConfig {

    private final Config config_;

    @Injectable
    public OnyxSizerConfig(
            final OnyxConfig onyxConfig) {
        config_ = onyxConfig.getOnyxConfig().getConfig(SIZER_CONFIG_PATH);
    }

    @Override
    public boolean getSizerRunOnAppStartup() {
        return config_.getBoolean(SIZER_RUN_ON_APP_STARTUP_PROP);
    }

    @Override
    public boolean getSizerRunOnSchedule() {
        return config_.getBoolean(SIZER_RUN_ON_SCHEDULE_PROP);
    }

    @Override
    public String getSizerRunCronExpression() {
        return config_.getString(SIZER_RUN_CRON_EXPRESSION_PROP);
    }

    @Override
    public int getBackoffMaxRetries() {
        return config_.getInt(SIZER_BACKOFF_MAX_RETRIES_PROP);
    }

    @Override
    public Duration getBackoffThrottleDuration() {
        return config_.getDuration(SIZER_BACKOFF_THROTTLE_DURATION_PROP);
    }

    @Override
    public List<StorageTier> getCostAnalysisStorageTiers() {
        return config_.getObjectList(SIZER_COST_ANALYSIS_TIERS_PROP).stream()
                .map(ConfigObject::toConfig)
                .map(tierConfig -> new StorageTier.Builder()
                        .setName(tierConfig.getString(SIZER_COST_ANALYSIS_TIER_NAME_PROP))
                        .setDaysSinceLastAccess(tierConfig.getInt(SIZER_COST_ANALYSIS_TIER_DAYS_SINCE_LAST_ACCESS_PROP))
                        .setCostPerGbPerMonth(tierConfig.getDouble(SIZER_COST_ANALYSIS_TIER_COST_PER_GB_PER_MONTH_PROP))
                        .build())
                .sorted(Comparator.comparingInt(StorageTier::getDaysSinceLastAccess))
                .collect(ImmutableList.toImmutableList());
    }

}
