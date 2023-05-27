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

package onyx.components.storage.reaper;

import java.time.Duration;

public interface ReaperConfig {

    String REAPER_CONFIG_PATH = "reaper";

    String REAPER_RUN_ON_APP_STARTUP_PROP = "run-on-app-startup";
    String REAPER_RUN_ON_SCHEDULE_PROP = "run-on-schedule";
    String REAPER_RUN_CRON_EXPRESSION_PROP = "run-cron-expression";

    String REAPER_BACKOFF_MAX_RETRIES_PROP = "backoff-max-retries";
    String REAPER_BACKOFF_THROTTLE_DURATION_PROP = "backoff-throttle-duration";

    String REAPER_ITERATION_THROTTLE_DURATION_PROP = "iteration-throttle-duration";

    boolean getReaperRunOnAppStartup();

    boolean getReaperRunOnSchedule();

    String getReaperRunCronExpression();

    int getBackoffMaxRetries();

    Duration getBackoffThrottleDuration();

    Duration getIterationThrottleDuration();

}
