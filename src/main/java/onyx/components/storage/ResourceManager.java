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

package onyx.components.storage;

import onyx.entities.storage.aws.dynamodb.Resource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public interface ResourceManager {

    String ROOT_PATH = "/";

    final class Extensions {

        public enum Sort {
            NONE,
            FAVORITE;
        }

    }

    @Nullable
    Resource getResourceAtPath(
            final String path);

    void createResource(
            final Resource resource);

    void updateResource(
            final Resource resource);

    void deleteResource(
            final Resource resource);

    void deleteResourceAsync(
            final Resource resource,
            final ExecutorService executorService);

    @Nonnull
    List<Resource> listDirectory(
            final Resource directory,
            final Set<Resource.Visibility> visibility,
            @Nullable final Extensions.Sort sort);

    @Nonnull
    List<Resource> listHomeDirectories();

}
