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

package onyx.components.quartz;

import com.typesafe.config.Config;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.OnyxConfig;

@Component
public final class OnyxQuartzConfig implements QuartzConfig {

    private final Config config_;

    @Injectable
    public OnyxQuartzConfig(
            final OnyxConfig onyxConfig) {
        config_ = onyxConfig.getOnyxConfig().getConfig(QUARTZ_CONFIG_PATH);
    }

    @Override
    public int getThreadPoolSize() {
        return config_.getInt(THREAD_POOL_SIZE_PROP);
    }

    @Override
    public boolean getUseDaemonThreads() {
        return config_.getBoolean(THREAD_POOL_USE_DAEMON_THREADS_PROP);
    }

}
