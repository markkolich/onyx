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

package onyx.components.authentication.twofactor;

import com.google.common.collect.ImmutableMap;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.core.servlet.HttpStatus;
import onyx.components.aws.sns.OnyxSnsClient;
import onyx.entities.authentication.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link TwoFactorAuthCodeManager} implementation that sends a 2FA verification
 * code as an SMS/text-message via {@link SnsClient}.
 */
@Component
public final class OnyxTwoFactorSmsAuthCodeManager implements TwoFactorAuthCodeManager {

    private static final Logger LOG = LoggerFactory.getLogger(OnyxTwoFactorSmsAuthCodeManager.class);

    private static final String SMS_VERIFICATION_MESSAGE = "Onyx verification code: %s";

    private final SnsClient sns_;

    private static final class Extensions {

        private static final String AWS_SNS_DATA_TYPE_STRING = "String";

        private static final String AWS_SNS_SMS_SMSTYPE = "AWS.SNS.SMS.SMSType";
        private static final String AWS_SNS_SMS_SMSTYPE_PROMOTIONAL = "Promotional";
        private static final String AWS_SNS_SMS_SMSTYPE_TRANSACTIONAL = "Transactional";

        private static MessageAttributeValue promotionalSms() {
            return MessageAttributeValue.builder()
                    .stringValue(AWS_SNS_SMS_SMSTYPE_PROMOTIONAL)
                    .dataType(AWS_SNS_DATA_TYPE_STRING)
                    .build();
        }

        private static MessageAttributeValue transactionalSms() {
            return MessageAttributeValue.builder()
                    .stringValue(AWS_SNS_SMS_SMSTYPE_TRANSACTIONAL)
                    .dataType(AWS_SNS_DATA_TYPE_STRING)
                    .build();
        }

    }

    @Injectable
    public OnyxTwoFactorSmsAuthCodeManager(
            final OnyxSnsClient snsClient) {
        sns_ = snsClient.getSnsClient();
    }

    @Override
    public void sendVerificationCodeToUser(
            final User user,
            final String code) {
        checkNotNull(user, "User cannot be null.");
        checkNotNull(code, "2FA verification code cannot be null.");

        try {
            final String smsMessage = String.format(SMS_VERIFICATION_MESSAGE, code);

            final Map<String, MessageAttributeValue> smsAttributes =
                    ImmutableMap.of(Extensions.AWS_SNS_SMS_SMSTYPE, Extensions.transactionalSms());

            final PublishRequest publishRequest = PublishRequest.builder()
                    .messageAttributes(smsAttributes)
                    .phoneNumber(user.getMobileNumber()) // E.164 formatted phone number
                    .message(smsMessage)
                    .build();

            // Send the text-message!
            final PublishResponse publishResponse = sns_.publish(publishRequest);

            final int publishStatus = publishResponse.sdkHttpResponse().statusCode();
            if (publishStatus != HttpStatus.SC_OK) {
                LOG.warn("AWS SNS publish failed while sending 2FA SMS text-message to user: "
                        + "{}, status: {}", user.getUsername(), publishStatus);
            }
        } catch (final Exception e) {
            LOG.error("Failed to send 2FA SMS text-message for user: {}",
                    user.getUsername(), e);
        }
    }

}
