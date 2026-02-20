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

package onyx;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import onyx.entities.storage.aws.dynamodb.Resource;

import java.io.IOException;
import java.math.BigDecimal;
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
    private static final String CREATED_AT = "createdAt";
    private static final String LAST_ACCESSED_AT = "lastAccessedAt";
    private static final String COST = "cost";

    public ResourceDeserializer() {
        this(null);
    }

    public ResourceDeserializer(
            final Class<?> vc) {
        super(vc);
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

        final Instant createdAt = node.has(CREATED_AT)
                ? Instant.parse(nodeText(node, CREATED_AT))
                : Instant.now();

        final Instant lastAccessedAt = node.has(LAST_ACCESSED_AT)
                ? Instant.parse(nodeText(node, LAST_ACCESSED_AT))
                : null;

        final BigDecimal cost = node.has(COST)
                ? new BigDecimal(nodeText(node, COST))
                : BigDecimal.ZERO;

        return new Resource.Builder()
                .setPath(path)
                .setParent(parent)
                .setSize(size)
                .setDescription(description)
                .setType(type)
                .setVisibility(visibility)
                .setOwner(owner)
                .setCreatedAt(createdAt)
                .setLastAccessedAt(lastAccessedAt)
                .setFavorite(favorite)
                .setCost(cost)
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
