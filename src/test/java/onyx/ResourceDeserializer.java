/*
 * Copyright (c) 2021 Mark S. Kolich
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

package onyx;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.dynamodbv2.datamodeling.S3Link;
import com.amazonaws.services.s3.model.Region;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import onyx.entities.storage.aws.dynamodb.Resource;

import java.io.IOException;
import java.time.Instant;

/**
 * Used by Jackson in unit tests to convert JSON resources on the test-classpath
 * to fully hydrated {@link Resource} objects.
 */
public final class ResourceDeserializer extends StdDeserializer<Resource> {

    private static final String PATH = "path";
    private static final String PARENT = "parent";
    private static final String SIZE = "size";
    private static final String DESCRIPTION = "description";
    private static final String TYPE = "type";
    private static final String VISIBILITY = "visibility";
    private static final String OWNER = "owner";
    private static final String FAVORITE = "favorite";

    private final S3Link.Factory s3LinkFactory_;

    public ResourceDeserializer() {
        this(null);
    }

    public ResourceDeserializer(
            final Class<?> vc) {
        super(vc);

        final AWSCredentialsProvider credentialsProvider =
                new AWSStaticCredentialsProvider(new AWSCredentials() {
                    @Override
                    public String getAWSAccessKeyId() {
                        return "unit-test";
                    }

                    @Override
                    public String getAWSSecretKey() {
                        return "unit-test";
                    }
                });
        s3LinkFactory_ = S3Link.Factory.of(credentialsProvider);
    }

    @Override
    public Resource deserialize(
            final JsonParser jp,
            final DeserializationContext ctx) throws IOException, JsonProcessingException {
        final JsonNode node = jp.getCodec().readTree(jp);

        final String path = nodeText(node, PATH);
        final String parent = nodeText(node, PARENT);
        final long size = nodeLong(node, SIZE);
        final String description = nodeText(node, DESCRIPTION);
        final Resource.Type type = stringToType(nodeText(node, TYPE));
        final Resource.Visibility visibility = stringToVisibility(nodeText(node, VISIBILITY));
        final String owner = nodeText(node, OWNER);
        final boolean favorite = nodeBoolean(node, FAVORITE);

        return new Resource.Builder()
                .setPath(path)
                .setParent(parent)
                .setSize(size)
                .setDescription(description)
                .setType(type)
                .setVisibility(visibility)
                .setOwner(owner)
                .setCreatedAt(Instant.now()) // now
                .setFavorite(favorite)
                .setS3Link(s3LinkFactory_.createS3Link(Region.US_West, "unit-test", path))
                .build();
    }

    private static String nodeText(
            final JsonNode node,
            final String fieldName) {
        final JsonNode textNode = node.get(fieldName);
        if (textNode == null) {
            throw new IllegalArgumentException("Found no field with name: " + fieldName);
        }

        return textNode.asText();
    }

    private static long nodeLong(
            final JsonNode node,
            final String fieldName) {
        return Long.parseLong(nodeText(node, fieldName));
    }

    private static boolean nodeBoolean(
            final JsonNode node,
            final String fieldName) {
        return Boolean.parseBoolean(nodeText(node, fieldName));
    }

    private static Resource.Type stringToType(
            final String type) {
        if (Resource.Type.DIRECTORY.toString().equals(type)) {
            return Resource.Type.DIRECTORY;
        } else if (Resource.Type.FILE.toString().equals(type)) {
            return Resource.Type.FILE;
        } else {
            throw new IllegalArgumentException("Unknown/unsupported resource type: " + type);
        }
    }

    private static Resource.Visibility stringToVisibility(
            final String visibility) {
        if (Resource.Visibility.PRIVATE.toString().equals(visibility)) {
            return Resource.Visibility.PRIVATE;
        } else if (Resource.Visibility.PUBLIC.toString().equals(visibility)) {
            return Resource.Visibility.PUBLIC;
        } else {
            throw new IllegalArgumentException("Unknown/unsupported resource visibility: " + visibility);
        }
    }

}
