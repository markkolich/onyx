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

package onyx.components.search;

import onyx.entities.storage.aws.dynamodb.Resource;

import java.util.Collection;
import java.util.List;

public interface SearchManager {

    String INDEX_FIELD_PATH = "path";
    String INDEX_FIELD_PATH_LOWER = "pathLower";
    String INDEX_FIELD_PARENT = "parent";
    String INDEX_FIELD_DESCRIPTION = "description";
    String INDEX_FIELD_DESCRIPTION_LOWER = "descriptionLower";
    String INDEX_FIELD_SIZE = "size";
    String INDEX_FIELD_TYPE = "type";
    String INDEX_FIELD_VISIBILITY = "visibility";
    String INDEX_FIELD_OWNER = "owner";
    String INDEX_FIELD_CREATED = "created";
    String INDEX_FIELD_FAVORITE = "favorite";

    // Derived fields

    String INDEX_FIELD_NAME = "name";
    String INDEX_FIELD_NAME_LOWER = "nameLower";

    // Query fields

    String QUERY_FIELD_SCORE = "score";

    void addResourceToIndex(
            final Resource resource);

    void addResourcesToIndex(
            final Collection<Resource> resources);

    void deleteResourceFromIndex(
            final Resource resource);

    void deleteResourcesFromIndex(
            final Collection<Resource> resources);

    void deleteIndex();

    List<Resource> searchIndex(
            final String owner,
            final String query);

}
