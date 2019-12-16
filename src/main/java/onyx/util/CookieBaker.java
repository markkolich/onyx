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

package onyx.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static com.google.common.primitives.Ints.checkedCast;

public final class CookieBaker {

    // Cannot instantiate
    private CookieBaker() {
    }

    public static void setCookie(
            final String cookieName,
            final String value,
            @Nullable final Long validityMs,
            @Nullable final String contextPath,
            final boolean isSecure,
            final HttpServletResponse response) {
        final Cookie cookie = new Cookie(cookieName, value);
        cookie.setHttpOnly(true);
        if (validityMs != null) {
            // Sigh -- "safely" convert the token validity from a long to int.
            // The max-age of a cookie is specified in seconds.
            cookie.setMaxAge(checkedCast(validityMs / 1000L));
        }
        if (contextPath != null) {
            cookie.setPath(contextPath);
        }
        cookie.setSecure(isSecure);
        // Attach the cookie to the Servlet response.
        response.addCookie(cookie);
    }

    public static void setCookie(
            final String cookieName,
            final String value,
            @Nullable final String contextPath,
            final boolean isSecure,
            final HttpServletResponse response) {
        setCookie(cookieName, value, null, contextPath, isSecure, response);
    }

    public static void setCookie(
            final String cookieName,
            final String value,
            final boolean isSecure,
            final HttpServletResponse response) {
        setCookie(cookieName, value, null, null, isSecure, response);
    }

    public static void unsetCookie(
            final String cookieName,
            @Nullable final String contextPath,
            final boolean isSecure,
            final HttpServletResponse response) {
        final Cookie cookie = new Cookie(cookieName, null);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // Tells the browser to forget this cookie.
        if (contextPath != null) {
            cookie.setPath(contextPath);
        }
        cookie.setSecure(isSecure);
        // Attach the cookie to the Servlet response.
        response.addCookie(cookie);
    }

    public static void unsetCookie(
            final String cookieName,
            final boolean isSecure,
            final HttpServletResponse response) {
        unsetCookie(cookieName, null, isSecure, response);
    }

    @Nonnull
    public static List<Cookie> getCookiesByName(
            final HttpServletRequest request,
            final String cookieName) {
        return getCookiesByName(request.getCookies(), cookieName);
    }

    @Nonnull
    public static List<Cookie> getCookiesByName(
            final Cookie[] cookies,
            final String cookieName) {
        final ImmutableList.Builder<Cookie> result = ImmutableList.builder();
        if (cookies != null) {
            for (final Cookie c : cookies) {
                if (cookieName.equals(c.getName())) {
                    result.add(c);
                }
            }
        }
        return result.build();
    }

    @Nullable
    public static Cookie getFirstCookieByName(
            final HttpServletRequest request,
            final String cookieName) {
        return getFirstCookieByName(request.getCookies(), cookieName);
    }

    @Nullable
    public static Cookie getFirstCookieByName(
            final Cookie[] cookies,
            final String cookieName) {
        return Iterables.getFirst(getCookiesByName(cookies, cookieName), null);
    }

}
