/*
 * Copyright (c) 2023 Mark S. Kolich
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

import onyx.AbstractOnyxTest;
import onyx.components.FreeMarkerContentRenderer;
import onyx.components.OnyxFreeMarkerConfig;
import onyx.entities.authentication.Session;
import onyx.entities.authentication.Session.Type;

import java.time.Instant;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractOnyxControllerTest extends AbstractOnyxTest {

    protected final OnyxFreeMarkerConfig onyxFreeMarkerConfig_;

    protected final FreeMarkerContentRenderer fmcRenderer_;

    public AbstractOnyxControllerTest() throws Exception {
        onyxFreeMarkerConfig_ = new OnyxFreeMarkerConfig(servletContext_);

        fmcRenderer_ = new FreeMarkerContentRenderer(onyxConfig_, onyxFreeMarkerConfig_);
    }

    protected static Session generateNewSession(
            final String username) {
        checkNotNull(username, "Username cannot be null.");

        final Instant sessionExpiry = Instant.now().plusSeconds(60);
        final Instant refreshAfter = Instant.now().plusSeconds(60);

        return generateNewSession(username, sessionExpiry, refreshAfter);
    }

    protected static Session generateNewSession(
            final String username,
            final Instant expiry,
            final Instant refreshAfter) {
        checkNotNull(username, "Username cannot be null.");
        checkNotNull(expiry, "Expiry instant cannot be null.");
        checkNotNull(refreshAfter, "Refresh after instant cannot be null.");

        return new Session.Builder()
                .setId(UUID.randomUUID().toString())
                .setType(Type.USER)
                .setUsername(username)
                .setExpiry(expiry)
                .setRefreshAfter(refreshAfter)
                .build();
    }

}
