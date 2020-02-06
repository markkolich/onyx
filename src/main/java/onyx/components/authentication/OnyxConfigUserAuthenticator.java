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
import onyx.components.config.authentication.OnyxSessionConfig;
import onyx.components.config.aws.OnyxAwsConfig;
import onyx.components.storage.ResourceManager;
import onyx.components.storage.aws.dynamodb.DynamoDbMapper;
import onyx.entities.authentication.Session;
import onyx.entities.storage.aws.dynamodb.Resource;
import onyx.exceptions.OnyxException;
import onyx.util.PasswordHasher;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public final class OnyxConfigUserAuthenticator implements UserAuthenticator, ComponentInitializable {

    private static final Logger LOG = LoggerFactory.getLogger(OnyxConfigUserAuthenticator.class);

    private static final String USERS_USERNAME_PROP = "username";
    private static final String USERS_PASSWORD_PROP = "password";

    private final OnyxSessionConfig onyxSessionConfig_;
    private final OnyxAwsConfig onyxAwsConfig_;

    private final ResourceManager resourceManager_;

    private final IDynamoDBMapper dbMapper_;

    private final PasswordHasher pwHasher_;
    private final Map<String, String> userCredentials_;

    @Injectable
    public OnyxConfigUserAuthenticator(
            final OnyxSessionConfig onyxSessionConfig,
            final OnyxAwsConfig onyxAwsConfig,
            final ResourceManager resourceManager,
            final DynamoDbMapper dynamoDbMapper) {
        onyxSessionConfig_ = onyxSessionConfig;
        onyxAwsConfig_ = onyxAwsConfig;
        resourceManager_ = resourceManager;
        dbMapper_ = dynamoDbMapper.getDbMapper();

        pwHasher_ = PasswordHasher.getInstance();
        userCredentials_ = buildUserCredentialsFromConfig();
    }

    private Map<String, String> buildUserCredentialsFromConfig() {
        final ImmutableMap.Builder<String, String> userCredentialsBuilder =
                ImmutableMap.builder();

        final ConfigList userCredentialsInConfig = onyxSessionConfig_.getUsers();
        for (final ConfigValue configValue : userCredentialsInConfig) {
            if (!ConfigValueType.OBJECT.equals(configValue.valueType())) {
                continue;
            }

            @SuppressWarnings("unchecked") // intentional, and safe
            final Map<String, String> configUser = (Map<String, String>) configValue.unwrapped();

            final String username = configUser.get(USERS_USERNAME_PROP);
            if (StringUtils.isBlank(username)) {
                throw new OnyxException("Blank or null user-authenticator config key: "
                        + USERS_USERNAME_PROP);
            }

            final String password = configUser.get(USERS_PASSWORD_PROP);
            if (StringUtils.isBlank(password)) {
                throw new OnyxException("Blank or null user-authenticator config key: "
                        + USERS_PASSWORD_PROP);
            }

            userCredentialsBuilder.put(username, password);
        }

        return userCredentialsBuilder.build();
    }

    @Nullable
    @Override
    public Session getSession(
            final String username,
            final String password) {
        // Lookup the password from configuration.
        final String passwordFromConfig = userCredentials_.get(username);
        if (passwordFromConfig == null) {
            return null;
        }

        // Check if the provided password matches configuration.
        final boolean matches = pwHasher_.verify(password, passwordFromConfig);
        if (!matches) {
            return null;
        }

        final long sessionDurationInSeconds =
                onyxSessionConfig_.getSessionDuration(TimeUnit.SECONDS);
        final Date sessionExpiry =
                new Date(Instant.now().plusSeconds(sessionDurationInSeconds).toEpochMilli());

        return new Session.Builder()
                .setId(UUID.randomUUID().toString())
                .setUsername(username)
                .setExpiry(sessionExpiry)
                .build();
    }

    @Override
    public Session refreshSession(
            final Session session) {
        final long sessionDurationInSeconds =
                onyxSessionConfig_.getSessionDuration(TimeUnit.SECONDS);
        final Date sessionExpiry =
                new Date(Instant.now().plusSeconds(sessionDurationInSeconds).toEpochMilli());

        return session.toBuilder()
                .setExpiry(sessionExpiry)
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
                        .setCreatedAt(new Date()) // now
                        .withS3BucketRegion(Region.fromValue(onyxAwsConfig_.getAwsRegion().getName()))
                        .withS3Bucket(onyxAwsConfig_.getAwsS3BucketName())
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

}
