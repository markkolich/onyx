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

package onyx.components.aws.s3;

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.components.ComponentDestroyable;
import onyx.components.aws.AwsClientConfig;
import onyx.components.aws.AwsCredentials;
import onyx.components.config.aws.AwsConfig;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Component
public final class OnyxS3Client implements ComponentDestroyable {

    private final S3Client s3_;
    private final S3Presigner presigner_;

    @Injectable
    public OnyxS3Client(
            final AwsConfig awsConfig,
            final AwsCredentials awsCredentials,
            final AwsClientConfig awsClientConfig) {
        final Region region = Region.of(awsConfig.getAwsS3Region());

        s3_ = S3Client.builder()
                .credentialsProvider(awsCredentials.getCredentialsProvider())
                .overrideConfiguration(awsClientConfig.getClientOverrideConfiguration())
                .region(region)
                .build();

        presigner_ = S3Presigner.builder()
                .credentialsProvider(awsCredentials.getCredentialsProvider())
                .region(region)
                .build();
    }

    public S3Client getS3Client() {
        return s3_;
    }

    public S3Presigner getPresigner() {
        return presigner_;
    }

    @Override
    public void destroy() throws Exception {
        s3_.close();
        presigner_.close();
    }

}
