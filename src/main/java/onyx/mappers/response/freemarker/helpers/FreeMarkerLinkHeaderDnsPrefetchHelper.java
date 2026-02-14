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

package onyx.mappers.response.freemarker.helpers;

import com.google.common.net.HttpHeaders;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.core.servlet.HttpResponse;
import onyx.components.config.aws.AwsConfig;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public final class FreeMarkerLinkHeaderDnsPrefetchHelper {

    private static final String DNS_PREFETCH_LINK_HEADER_FORMAT = "<%s>; rel=dns-prefetch";

    private final String dnsPrefetchLinkHeader_;

    @Injectable
    public FreeMarkerLinkHeaderDnsPrefetchHelper(
            @Nonnull final AwsConfig awsConfig) {
        final String dnsPrefetchLinkHeaderUrl = String.format("https://%s.s3.%s.amazonaws.com",
                awsConfig.getAwsS3BucketName(), awsConfig.getAwsS3Region());
        dnsPrefetchLinkHeader_ = String.format(DNS_PREFETCH_LINK_HEADER_FORMAT, dnsPrefetchLinkHeaderUrl);
    }

    public void addLinkHeader(
            final HttpResponse response) {
        checkNotNull(response, "HTTP servlet response cannot be null.");

        response.addHeader(HttpHeaders.LINK, dnsPrefetchLinkHeader_);
    }

}
