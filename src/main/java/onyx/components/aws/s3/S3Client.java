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

package onyx.components.aws.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.components.ComponentDestroyable;
import onyx.components.aws.AwsClientConfig;
import onyx.components.aws.AwsCredentials;
import onyx.components.config.aws.AwsConfig;

@Component
public final class S3Client implements ComponentDestroyable {

    private final AmazonS3 s3_;

    @Injectable
    public S3Client(
            final AwsConfig awsConfig,
            final AwsCredentials awsCredentials,
            final AwsClientConfig awsClientConfig) {
        s3_ = AmazonS3ClientBuilder.standard()
                .withCredentials(awsCredentials.getCredentialsProvider())
                .withClientConfiguration(awsClientConfig.getClientConfiguration())
                .withRegion(awsConfig.getAwsS3Region())
                .build();
    }

    public AmazonS3 getS3Client() {
        return s3_;
    }

    @Override
    public void destroy() throws Exception {
        s3_.shutdown();
    }

}
