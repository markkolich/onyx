/*
 * Copyright (c) 2021 Mark S. Kolich
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

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import freemarker.cache.TemplateLookupContext;
import freemarker.cache.TemplateLookupResult;
import freemarker.cache.TemplateLookupStrategy;
import freemarker.cache.URLTemplateLoader;
import freemarker.template.Configuration;
import onyx.Application;
import onyx.exceptions.OnyxException;
import org.eclipse.jetty.util.resource.Resource;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Component
public final class OnyxFreeMarkerConfig {

    private final Configuration freemarkerConfig_;

    @Injectable
    public OnyxFreeMarkerConfig(
            final ServletContext servletContext) {
        final Resource baseResource =
                (Resource) servletContext.getAttribute(Application.CONTEXT_ATTRIBUTE_BASE_RESOURCE);

        freemarkerConfig_ = new Configuration(Configuration.VERSION_2_3_30);
        freemarkerConfig_.setDefaultEncoding(StandardCharsets.UTF_8.toString());
        freemarkerConfig_.setTemplateLookupStrategy(new TemplateLookupStrategy() {
            @Override
            public TemplateLookupResult lookup(
                    final TemplateLookupContext ctx) throws IOException {
                // Override the template lookup strategy to exclude the locale on
                // any resolved templates.
                return ctx.lookupWithAcquisitionStrategy(ctx.getTemplateName());
            }
        });
        freemarkerConfig_.setTemplateLoader(new URLTemplateLoader() {
            @Override
            protected URL getURL(
                    final String name) {
                try {
                    final String baseResourceUri = baseResource.getURI().toString();
                    return URI.create(baseResourceUri + ((baseResourceUri.endsWith("/")) ? name : "/" + name))
                            .toURL();
                } catch (final Exception e) {
                    throw new OnyxException("Failed to load template resource: " + name, e);
                }
            }
        });
    }

    public Configuration getFreeMarkerConfig() {
        return freemarkerConfig_;
    }

}
