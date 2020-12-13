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

import static com.google.common.base.Preconditions.checkNotNull;

public final class CookieBaker {

    private final String name_;
    private final String value_;

    private final String domain_;
    private final String contextPath_;

    private final Integer maxAge_;

    private final Boolean isSecure_;

    private CookieBaker(
            final String name,
            @Nullable final String value,
            @Nullable final String domain,
            @Nullable final String contextPath,
            @Nullable final Integer maxAge,
            @Nullable final Boolean isSecure) {
        name_ = checkNotNull(name, "Cookie name cannot be null.");
        value_ = value;

        domain_ = domain;
        contextPath_ = contextPath;

        maxAge_ = maxAge;
        isSecure_ = isSecure;
    }

    public void bake(
            final HttpServletResponse response) {
        checkNotNull(response, "HTTP response cannot be null.");

        final Cookie cookie = new Cookie(name_, value_);
        cookie.setHttpOnly(true);

        if (maxAge_ != null) {
            if (maxAge_ <= 0) {
                cookie.setMaxAge(0);
            } else {
                cookie.setMaxAge(maxAge_);
            }
        }

        if (domain_ != null) {
            cookie.setDomain(domain_);
        }
        if (contextPath_ != null) {
            cookie.setPath(contextPath_);
        }
        if (isSecure_ != null) {
            cookie.setSecure(isSecure_);
        }

        // Attach the cookie to the Servlet response.
        response.addCookie(cookie);
    }

    public static final class Builder {

        private String name_;
        private String value_;

        private String domain_;
        private String contextPath_;

        private Integer maxAge_;

        private Boolean isSecure_;

        public Builder setName(
                final String name) {
            name_ = checkNotNull(name, "Cookie name cannot be null.");
            return this;
        }

        public Builder setValue(
                @Nullable final String value) {
            value_ = value;
            return this;
        }

        public Builder setDomain(
                @Nullable final String domain) {
            domain_ = domain;
            return this;
        }

        public Builder setContextPath(
                @Nullable final String contextPath) {
            contextPath_ = contextPath;
            return this;
        }

        public Builder setMaxAge(
                @Nullable final Integer maxAge) {
            maxAge_ = maxAge;
            return this;
        }

        public Builder setSecure(
                final Boolean isSecure) {
            isSecure_ = isSecure;
            return this;
        }

        public CookieBaker build() {
            checkNotNull(name_, "Cookie name cannot be null.");

            return new CookieBaker(name_, value_, domain_, contextPath_,
                    maxAge_, isSecure_);
        }

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
