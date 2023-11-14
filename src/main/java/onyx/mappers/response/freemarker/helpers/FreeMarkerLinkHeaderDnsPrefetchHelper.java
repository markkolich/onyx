/*
 * Copyright (c) 2024 Mark S. Kolich
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

import com.amazonaws.services.s3.AmazonS3;
import com.google.common.net.HttpHeaders;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.core.servlet.HttpResponse;
import onyx.components.aws.s3.S3Client;
import onyx.components.config.aws.AwsConfig;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public final class FreeMarkerLinkHeaderDnsPrefetchHelper {

    private static final String DNS_PREFETCH_LINK_HEADER_FORMAT = "<%s>; rel=dns-prefetch";

    private final AwsConfig awsConfig_;
    private final AmazonS3 s3_;

    private final String dnsPrefetchLinkHeader_;

    @Injectable
    public FreeMarkerLinkHeaderDnsPrefetchHelper(
            @Nonnull final AwsConfig awsConfig,
            @Nonnull final S3Client s3Client) {
        awsConfig_ = awsConfig;
        s3_ = s3Client.getS3Client();

        final String dnsPrefetchLinkHeaderUrl = getDnsPrefetchLinkHeaderUrl();
        dnsPrefetchLinkHeader_ = String.format(DNS_PREFETCH_LINK_HEADER_FORMAT, dnsPrefetchLinkHeaderUrl);
    }

    public void addLinkHeader(
            final HttpResponse response) {
        checkNotNull(response, "HTTP servlet response cannot be null.");

        response.addHeader(HttpHeaders.LINK, dnsPrefetchLinkHeader_);
    }

    private String getDnsPrefetchLinkHeaderUrl() {
        final String s3BucketName = awsConfig_.getAwsS3BucketName();
        final String s3BucketUrl = s3_.getUrl(s3BucketName, null).toExternalForm();
        if (s3BucketUrl.endsWith("/")) {
            return StringUtils.removeEnd(s3BucketUrl, "/");
        }

        return s3BucketUrl;
    }

}
