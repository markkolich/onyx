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

package onyx.components.aws.sns;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.components.ComponentDestroyable;
import onyx.components.aws.AwsClientConfig;
import onyx.components.aws.AwsCredentials;
import onyx.components.config.aws.AwsConfig;

@Component
public final class SnsClient implements ComponentDestroyable {

    private final AmazonSNS sns_;

    @Injectable
    public SnsClient(
            final AwsConfig awsConfig,
            final AwsCredentials awsCredentials,
            final AwsClientConfig awsClientConfig) {
        sns_ = AmazonSNSClientBuilder.standard()
                .withCredentials(awsCredentials.getCredentialsProvider())
                .withClientConfiguration(awsClientConfig.getClientConfiguration())
                .withRegion(awsConfig.getAwsSnsRegion())
                .build();
    }

    public AmazonSNS getSnsClient() {
        return sns_;
    }

    @Override
    public void destroy() throws Exception {
        sns_.shutdown();
    }

}
