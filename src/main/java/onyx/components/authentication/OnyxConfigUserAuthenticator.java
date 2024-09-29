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

package onyx.components.authentication;

import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.s3.model.Region;
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
import onyx.util.security.PasswordHasher;
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
        userCredentials_ = sessionConfig_.getUsers();
    }

    @Nullable
    @Override
    public Pair<User, Session> getSessionForCredentials(
            final String username,
            final String password) {
        // Lookup the password from configuration.
        final User userFromConfig = userCredentials_.get(username);
        if (userFromConfig == null) {
            LOG.debug("Found no user for username: {}", username);
            return null;
        }

        // Check if the provided password matches configuration.
        final String passwordFromConfig = userFromConfig.getPassword();
        final boolean matches = pwHasher_.verify(password, passwordFromConfig);
        if (!matches) {
            LOG.debug("Failed authentication attempt for user: {}", username);
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

}
