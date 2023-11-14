/*
 * Copyright (c) 2024 Mark S. Kolich
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

package onyx.components;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import freemarker.template.Configuration;
import freemarker.template.Template;
import onyx.BuildVersion;
import onyx.components.config.OnyxConfig;
import onyx.entities.freemarker.FreeMarkerContent;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

@Component
public final class FreeMarkerContentRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(FreeMarkerContentRenderer.class);

    private static final String BUILD_VERSION_ATTR = "buildVersion";
    private static final String CONTEXT_PATH_ATTR = "contextPath";
    private static final String FULL_URI_ATTR = "fullUri";
    private static final String DEV_MODE_ATTR = "devMode";

    private final OnyxConfig onyxConfig_;
    private final OnyxFreeMarkerConfig onyxFreeMarkerConfig_;

    @Injectable
    public FreeMarkerContentRenderer(
            final OnyxConfig onyxConfig,
            final OnyxFreeMarkerConfig onyxFreeMarkerConfig) throws Exception {
        onyxConfig_ = onyxConfig;
        onyxFreeMarkerConfig_ = onyxFreeMarkerConfig;
    }

    public String contentToString(
            final FreeMarkerContent content) throws Exception {
        try (Writer w = contentToWriter(content)) {
            return w.toString();
        }
    }

    public Writer contentToWriter(
            final FreeMarkerContent content) throws Exception {
        final Writer w = new StringWriter();
        try {
            final Configuration freeMarkerConfig = onyxFreeMarkerConfig_.getFreeMarkerConfig();
            final Template tp = freeMarkerConfig.getTemplate(content.getTemplateName());
            tp.process(getMergedTemplateDataMap(tp, content), w);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to process free marker template into string.", e);
        }
        return w;
    }

    public Map<String, Object> getGlobalDataMap() {
        final ImmutableMap.Builder<String, Object> map = ImmutableMap.builder();

        // Application build version
        final BuildVersion buildVersion = BuildVersion.getInstance();
        map.put(BUILD_VERSION_ATTR, buildVersion);

        // Shared application/service properties
        map.put(CONTEXT_PATH_ATTR, onyxConfig_.getViewSafeContentPath());
        map.put(FULL_URI_ATTR, onyxConfig_.getViewSafeFullUri());
        map.put(DEV_MODE_ATTR, onyxConfig_.isDevMode());

        return map.build();
    }

    private Map<String, Object> getMergedTemplateDataMap(
            final Template tp,
            final FreeMarkerContent content) {
        // The resulting map is intentionally not immutable here because it's
        // mutated during template processing.
        final Map<String, Object> map = Maps.newLinkedHashMap();
        // Merge in the web-application specific data map.
        map.putAll(getGlobalDataMap());
        // Get the entity defined set of template attributes.
        final Map<String, Object> templateDataMap = content.getDataMap();
        if (MapUtils.isNotEmpty(templateDataMap)) {
            map.putAll(templateDataMap);
        }
        // Get the template defined set of attributes (these are defined
        // inline within the .ftl template file itself).
        for (final String attrName : tp.getCustomAttributeNames()) {
            map.put(attrName, tp.getCustomAttribute(attrName));
        }
        return ImmutableMap.copyOf(map);
    }

}
