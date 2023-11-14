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

package onyx.controllers;

import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import onyx.components.config.OnyxConfig;
import onyx.components.storage.ResourceManager;
import onyx.entities.authentication.Session;
import onyx.entities.freemarker.FreeMarkerContent;
import onyx.entities.storage.aws.dynamodb.Resource;

import java.util.List;

import static onyx.entities.freemarker.Utf8TextEntity.EntityType.TEXT;
import static onyx.entities.freemarker.Utf8TextEntity.EntityType.XML;

@Controller
public final class Index extends AbstractOnyxFreeMarkerController {

    @Injectable
    public Index(
            final OnyxConfig onyxConfig,
            final ResourceManager resourceManager) {
        super(onyxConfig, resourceManager);
    }

    @RequestMapping(value = "^/$")
    public FreeMarkerContent index(
            final Session session) {
        final List<Resource> homeDirectories = resourceManager_.listHomeDirectories();

        return new FreeMarkerContent.Builder("templates/index.ftl")
                .withSession(session)
                .withAttr("children", homeDirectories)
                .build();
    }

    @RequestMapping("^/robots\\.txt$")
    public FreeMarkerContent robots() {
        return new FreeMarkerContent.Builder("templates/txt/robots.ftl", TEXT).build();
    }

    @RequestMapping("^/sitemap\\.xml$")
    public FreeMarkerContent sitemap() {
        final List<Resource> homeDirectories = resourceManager_.listHomeDirectories();

        return new FreeMarkerContent.Builder("templates/xml/sitemap.ftl", XML)
                .withAttr("children", homeDirectories)
                .build();
    }

}
