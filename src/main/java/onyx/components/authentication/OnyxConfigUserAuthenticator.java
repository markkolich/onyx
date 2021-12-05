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

package onyx.components.authentication;

import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.s3.model.Region;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.components.ComponentInitializable;
import onyx.components.aws.dynamodb.DynamoDbMapper;
import onyx.components.config.authentication.SessionConfig;
import onyx.components.config.aws.AwsConfig;
import onyx.components.storage.ResourceManager;
import onyx.entities.authentication.Session;
import onyx.entities.authentication.Session.Type;
import onyx.entities.authentication.User;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.OnyxException;
import onyx.util.security.PasswordHasher;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public final class OnyxConfigUserAuthenticator implements UserAuthenticator, ComponentInitializable {

    private static final Logger LOG = LoggerFactory.getLogger(OnyxConfigUserAuthenticator.class);

    private static final String USERS_USERNAME_PROP = "username";
    private static final String USERS_PASSWORD_PROP = "password";
    private static final String USERS_MOBILE_NUMBER_PROP = "mobileNumber";

    private final SessionConfig sessionConfig_;
    private final AwsConfig awsConfig_;

    private final ResourceManager resourceManager_;

    private final IDynamoDBMapper dbMapper_;

    private final PasswordHasher pwHasher_;
    private final Map<String, User> userCredentials_;

    @Injectable
    public OnyxConfigUserAuthenticator(
            final SessionConfig sessionConfig,
            final AwsConfig awsConfig,
            final ResourceManager resourceManager,
            final DynamoDbMapper dynamoDbMapper) {
        sessionConfig_ = sessionConfig;
        awsConfig_ = awsConfig;
        resourceManager_ = resourceManager;
        dbMapper_ = dynamoDbMapper.getDbMapper();

        pwHasher_ = PasswordHasher.getInstance();
        userCredentials_ = buildUserCredentialsFromConfig();
    }

    private Map<String, User> buildUserCredentialsFromConfig() {
        final ImmutableMap.Builder<String, User> userCredentialsBuilder =
                ImmutableMap.builder();

        final ConfigList userCredentialsInConfig = sessionConfig_.getUsers();
        for (final ConfigValue configValue : userCredentialsInConfig) {
            if (!ConfigValueType.OBJECT.equals(configValue.valueType())) {
                continue;
            }

            @SuppressWarnings("unchecked") // intentional, and safe
            final Map<String, String> configUser = (Map<String, String>) configValue.unwrapped();

            final String username = configUser.get(USERS_USERNAME_PROP);
            if (StringUtils.isBlank(username)) {
                throw missingConfigKey(USERS_USERNAME_PROP);
            }

            final String password = configUser.get(USERS_PASSWORD_PROP);
            if (StringUtils.isBlank(password)) {
                throw missingConfigKey(USERS_PASSWORD_PROP);
            }

            final String mobileNumber = configUser.get(USERS_MOBILE_NUMBER_PROP);
            if (StringUtils.isBlank(mobileNumber)) {
                throw missingConfigKey(USERS_MOBILE_NUMBER_PROP);
            }

            final User user = new User.Builder()
                    .setUsername(username)
                    .setPassword(password)
                    .setMobileNumber(mobileNumber)
                    .build();

            userCredentialsBuilder.put(username, user);
        }

        return userCredentialsBuilder.build();
    }

    @Nullable
    @Override
    public Pair<User, Session> getSessionForCredentials(
            final String username,
            final String password) {
        // Lookup the password from configuration.
        final User userFromConfig = userCredentials_.get(username);
        if (userFromConfig == null) {
            LOG.warn("Found no user for username: {}", username);
            return null;
        }

        // Check if the provided password matches configuration.
        final String passwordFromConfig = userFromConfig.getPassword();
        final boolean matches = pwHasher_.verify(password, passwordFromConfig);
        if (!matches) {
            LOG.warn("Failed authentication attempt for user: {}", username);
            return null;
        }

        final Session session = getSessionForUsername(Type.USER, username);

        return Pair.of(userFromConfig, session);
    }

    @Override
    public Session getSessionForUsername(
            final Type sessionType,
            final String username) {
        final long sessionDurationInSeconds =
                sessionConfig_.getSessionDuration(TimeUnit.SECONDS);
        final Instant sessionExpiry = Instant.now().plusSeconds(sessionDurationInSeconds);
        final long refreshSessionAfterInSeconds =
                sessionConfig_.getRefreshSessionAfter(TimeUnit.SECONDS);
        final Instant refreshAfter = Instant.now().plusSeconds(refreshSessionAfterInSeconds);

        return new Session.Builder()
                .setId(UUID.randomUUID().toString())
                .setType(sessionType)
                .setUsername(username)
                .setExpiry(sessionExpiry)
                .setRefreshAfter(refreshAfter)
                .build();
    }

    @Override
    public Session refreshSession(
            final Session session) {
        final long sessionDurationInSeconds =
                sessionConfig_.getSessionDuration(TimeUnit.SECONDS);
        final Instant sessionExpiry = Instant.now().plusSeconds(sessionDurationInSeconds);
        final long refreshSessionAfterInSeconds =
                sessionConfig_.getRefreshSessionAfter(TimeUnit.SECONDS);
        final Instant refreshAfter = Instant.now().plusSeconds(refreshSessionAfterInSeconds);

        return session.toBuilder()
                .setExpiry(sessionExpiry)
                .setRefreshAfter(refreshAfter)
                .build();
    }

    @Override
    public void initialize() throws Exception {
        // Verify that any users in configuration have an active "home directory".
        // If not, create them!
        for (final String username : userCredentials_.keySet()) {
            final String userDirectoryPath = ResourceManager.ROOT_PATH + username;

            final Resource userHomeDirectory = resourceManager_.getResourceAtPath(userDirectoryPath);
            if (userHomeDirectory == null) {
                final Resource newHome = new Resource.Builder()
                        .setPath(userDirectoryPath)
                        .setParent(ResourceManager.ROOT_PATH)
                        .setDescription("")
                        .setType(Resource.Type.DIRECTORY)
                        .setVisibility(Resource.Visibility.PUBLIC)
                        .setOwner(username)
                        .setCreatedAt(Instant.now()) // now
                        .withS3BucketRegion(Region.fromValue(awsConfig_.getAwsS3Region().getName()))
                        .withS3Bucket(awsConfig_.getAwsS3BucketName())
                        .withDbMapper(dbMapper_)
                        .build();

                resourceManager_.createResource(newHome);
                LOG.info("Created user home directory: {}", userDirectoryPath);
            } else {
                LOG.debug("User home directory already exists, nothing to create: {}",
                        userDirectoryPath);
            }
        }
    }

    private static OnyxException missingConfigKey(
            final String key) {
        return new OnyxException("Blank or null user-authenticator config key: " + key);
    }

}
