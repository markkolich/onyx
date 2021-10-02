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

package onyx.entities.freemarker;

import com.google.common.collect.ImmutableMap;
import onyx.entities.authentication.Session;
import onyx.entities.freemarker.Utf8TextEntity.EntityType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static javax.servlet.http.HttpServletResponse.SC_OK;

public interface FreeMarkerContent {

    String DATA_MAP_SESSION_ATTR = "session";

    String getTemplateName();

    EntityType getEntityType();

    int getStatus();

    @Nonnull
    default Map<String, Object> getDataMap() {
        return ImmutableMap.of();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    default <T> T getAttribute(
            final String name) {
        checkNotNull(name, "Attribute name cannot be null.");

        return (T) getDataMap().get(name);
    }

    final class Builder {

        private final String templateName_;
        private final EntityType entityType_;
        private final int status_;

        /**
         * Note, {@link ImmutableMap.Builder}'s do not support multiple calls to
         * {@link ImmutableMap.Builder#put(Object, Object)} with the same key. If the consumer/caller
         * of this builder invokes <code>put(x, y);</code> followed by a <code>put(x, z);</code> the
         * internal call to {@link ImmutableMap.Builder#build()} will fail hard at builder-build time
         * because of the duplicate keys. This is expected and avoids bugs/mistakes in creating the
         * internal data map.
         */
        private final ImmutableMap.Builder<String, Object> dataMap_ = ImmutableMap.builder();

        public Builder(
                final String templateName,
                final EntityType entityType,
                final int status) {
            templateName_ = checkNotNull(templateName, "Template name cannot be null.");
            entityType_ = checkNotNull(entityType, "Entity type cannot be null.");
            checkState(status >= SC_OK, "HTTP response status attached to template cannot be < " + SC_OK);
            status_ = status;
        }

        public Builder(
                final String templateName,
                final int status) {
            this(templateName, EntityType.HTML, status);
        }

        public Builder(
                final String templateName,
                final EntityType entityType) {
            this(templateName, entityType, SC_OK);
        }

        public Builder(
                final String templateName) {
            this(templateName, SC_OK);
        }

        public Builder withSession(
                @Nullable final Session session) {
            return withAttr(DATA_MAP_SESSION_ATTR, session);
        }

        public Builder withAttr(
                @Nullable final String name,
                @Nullable final Object value) {
            if (name != null && value != null) {
                dataMap_.put(name, value);
            }
            return this;
        }

        public FreeMarkerContent build() {
            return new FreeMarkerContent() {
                @Override
                public String getTemplateName() {
                    return templateName_;
                }

                @Override
                public EntityType getEntityType() {
                    return entityType_;
                }

                @Override
                public int getStatus() {
                    return status_;
                }

                @Nonnull
                @Override
                public Map<String, Object> getDataMap() {
                    return dataMap_.build();
                }
            };
        }

    }

}
