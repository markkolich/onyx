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

package onyx.controllers;

import onyx.components.authentication.SessionManager;
import onyx.components.authentication.UserAuthenticator;
import onyx.components.authentication.twofactor.TwoFactorAuthCodeManager;
import onyx.components.authentication.twofactor.TwoFactorAuthTokenManager;
import onyx.components.config.authentication.SessionConfig;
import onyx.components.config.authentication.twofactor.TwoFactorAuthConfig;
import onyx.components.storage.ResourceManager;
import onyx.entities.freemarker.FreeMarkerContent;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SessionTest extends AbstractOnyxControllerTest {

    public SessionTest() throws Exception {
    }

    @Test
    public void loginTest() throws Exception {
        final ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        final SessionConfig sessionConfig = Mockito.mock(SessionConfig.class);
        final SessionManager sessionManager = Mockito.mock(SessionManager.class);
        final UserAuthenticator userAuthenticator = Mockito.mock(UserAuthenticator.class);
        final TwoFactorAuthConfig twoFactorAuthConfig = Mockito.mock(TwoFactorAuthConfig.class);
        final TwoFactorAuthTokenManager twoFactorAuthTokenManager = Mockito.mock(TwoFactorAuthTokenManager.class);
        final TwoFactorAuthCodeManager twoFactorAuthCodeManager = Mockito.mock(TwoFactorAuthCodeManager.class);

        final Session controller = new Session(onyxConfig_, resourceManager, sessionConfig,
                sessionManager, userAuthenticator, twoFactorAuthConfig,
                twoFactorAuthTokenManager, twoFactorAuthCodeManager);

        final FreeMarkerContent responseEntity = controller.login();
        assertNotNull(responseEntity);

        final String renderedHtml = fmcRenderer_.contentToString(responseEntity);
        assertTrue(StringUtils.isNotBlank(renderedHtml));
    }

}
