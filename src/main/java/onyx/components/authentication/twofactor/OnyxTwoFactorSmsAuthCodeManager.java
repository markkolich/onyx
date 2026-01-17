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

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.google.common.collect.ImmutableMap;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.core.servlet.HttpStatus;
import onyx.components.aws.sns.SnsClient;
import onyx.entities.authentication.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link TwoFactorAuthCodeManager} implementation that sends a 2FA verification
 * code as an SMS/text-message via {@link AmazonSNS}.
 */
@Component
public final class OnyxTwoFactorSmsAuthCodeManager implements TwoFactorAuthCodeManager {

    private static final Logger LOG = LoggerFactory.getLogger(OnyxTwoFactorSmsAuthCodeManager.class);

    private static final String AWS_SNS_DATA_TYPE_STRING = "String";

    private static final String AWS_SNS_SMS_SMSTYPE = "AWS.SNS.SMS.SMSType";
    private static final String AWS_SNS_SMS_SMSTYPE_PROMOTIONAL = "Promotional";
    private static final String AWS_SNS_SMS_SMSTYPE_TRANSACTIONAL = "Transactional";

    private static final String SMS_VERIFICATION_MESSAGE = "Onyx verification code: %s";

    private final AmazonSNS sns_;

    @Injectable
    public OnyxTwoFactorSmsAuthCodeManager(
            final SnsClient snsClient) {
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

            final MessageAttributeValue smsAttributeType = new MessageAttributeValue()
                    .withStringValue(AWS_SNS_SMS_SMSTYPE_TRANSACTIONAL)
                    .withDataType(AWS_SNS_DATA_TYPE_STRING);
            final Map<String, MessageAttributeValue> smsAttributes =
                    ImmutableMap.of(AWS_SNS_SMS_SMSTYPE, smsAttributeType);

            final PublishRequest publishRequest = new PublishRequest()
                    .withMessageAttributes(smsAttributes)
                    .withPhoneNumber(user.getMobileNumber()) // E.164 formatted phone number
                    .withMessage(smsMessage);

            // Send the text-message!
            final PublishResult publishResult = sns_.publish(publishRequest);

            final int publishStatus = publishResult.getSdkHttpMetadata().getHttpStatusCode();
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
