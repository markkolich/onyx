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

package onyx.entities.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.s3.model.Region;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;

import java.util.Date;

import static com.amazonaws.util.SdkHttpUtils.urlDecode;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

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
    private Date createdAt_;
    private Boolean favorite_;

    private S3Link s3Link_;

    @DynamoDBHashKey(attributeName = "path")
    public String getPath() {
        return path_;
    }

    public Resource setPath(
            final String path) {
        path_ = path;
        return this;
    }

    @DynamoDBRangeKey(attributeName = "parent")
    public String getParent() {
        return parent_;
    }

    public Resource setParent(
            final String parent) {
        parent_ = parent;
        return this;
    }

    @DynamoDBAttribute(attributeName = "description")
    public String getDescription() {
        return description_;
    }

    public Resource setDescription(
            final String description) {
        description_ = description;
        return this;
    }

    @DynamoDBAttribute(attributeName = "size")
    public long getSize() {
        return size_;
    }

    public Resource setSize(
            final long size) {
        size_ = size;
        return this;
    }

    @DynamoDBTypeConvertedEnum
    @DynamoDBAttribute(attributeName = "type")
    public Type getType() {
        return type_;
    }

    public Resource setType(
            final Type type) {
        type_ = type;
        return this;
    }

    @DynamoDBTypeConvertedEnum
    @DynamoDBAttribute(attributeName = "visibility")
    public Visibility getVisibility() {
        return visibility_;
    }

    public Resource setVisibility(
            final Visibility visibility) {
        visibility_ = visibility;
        return this;
    }

    @DynamoDBAttribute(attributeName = "owner")
    public String getOwner() {
        return owner_;
    }

    public Resource setOwner(
            final String owner) {
        owner_ = owner;
        return this;
    }

    @DynamoDBTypeConvertedTimestamp
    @DynamoDBAttribute(attributeName = "created")
    public Date getCreatedAt() {
        return createdAt_;
    }

    public Resource setCreatedAt(
            final Date createdAt) {
        createdAt_ = createdAt;
        return this;
    }

    @DynamoDBAttribute(attributeName = "favorite")
    public Boolean getFavorite() {
        return BooleanUtils.isTrue(favorite_);
    }

    public Resource setFavorite(
            final Boolean favorite) {
        favorite_ = favorite;
        return this;
    }

    @DynamoDBAttribute(attributeName = "s3")
    public S3Link getS3Link() {
        return s3Link_;
    }

    public Resource setS3Link(
            final S3Link s3Link) {
        s3Link_ = s3Link;
        return this;
    }

    // Derived fields

    /**
     * Returns the complete path of a resource, properly HTML escaped safe for injection
     * into an HTML template.
     */
    @DynamoDBIgnore
    public String getHtmlPath() {
        return escapeHtml4(urlDecode(path_));
    }

    /**
     * Returns the simple name of a resource, typically the string after the last "/".
     * For example, given a resource with path "/foo/bar/baz" this method would
     * return "baz". This method handles all URL decoding and HTML escaping of
     * the resulting name safe for injection into a HTML template.
     */
    @DynamoDBIgnore
    public String getHtmlName() {
        return escapeHtml4(urlDecode(FilenameUtils.getName(path_)));
    }

    /**
     * Returns a human readable/friendly size using {@link FileUtils#byteCountToDisplaySize}.
     */
    @DynamoDBIgnore
    public String getHtmlSize() {
        return FileUtils.byteCountToDisplaySize(size_);
    }

    /**
     * Returns the description of a resource, properly HTML escaped safe for injection
     * into an HTML template.
     */
    @DynamoDBIgnore
    public String getHtmlDescription() {
        return escapeHtml4(description_);
    }

    public static final class Builder {

        private String path_;
        private String parent_;
        private long size_;
        private String description_;
        private Type type_;
        private Visibility visibility_;
        private String owner_;
        private Date createdAt_;
        private Boolean favorite_;

        private S3Link s3Link_;

        private Region s3BucketRegion_;
        private String s3BucketName_;
        private IDynamoDBMapper dbMapper_;

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
                final Date createdAt) {
            createdAt_ = checkNotNull(createdAt, "Resource created at cannot be null.");
            return this;
        }

        public Builder setFavorite(
                final Boolean favorite) {
            favorite_ = checkNotNull(favorite, "Resource favorite cannot be null.");
            return this;
        }

        public Builder setS3Link(
                final S3Link s3Link) {
            s3Link_ = checkNotNull(s3Link, "Resource S3 link cannot be null.");
            return this;
        }

        public Builder withS3BucketRegion(
                final Region region) {
            s3BucketRegion_ = checkNotNull(region, "S3 bucket region cannot be null.");
            return this;
        }

        public Builder withS3Bucket(
                final String bucketName) {
            s3BucketName_ = checkNotNull(bucketName, "S3 bucket name cannot be null.");
            return this;
        }

        public Builder withDbMapper(
                final IDynamoDBMapper dbMapper) {
            dbMapper_ = checkNotNull(dbMapper, "Dynamo DB mapper cannot be null.");
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

            if (s3Link_ == null) {
                // If we're going to generate an S3 link at the time the resource is built
                // then we need to have a region, bucket name, and DB mapper set on the builder.
                checkNotNull(s3BucketRegion_, "S3 bucket region cannot be null.");
                checkNotNull(s3BucketName_, "S3 bucket name cannot be null.");
                checkNotNull(dbMapper_, "Dynamo DB mapper cannot be null.");

                // When used as part of an S3 key, we need to strip the leading "/".
                final String key = (path_.startsWith("/")) ? path_.substring(1) : path_;
                s3Link_ = dbMapper_.createS3Link(s3BucketRegion_, s3BucketName_, key);
            }

            return new Resource()
                    .setPath(path_)
                    .setParent(parent_)
                    .setSize(size_)
                    .setDescription(description_)
                    .setType(type_)
                    .setVisibility(visibility_)
                    .setOwner(owner_)
                    .setCreatedAt(createdAt_)
                    .setFavorite(favorite_)
                    .setS3Link(s3Link_);
        }

    }

}
