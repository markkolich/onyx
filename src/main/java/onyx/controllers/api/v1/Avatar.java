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

package onyx.controllers.api.v1;

import com.google.common.net.HttpHeaders;
import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.Path;
import curacao.core.servlet.AsyncContext;
import curacao.core.servlet.HttpResponse;
import onyx.components.avatar.AvatarManager;
import onyx.components.config.OnyxConfig;
import onyx.components.config.aws.AwsConfig;
import onyx.controllers.api.AbstractOnyxApiController;
import onyx.entities.authentication.Session;
import onyx.exceptions.resource.ResourceNotFoundException;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static curacao.annotations.RequestMapping.Method.GET;

@Controller
public final class Avatar extends AbstractOnyxApiController {

    private static final String DEFAULT_AVATAR_PATH = "/static/img/onyx/avatars/default.jpg";

    private static final String CACHE_CONTROL_PRIVATE_FORMAT = "private, max-age=%d";

    private final AwsConfig awsConfig_;
    private final AvatarManager avatarManager_;

    @Injectable
    public Avatar(
            final OnyxConfig onyxConfig,
            final AwsConfig awsConfig,
            final AvatarManager avatarManager) {
        super(onyxConfig);
        awsConfig_ = awsConfig;
        avatarManager_ = avatarManager;
    }

    @RequestMapping(value = "^/api/v1/avatar/(?<username>[a-zA-Z0-9]+)$",
            methods = GET)
    public void getAvatarForUser(
            @Path("username") final String username,
            final Session session,
            final HttpResponse response,
            final AsyncContext context) throws Exception {
        if (session == null) {
            throw new ResourceNotFoundException("User not authenticated.");
        }

        final long validitySeconds =
                awsConfig_.getAwsS3PresignedAssetUrlValidityDuration(TimeUnit.SECONDS);
        final String cacheControl = String.format(CACHE_CONTROL_PRIVATE_FORMAT, validitySeconds);
        response.setHeader(HttpHeaders.CACHE_CONTROL, cacheControl);

        final URL avatarUrl = avatarManager_.getPresignedAvatarUrlForUsername(username);
        if (avatarUrl != null) {
            response.sendRedirect(avatarUrl.toString());
        } else {
            response.sendRedirect(onyxConfig_.getViewSafeFullUri() + DEFAULT_AVATAR_PATH);
        }

        context.complete();
    }

}
