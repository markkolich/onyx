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

package onyx.controllers;

import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.RequestBody;
import onyx.components.authentication.SessionManager;
import onyx.components.authentication.UserAuthenticator;
import onyx.components.config.OnyxConfig;
import onyx.entities.freemarker.FreeMarkerContent;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import static curacao.annotations.RequestMapping.Method.POST;
import static onyx.util.CookieBaker.setCookie;
import static onyx.util.CookieBaker.unsetCookie;

@Controller
public final class Session extends AbstractOnyxController {

    private static final String USERNAME_FIELD = "username";
    private static final String PASSWORD_FIELD = "password";

    private final UserAuthenticator userAuthenticator_;
    private final SessionManager sessionManager_;

    @Injectable
    public Session(
            final OnyxConfig onyxConfig,
            final UserAuthenticator userAuthenticator,
            final SessionManager sessionManager) {
        super(onyxConfig);
        userAuthenticator_ = userAuthenticator;
        sessionManager_ = sessionManager;
    }

    @RequestMapping(value = "^/login$")
    public FreeMarkerContent login() {
        return new FreeMarkerContent.Builder("templates/login.ftl")
                .build();
    }

    @RequestMapping(value = "^/login$", methods = POST)
    public FreeMarkerContent doLogin(
            @RequestBody(USERNAME_FIELD) final String username,
            @RequestBody(PASSWORD_FIELD) final String password,
            final HttpServletResponse response,
            final AsyncContext context) throws Exception {
        if (StringUtils.isBlank(username)
                || StringUtils.isBlank(password)) {
            return new FreeMarkerContent.Builder("templates/login.ftl")
                    .build();
        }

        final onyx.entities.Session session =
                userAuthenticator_.getSession(username, password);
        if (session == null) {
            return new FreeMarkerContent.Builder("templates/login.ftl")
                    .build();
        }

        final String signedSession = sessionManager_.signSession(session);
        setCookie(SessionManager.SESSION_NAME, signedSession,
                onyxConfig_.isSessionUsingHttps(), response);
        response.sendRedirect(onyxConfig_.getFullUri() + "browse/" + session.getUsername());
        context.complete();

        return null;
    }

    @RequestMapping(value = "^/logout$")
    public void logout(
            final HttpServletResponse response,
            final AsyncContext context) throws Exception {
        unsetCookie(SessionManager.SESSION_NAME, onyxConfig_.isSessionUsingHttps(), response);
        response.sendRedirect(onyxConfig_.getFullUri());
        context.complete();
    }

}
