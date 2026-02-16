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

package onyx.components.authentication;

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.core.servlet.HttpResponse;
import onyx.components.config.OnyxConfig;
import onyx.components.config.authentication.SessionConfig;
import onyx.util.CookieBaker;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public final class OnyxCookieManager implements CookieManager {

    private final OnyxConfig onyxConfig_;
    private final SessionConfig sessionConfig_;

    @Injectable
    public OnyxCookieManager(
            final OnyxConfig onyxConfig,
            final SessionConfig sessionConfig) {
        onyxConfig_ = onyxConfig;
        sessionConfig_ = sessionConfig;
    }

    @Override
    public void setCookie(
            final String name,
            final String value,
            final HttpResponse response) {
        checkNotNull(name, "Cookie name cannot be null.");
        checkNotNull(value, "Cookie value cannot be null.");
        checkNotNull(response, "HTTP response cannot be null.");

        setCookie(name, value, null, response);
    }

    @Override
    public void setCookie(
            final String name,
            final String value,
            @Nullable final Integer maxAge,
            final HttpResponse response) {
        checkNotNull(name, "Cookie name cannot be null.");
        checkNotNull(value, "Cookie value cannot be null.");
        checkNotNull(response, "HTTP response cannot be null.");

        final CookieBaker.Builder builder = newCookieBuilder()
                .setName(name)
                .setValue(value);
        if (maxAge != null) {
            builder.setMaxAge(maxAge);
        }

        builder.build().bake(response);
    }

    @Override
    public void clearCookie(
            final String name,
            final HttpResponse response) {
        checkNotNull(name, "Cookie name cannot be null.");
        checkNotNull(response, "HTTP response cannot be null.");

        newCookieBuilder()
                .setName(name)
                .setMaxAge(0) // unset!
                .build()
                .bake(response);
    }

    private CookieBaker.Builder newCookieBuilder() {
        return new CookieBaker.Builder()
                .setDomain(sessionConfig_.getSessionDomain())
                .setContextPath(onyxConfig_.getContextPath())
                .setSecure(sessionConfig_.isSessionUsingHttps());
    }

}
