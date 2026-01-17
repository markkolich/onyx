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

package onyx.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import onyx.components.storage.ResourceManager;
import onyx.entities.authentication.Session;
import onyx.entities.freemarker.FreeMarkerContent;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IndexTest extends AbstractOnyxControllerTest {

    public IndexTest() throws Exception {
    }

    @Test
    public void indexTest() throws Exception {
        final List<Resource> homeDirectories =
                resourceJsonToObject("mock/index.json", new TypeReference<>() {});

        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        Mockito.when(resourceManager.listHomeDirectories()).thenReturn(homeDirectories);

        final Index controller = new Index(onyxConfig_, resourceManager);

        final Session session = generateNewSession("foobar");
        final FreeMarkerContent responseEntity = controller.index(session);
        assertNotNull(responseEntity);

        final String renderedHtml = fmcRenderer_.contentToString(responseEntity);
        assertTrue(StringUtils.isNotBlank(renderedHtml));
    }

}
