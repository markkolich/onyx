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

package onyx.entities.storage.aws.dynamodb;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class ResourceTest {

    @Test
    public void getFavoriteOwnerIsNullWhenNotFavoriteTest() {
        final Resource resource = newResourceBuilder()
                .setFavorite(false)
                .build();

        assertNull(resource.getFavoriteOwner());
    }

    @Test
    public void getFavoriteOwnerEqualsOwnerWhenFavoriteTest() {
        final Resource resource = newResourceBuilder()
                .setFavorite(true)
                .build();

        assertEquals("foobar", resource.getFavoriteOwner());
    }

    @Test
    public void getFavoriteOwnerIsNullAgainAfterUnfavoritingTest() {
        final Resource resource = newResourceBuilder()
                .setFavorite(true)
                .build();
        assertEquals("foobar", resource.getFavoriteOwner());

        resource.setFavorite(false);

        assertNull(resource.getFavoriteOwner());
    }

    private static Resource.Builder newResourceBuilder() {
        return new Resource.Builder()
                .setPath("/foobar/baz")
                .setParent("/foobar")
                .setSize(0L)
                .setDescription("Baz")
                .setType(Resource.Type.FILE)
                .setVisibility(Resource.Visibility.PUBLIC)
                .setOwner("foobar")
                .setCreatedAt(Instant.now())
                .setCost(BigDecimal.ZERO);
    }

}
