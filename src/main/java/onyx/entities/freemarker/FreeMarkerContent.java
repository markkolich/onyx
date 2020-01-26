/*
 * Copyright (c) 2020 Mark S. Kolich
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

package onyx.entities.freemarker;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import onyx.entities.authentication.Session;
import onyx.entities.freemarker.Utf8TextEntity.HtmlEntityType;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static javax.servlet.http.HttpServletResponse.SC_OK;

public final class FreeMarkerContent extends AbstractFreeMarkerContent {

    private static final String DATA_MAP_SESSION_ATTR = "session";

    private final String templateName_;
    private final HtmlEntityType entityType_;
    private final int status_;

    private final Map<String, Object> dataMap_;

    private FreeMarkerContent(
            final String templateName,
            final HtmlEntityType entityType,
            final int status) {
        templateName_ = checkNotNull(templateName, "Template name cannot be null.");
        entityType_ = checkNotNull(entityType, "Entity type cannot be null.");
        checkState(status >= SC_OK, "HTTP response status attached to template content cannot be < " + SC_OK);
        status_ = status;
        dataMap_ = Maps.newLinkedHashMap();
    }

    @Override
    public String getTemplateName() {
        return templateName_;
    }

    @Override
    public int getStatus() {
        return status_;
    }

    @Override
    public HtmlEntityType getEntityType() {
        return entityType_;
    }

    @Nullable
    @Override
    public Map<String, Object> getDataMap() {
        return ImmutableMap.copyOf(dataMap_);
    }

    public static final class Builder {

        private final FreeMarkerContent content_;

        public Builder(
                final String templateName,
                final HtmlEntityType entityType,
                final int status) {
            content_ = new FreeMarkerContent(templateName, entityType, status);
        }

        public Builder(
                final String templateName,
                final int status) {
            content_ = new FreeMarkerContent(templateName, HtmlEntityType.HTML, status);
        }

        public Builder(
                final String templateName,
                final HtmlEntityType entityType) {
            content_ = new FreeMarkerContent(templateName, entityType, SC_OK);
        }

        public Builder(
                final String templateName) {
            this(templateName, SC_OK);
        }

        public final Builder withAttr(
                @Nullable final String name,
                @Nullable final Object value) {
            if (name != null && value != null) {
                content_.dataMap_.put(name, value);
            }
            return this;
        }

        public final Builder withSession(
                @Nullable final Session session) {
            if (session != null) {
                content_.dataMap_.put(DATA_MAP_SESSION_ATTR, session);
            }
            return this;
        }

        public final FreeMarkerContent build() {
            return content_;
        }

    }

}
