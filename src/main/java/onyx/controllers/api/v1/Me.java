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

import com.fasterxml.jackson.databind.ObjectMapper;
import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.config.OnyxConfig;
import onyx.controllers.api.AbstractOnyxApiController;
import onyx.entities.api.response.v1.MeResponse;
import onyx.entities.authentication.Session;
import onyx.exceptions.api.ApiUnauthorizedException;

import static curacao.annotations.RequestMapping.Method.GET;

/**
 * Reports who the caller is, given whatever credential they presented &mdash; the
 * conventional "who am I" endpoint.
 *
 * <p>Exists primarily so a credential can be validated without side effects.
 */
@Controller
public final class Me extends AbstractOnyxApiController {

    private final ObjectMapper objectMapper_;

    @Injectable
    public Me(
            final OnyxConfig onyxConfig,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper) {
        super(onyxConfig);
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();
    }

    @RequestMapping(value = "^/api/v1/me$",
            methods = GET)
    public MeResponse me(
            final Session session) {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        }

        return MeResponse.Builder.fromSession(objectMapper_, session)
                .build();
    }

}
