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

package onyx.components.storage.filter;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.OnyxConfig;

import java.util.List;

@Component
public final class OnyxFilterConfig implements FilterConfig {

    private final Config config_;

    @Injectable
    public OnyxFilterConfig(
            final OnyxConfig onyxConfig) {
        config_ = onyxConfig.getOnyxConfig().getConfig(FILTER_CONFIG_PATH);
    }

    @Override
    public List<String> getExcludes() {
        final ConfigList excludes = config_.getList(EXCLUDES_PROP);

        final ImmutableList.Builder<String> excludesListBuilder =
                ImmutableList.builder();
        for (final ConfigValue exclude : excludes) {
            if (!ConfigValueType.STRING.equals(exclude.valueType())) {
                continue;
            }

            // cast intentional, and safe
            excludesListBuilder.add((String) exclude.unwrapped());
        }

        return excludesListBuilder.build();
    }

}
