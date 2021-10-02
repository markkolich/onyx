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

package onyx.components.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.config.aws.AwsConfig;

@Component
public final class AwsCredentials {

    private final AWSCredentials awsCredentials_;
    private final AWSCredentialsProvider awsCredentialsProvider_;

    @Injectable
    public AwsCredentials(
            final AwsConfig awsConfig) {
        awsCredentials_ = new BasicAWSCredentials(awsConfig.getAwsAccessKey(),
                awsConfig.getAwsSecretKey());
        awsCredentialsProvider_ = new AWSStaticCredentialsProvider(awsCredentials_);
    }

    public AWSCredentials getCredentials() {
        return awsCredentials_;
    }

    public AWSCredentialsProvider getCredentialsProvider() {
        return awsCredentialsProvider_;
    }

}
