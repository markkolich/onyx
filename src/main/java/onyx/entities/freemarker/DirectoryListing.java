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

package onyx.entities.freemarker;

import com.google.common.collect.ImmutableList;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A helper POJO that represents of the contents of a directory, specifically decorated
 * with helper methods for use in FreeMarker templates.
 */
public interface DirectoryListing {

    List<Resource> getFavorites();

    List<Resource> getNonFavorites();

    List<Resource> getAll();

    long getDirectoryCount();

    long getFileCount();

    default Builder toBuilder() {
        return new Builder(getAll());
    }

    static DirectoryListing of() {
        return new Builder(ImmutableList.of()).build();
    }

    final class Builder {

        private final List<Resource> resources_;

        public Builder(
                final List<Resource> resources) {
            resources_ = checkNotNull(resources, "Resources list cannot be null.");
        }

        public DirectoryListing build() {
            return new DirectoryListing() {
                @Override
                public List<Resource> getFavorites() {
                    return resources_.stream()
                            .filter(Resource::getFavorite)
                            .collect(ImmutableList.toImmutableList());
                }

                @Override
                public List<Resource> getNonFavorites() {
                    return resources_.stream()
                            .filter(r -> !r.getFavorite())
                            .collect(ImmutableList.toImmutableList());
                }

                @Override
                public List<Resource> getAll() {
                    return resources_;
                }

                @Override
                public long getDirectoryCount() {
                    if (CollectionUtils.isEmpty(resources_)) {
                        return 0L;
                    }

                    return resources_.stream()
                            .filter(c -> Resource.Type.DIRECTORY.equals(c.getType()))
                            .count();
                }

                @Override
                public long getFileCount() {
                    if (CollectionUtils.isEmpty(resources_)) {
                        return 0L;
                    }

                    return resources_.stream()
                            .filter(c -> Resource.Type.FILE.equals(c.getType()))
                            .count();
                }
            };
        }

    }

}
