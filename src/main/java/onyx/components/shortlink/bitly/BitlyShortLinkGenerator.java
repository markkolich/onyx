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

package onyx.components.shortlink.bitly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.BuildVersion;
import onyx.components.OnyxJacksonObjectMapper;
import onyx.components.config.OnyxConfig;
import onyx.components.shortlink.ShortLinkGenerator;
import onyx.entities.shortlink.bitly.request.BitlyShortenUrlRequest;
import onyx.entities.shortlink.bitly.response.BitlyShortenUrlResponse;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.OnyxException;
import org.asynchttpclient.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.asynchttpclient.Dsl.asyncHttpClient;

@Component
public final class BitlyShortLinkGenerator implements ShortLinkGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(BitlyShortLinkGenerator.class);

    private static final String USER_AGENT_FORMAT = "Onyx/%s (+%s)";
    private static final String AUTHORIZATION_HEADER_FORMAT = "Bearer %s";

    private static final String V4_SHORTEN = "/v4/shorten";

    private static final String JSON_UTF_8 = MediaType.JSON_UTF_8.toString();

    private static final String FILE_RESOURCE_PATH_FORMAT = "%s/file%s";
    private static final String BROWSE_RESOURCE_PATH_FORMAT = "%s/browse%s";

    private final OnyxConfig onyxConfig_;

    private final BitlyShortLinkGeneratorConfig shortLinkGeneratorConfig_;

    private final AsyncHttpClientConfig asyncHttpClientConfig_;

    private final ObjectMapper objectMapper_;

    @Injectable
    public BitlyShortLinkGenerator(
            final OnyxConfig onyxConfig,
            final BitlyShortLinkGeneratorConfig shortLinkGeneratorConfig,
            final OnyxJacksonObjectMapper onyxJacksonObjectMapper) {
        onyxConfig_ = onyxConfig;
        shortLinkGeneratorConfig_ = shortLinkGeneratorConfig;
        objectMapper_ = onyxJacksonObjectMapper.getObjectMapper();

        final BuildVersion buildVersion = BuildVersion.getInstance();

        final String userAgent = String.format(USER_AGENT_FORMAT,
                buildVersion.getBuildNumber(), shortLinkGeneratorConfig_.getVisibleBaseAppUrl());

        asyncHttpClientConfig_ = new DefaultAsyncHttpClientConfig.Builder()
                .setUserAgent(userAgent)
                .build();
    }

    @Override
    public URL createShortLinkForResource(
            final Resource resource) {
        checkNotNull(resource, "Resource cannot be null.");

        try (AsyncHttpClient asyncHttpClient = asyncHttpClient(asyncHttpClientConfig_)) {
            final String longUrlForResource = getLongUrlForResource(resource);
            final BitlyShortenUrlRequest bitlyShortenUrlRequest = new BitlyShortenUrlRequest.Builder()
                    .setLongUrl(longUrlForResource)
                    .build();

            final String apiBaseUrl = shortLinkGeneratorConfig_.getApiBaseUrl();
            final String shortenApiUrl = String.format("%s%s", apiBaseUrl, V4_SHORTEN);

            final String apiAccessToken = shortLinkGeneratorConfig_.getApiAccessToken();
            final String authorizationHeader = String.format(AUTHORIZATION_HEADER_FORMAT, apiAccessToken);

            final String shortenUrlRequestBody =
                    objectMapper_.writeValueAsString(bitlyShortenUrlRequest);

            final ListenableFuture<Response> futureResponse = asyncHttpClient.preparePost(shortenApiUrl)
                    .setHeader(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .setHeader(HttpHeaders.CONTENT_TYPE, JSON_UTF_8)
                    .setBody(shortenUrlRequestBody)
                    .execute();

            final long apiClientTimeoutInMs =
                    shortLinkGeneratorConfig_.getApiClientTimeout(TimeUnit.MILLISECONDS);

            final Response response = futureResponse.get(apiClientTimeoutInMs, TimeUnit.MILLISECONDS);
            final int statusCode = response.getStatusCode();
            if (statusCode == HttpServletResponse.SC_OK) {
                LOG.debug("Using existing bit.ly link for resource: {}", resource.getPath());
            } else if (statusCode == HttpServletResponse.SC_CREATED) {
                LOG.debug("Created new bit.ly link for resource: {}", resource.getPath());
            } else {
                throw new OnyxException(String.format("Unsuccessful status code from bit.ly API: %s",
                        response.getStatusCode()));
            }

            final String responseBody = response.getResponseBody(StandardCharsets.UTF_8);
            final BitlyShortenUrlResponse responseEntity =
                    objectMapper_.readValue(responseBody, BitlyShortenUrlResponse.class);

            return new URL(responseEntity.getLink());
        } catch (final Exception e) {
            LOG.error("Failed to generate bit.ly short link for resource: {}",
                    resource.getPath(), e);
            return null;
        }
    }

    private String getLongUrlForResource(
            final Resource resource) {
        checkNotNull(resource, "Resource cannot be null.");

        final String visibleBaseAppUrl = shortLinkGeneratorConfig_.getVisibleBaseAppUrl();
        final String resourcePath = resource.getPath();

        if (Resource.Type.FILE.equals(resource.getType())) {
            return String.format(FILE_RESOURCE_PATH_FORMAT, visibleBaseAppUrl, resourcePath);
        } else if (Resource.Type.DIRECTORY.equals(resource.getType())) {
            return String.format(BROWSE_RESOURCE_PATH_FORMAT, visibleBaseAppUrl, resourcePath);
        } else {
            throw new OnyxException("Unsupported short link resource type: "
                    + resource.getType());
        }
    }

}
