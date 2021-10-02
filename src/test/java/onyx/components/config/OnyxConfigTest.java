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

package onyx.components.config;

import onyx.AbstractOnyxTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class OnyxConfigTest extends AbstractOnyxTest {

    public OnyxConfigTest() throws Exception {
    }

    @Test
    public void testGetViewSafeContentPathRootContext() {
        final OnyxConfig mockOnyxConfig = Mockito.mock(OnyxConfig.class);
        Mockito.when(mockOnyxConfig.getContextPath()).thenReturn("/");
        Mockito.when(mockOnyxConfig.getViewSafeContentPath()).thenCallRealMethod();

        assertEquals("/", mockOnyxConfig.getContextPath());
        assertEquals("", mockOnyxConfig.getViewSafeContentPath());
    }

    @Test
    public void testGetViewSafeContentPathOtherContext() {
        final OnyxConfig mockOnyxConfig = Mockito.mock(OnyxConfig.class);
        Mockito.when(mockOnyxConfig.getContextPath()).thenReturn("/foobar");
        Mockito.when(mockOnyxConfig.getViewSafeContentPath()).thenCallRealMethod();

        assertEquals("/foobar", mockOnyxConfig.getContextPath());
        assertEquals("/foobar", mockOnyxConfig.getViewSafeContentPath());
    }

    @Test
    public void testGetViewSafeFullUriRootContext() {
        final OnyxConfig mockOnyxConfig = Mockito.mock(OnyxConfig.class);
        Mockito.when(mockOnyxConfig.getFullUri()).thenReturn("http://localhost/");
        Mockito.when(mockOnyxConfig.getViewSafeFullUri()).thenCallRealMethod();

        assertEquals("http://localhost/", mockOnyxConfig.getFullUri());
        assertEquals("http://localhost", mockOnyxConfig.getViewSafeFullUri());
    }

    @Test
    public void testGetViewSafeFullUriOtherContext() {
        final OnyxConfig mockOnyxConfig = Mockito.mock(OnyxConfig.class);
        Mockito.when(mockOnyxConfig.getFullUri()).thenReturn("http://localhost/foobar/");
        Mockito.when(mockOnyxConfig.getViewSafeFullUri()).thenCallRealMethod();

        assertEquals("http://localhost/foobar/", mockOnyxConfig.getFullUri());
        assertEquals("http://localhost/foobar", mockOnyxConfig.getViewSafeFullUri());
    }

}
