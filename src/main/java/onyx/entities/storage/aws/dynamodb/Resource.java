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

package onyx.entities.storage.aws.dynamodb;

import onyx.components.aws.dynamodb.converters.InstantToStringTypeConverter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static onyx.components.aws.dynamodb.DynamoDbManager.PARENT_INDEX_NAME;
import static onyx.util.FileUtils.humanReadableByteCountBin;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

@DynamoDbBean
public final class Resource {

    public enum Type {
        DIRECTORY, FILE
    }

    public enum Visibility {
        PUBLIC, PRIVATE
    }

    private String path_;
    private String parent_;
    private long size_;
    private String description_;
    private Type type_;
    private Visibility visibility_;
    private String owner_;
    private Instant createdAt_;
    private Boolean favorite_;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("path")
    public String getPath() {
        return path_;
    }

    public Resource setPath(
            final String path) {
        path_ = path;
        return this;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = PARENT_INDEX_NAME)
    @DynamoDbSortKey
    @DynamoDbAttribute("parent")
    public String getParent() {
        return parent_;
    }

    public Resource setParent(
            final String parent) {
        parent_ = parent;
        return this;
    }

    @DynamoDbAttribute("description")
    public String getDescription() {
        return description_;
    }

    public Resource setDescription(
            final String description) {
        description_ = description;
        return this;
    }

    @DynamoDbAttribute("size")
    public long getSize() {
        return size_;
    }

    public Resource setSize(
            final long size) {
        size_ = size;
        return this;
    }

    @DynamoDbAttribute("type")
    public Type getType() {
        return type_;
    }

    public Resource setType(
            final Type type) {
        type_ = type;
        return this;
    }

    @DynamoDbAttribute("visibility")
    public Visibility getVisibility() {
        return visibility_;
    }

    public Resource setVisibility(
            final Visibility visibility) {
        visibility_ = visibility;
        return this;
    }

    @DynamoDbAttribute("owner")
    public String getOwner() {
        return owner_;
    }

    public Resource setOwner(
            final String owner) {
        owner_ = owner;
        return this;
    }

    @DynamoDbConvertedBy(InstantToStringTypeConverter.class)
    @DynamoDbAttribute("created")
    public Instant getCreatedAt() {
        return createdAt_;
    }

    public Resource setCreatedAt(
            final Instant createdAt) {
        createdAt_ = createdAt;
        return this;
    }

    @DynamoDbAttribute("favorite")
    public Boolean getFavorite() {
        return BooleanUtils.isTrue(favorite_);
    }

    public Resource setFavorite(
            final Boolean favorite) {
        favorite_ = favorite;
        return this;
    }

    // Derived fields

    /**
     * Returns the S3 object key for this resource, derived from the path
     * by stripping the leading "/".
     */
    @DynamoDbIgnore
    public String getS3Key() {
        return (path_ != null && path_.startsWith("/")) ? path_.substring(1) : path_;
    }

    /**
     * Returns the complete path of a resource, properly URL decoded and HTML escaped,
     * safe for injection into an HTML template.
     */
    @DynamoDbIgnore
    public String getHtmlPath() {
        return escapeHtml4(URLDecoder.decode(path_, StandardCharsets.UTF_8));
    }

    /**
     * Returns the simple name of a resource, typically the string after the last "/".
     * For example, given a resource with path "/foo/bar/baz" this method would
     * return "baz". Note, this method does not URL decode or HTML escape
     * the resulting filename; for that, see {@link #getHtmlName()}.
     */
    @DynamoDbIgnore
    public String getName() {
        return FilenameUtils.getName(path_);
    }

    /**
     * Returns the simple name of a resource, typically the string after the last "/".
     * For example, given a resource with path "/foo/bar/baz" this method would
     * return "baz". This method handles all URL decoding and HTML escaping of
     * the resulting name safe for injection into an HTML template.
     */
    @DynamoDbIgnore
    public String getHtmlName() {
        return escapeHtml4(URLDecoder.decode(getName(), StandardCharsets.UTF_8));
    }

    /**
     * Returns a human readable/friendly size of the resource.
     */
    @DynamoDbIgnore
    public String getHtmlSize() {
        return humanReadableByteCountBin(size_);
    }

    /**
     * Returns the description of a resource, properly HTML escaped safe for injection
     * into an HTML template.
     */
    @DynamoDbIgnore
    public String getHtmlDescription() {
        return escapeHtml4(description_);
    }

    /**
     * Returns a {@link Date} representing the instant at which this resource
     * was created. This is really only useful for legacy APIs and libraries that
     * don't understand or handle {@link Instant}. Modern callers should use
     * {@link #getCreatedAt()} instead.
     */
    @Deprecated
    @DynamoDbIgnore
    public Date getCreatedDate() {
        return new Date(getCreatedAt().toEpochMilli());
    }

    @Override
    @DynamoDbIgnore
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static final class Builder {

        private String path_;
        private String parent_;
        private long size_;
        private String description_;
        private Type type_;
        private Visibility visibility_;
        private String owner_;
        private Instant createdAt_;
        private Boolean favorite_;

        public Builder setPath(
                final String path) {
            path_ = checkNotNull(path, "Resource path cannot be null.");
            return this;
        }

        public Builder setParent(
                final String parent) {
            parent_ = checkNotNull(parent, "Resource parent cannot be null.");
            return this;
        }

        public Builder setSize(
                final long size) {
            checkState(size_ >= 0L, "Resource size must be >= 0");
            size_ = size;
            return this;
        }

        public Builder setDescription(
                final String description) {
            description_ = checkNotNull(description, "Resource description cannot be null.");
            return this;
        }

        public Builder setType(
                final Type type) {
            type_ = checkNotNull(type, "Resource type cannot be null.");
            return this;
        }

        public Builder setVisibility(
                final Visibility visibility) {
            visibility_ = checkNotNull(visibility, "Resource visibility cannot be null.");
            return this;
        }

        public Builder setOwner(
                final String owner) {
            owner_ = checkNotNull(owner, "Resource owner cannot be null.");
            return this;
        }

        public Builder setCreatedAt(
                final Instant createdAt) {
            createdAt_ = checkNotNull(createdAt, "Resource created instant cannot be null.");
            return this;
        }

        public Builder setFavorite(
                final Boolean favorite) {
            favorite_ = checkNotNull(favorite, "Resource favorite cannot be null.");
            return this;
        }

        public Resource build() {
            checkNotNull(path_, "Resource path cannot be null.");
            checkState(path_.startsWith("/"), "Resource path must start with a '/'.");
            checkNotNull(parent_, "Resource parent cannot be null.");
            checkState(size_ >= 0L, "Resource size must be >= 0");
            checkNotNull(description_, "Resource description cannot be null.");
            checkNotNull(type_, "Resource type cannot be null.");
            checkNotNull(visibility_, "Resource visibility cannot be null.");
            checkNotNull(owner_, "Resource owner cannot be null.");
            checkNotNull(createdAt_, "Resource created at cannot be null.");

            return new Resource()
                    .setPath(path_)
                    .setParent(parent_)
                    .setSize(size_)
                    .setDescription(description_)
                    .setType(type_)
                    .setVisibility(visibility_)
                    .setOwner(owner_)
                    .setCreatedAt(createdAt_)
                    .setFavorite(favorite_);
        }

    }

}
