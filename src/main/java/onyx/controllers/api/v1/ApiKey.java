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
import com.google.common.collect.ImmutableList;
import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.parameters.Path;
import curacao.annotations.parameters.RequestBody;
import curacao.entities.empty.StatusCodeOnlyCuracaoEntity;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.authentication.api.ApiKeyManager;
import onyx.components.config.OnyxConfig;
import onyx.controllers.api.AbstractOnyxApiController;
import onyx.entities.api.request.v1.CreateApiKeyRequest;
import onyx.entities.api.response.v1.ApiKeyResponse;
import onyx.entities.api.response.v1.CreateApiKeyResponse;
import onyx.entities.api.response.v1.ListApiKeysResponse;
import onyx.entities.authentication.ApiKeyCredential;
import onyx.entities.authentication.Session;
import onyx.exceptions.api.ApiNotFoundException;
import onyx.exceptions.api.ApiUnauthorizedException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

import static curacao.annotations.RequestMapping.Method.DELETE;
import static curacao.annotations.RequestMapping.Method.GET;
import static curacao.annotations.RequestMapping.Method.POST;

@Controller
public final class ApiKey extends AbstractOnyxApiController {

    private final ApiKeyManager apiKeyManager_;

    private final ObjectMapper objectMapper_;

    @Injectable
    public ApiKey(
            final OnyxConfig onyxConfig,
            final ApiKeyManager apiKeyManager,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper) {
        super(onyxConfig);
        apiKeyManager_ = apiKeyManager;
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();
    }

    @RequestMapping(value = "^/api/v1/apikey$",
            methods = POST)
    public CreateApiKeyResponse createApiKey(
            @RequestBody final CreateApiKeyRequest request,
            final Session session) {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        }

        final String username = session.getUsername();

        final Pair<String, ApiKeyCredential> result =
                apiKeyManager_.createApiKeyForUsername(username, request.getDescription());
        final ApiKeyCredential credential = result.getRight();

        return new CreateApiKeyResponse.Builder(objectMapper_)
                .setApiKey(result.getLeft())
                .setKeyHash(credential.getKeyHash())
                .setDescription(credential.getDescription())
                .setCreatedAt(credential.getCreatedAt())
                .build();
    }

    @RequestMapping(value = "^/api/v1/apikey$",
            methods = GET)
    public ListApiKeysResponse listApiKeys(
            final Session session) {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        }

        final List<ApiKeyCredential> credentials = apiKeyManager_.getApiKeysForUsername(session.getUsername());

        final List<ApiKeyResponse> keys = credentials.stream()
                .map(credential -> ApiKeyResponse.Builder.fromCredential(objectMapper_, credential).build())
                .collect(ImmutableList.toImmutableList());

        return new ListApiKeysResponse.Builder(objectMapper_)
                .setKeys(keys)
                .build();
    }

    @RequestMapping(value = "^/api/v1/apikey/(?<keyHash>[a-f0-9]{64})$",
            methods = DELETE)
    public StatusCodeOnlyCuracaoEntity deleteApiKey(
            @Path("keyHash") final String keyHash,
            final Session session) {
        if (session == null) {
            throw new ApiUnauthorizedException("User not authenticated.");
        }

        final boolean deleted = apiKeyManager_.deleteApiKeyForUsername(session.getUsername(), keyHash);
        if (!deleted) {
            throw new ApiNotFoundException("Found no API key for hash: " + keyHash);
        }

        return noContent();
    }

}
