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

package onyx.util;

import com.google.common.net.HttpHeaders;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An internal utility class that can be used to build out a well-formed
 * {@link HttpHeaders#CONTENT_DISPOSITION} HTTP response header.
 */
public final class ContentDisposition {

    public enum DispositionType {

        INLINE("inline"),
        ATTACHMENT("attachment"),
        FORM_DATA("form-data");

        private final String type_;

        DispositionType(
                final String type) {
            type_ = checkNotNull(type, "Content disposition type cannot be null.");
        }

        public final String getType() {
            return type_;
        }

    }

    private static final String EXTENSION_PDF = "pdf";
    private static final String EXTENSION_CSV = "csv";
    private static final String EXTENSION_XLSX = "xlsx";
    private static final String EXTENSION_ZIP = "zip";

    private final DispositionType type_;

    private final Supplier<String> nameSupplier_;

    private final Supplier<String> fileNameSupplier_;
    private final Supplier<String> fileExtensionSupplier_;

    private ContentDisposition(
            final DispositionType type,
            @Nullable final Supplier<String> nameSupplier,
            @Nullable final Supplier<String> fileNameSupplier,
            @Nullable final Supplier<String> fileExtensionSupplier) {
        type_ = checkNotNull(type, "Content disposition type cannot be null.");

        nameSupplier_ = nameSupplier;

        fileNameSupplier_ = fileNameSupplier;
        fileExtensionSupplier_ = fileExtensionSupplier;
    }

    /**
     * Returns a well-formed content disposition that can be used within
     * an {@link HttpHeaders#CONTENT_DISPOSITION} HTTP response header.
     */
    public String getHeader() {
        final StringBuilder sb = new StringBuilder(type_.getType());

        final String name = (nameSupplier_ != null) ? nameSupplier_.get() : null;
        if (StringUtils.isNotBlank(name)) {
            sb.append("; ").append(String.format("name=\"%s\"", name));
        }

        final String fileName = (fileNameSupplier_ != null) ? fileNameSupplier_.get() : null;
        if (StringUtils.isNotBlank(fileName)) {
            final String sanitizedFileName = fileName.replaceAll("[^a-zA-Z0-9-_]+", "_");
            sb.append("; ").append(String.format("filename=\"%s", sanitizedFileName));

            final String fileExtension = (fileExtensionSupplier_ != null) ? fileExtensionSupplier_.get() : null;
            if (StringUtils.isNotBlank(fileExtension)) {
                sb.append(String.format(".%s\"",
                        fileExtension.startsWith(".") ? fileExtension.substring(1) : fileExtension));
            } else {
                sb.append("\"");
            }
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return getHeader();
    }

    public static final class Builder {

        private DispositionType type_;

        private Supplier<String> nameSupplier_;

        private Supplier<String> fileNameSupplier_;
        private Supplier<String> fileExtensionSupplier_;

        public Builder setType(
                final DispositionType type) {
            type_ = type;
            return this;
        }

        public Builder setNameSupplier(
                @Nullable final Supplier<String> nameSupplier) {
            nameSupplier_ = nameSupplier;
            return this;
        }

        public Builder setName(
                @Nullable final String name) {
            return setNameSupplier(() -> name);
        }

        public Builder setFileNameSupplier(
                @Nullable final Supplier<String> fileNameSupplier) {
            fileNameSupplier_ = fileNameSupplier;
            return this;
        }

        /**
         * Sets the filename, without the dot-extension. Use {@link FilenameUtils#getBaseName(String)}
         * if you need to extract just the name from an existing filename string prior to calling this
         * method.
         */
        public Builder setFileName(
                @Nullable final String fileName) {
            return setFileNameSupplier(() -> fileName);
        }

        public Builder setFileExtensionSupplier(
                @Nullable final Supplier<String> fileExtensionSupplier) {
            fileExtensionSupplier_ = fileExtensionSupplier;
            return this;
        }

        /**
         * Sets the file extension, with or without the leading dot. If the provided extension
         * string contains a leading dot (e.g., {@code .txt}), then the dot is removed ensuring
         * that the generated content disposition string does not contain two-dots before the
         * extension.
         */
        public Builder setFileExtension(
                @Nullable final String fileExtension) {
            return setFileExtensionSupplier(() -> fileExtension);
        }

        public ContentDisposition build() {
            checkNotNull(type_, "Content disposition type cannot be null.");

            return new ContentDisposition(type_, nameSupplier_, fileNameSupplier_, fileExtensionSupplier_);
        }

        // Helpers

        public static Builder pdf() {
            return pdf(DispositionType.INLINE);
        }

        public static Builder pdf(
                final DispositionType type) {
            checkNotNull(type, "Content disposition type cannot be null.");

            return new Builder()
                    .setType(type)
                    .setFileExtension(EXTENSION_PDF);
        }

        public static Builder csv() {
            return csv(DispositionType.ATTACHMENT);
        }

        public static Builder csv(
                final DispositionType type) {
            checkNotNull(type, "Content disposition type cannot be null.");

            return new Builder()
                    .setType(type)
                    .setFileExtension(EXTENSION_CSV);
        }

        public static Builder xlsx() {
            return xlsx(DispositionType.ATTACHMENT);
        }

        public static Builder xlsx(
                final DispositionType type) {
            checkNotNull(type, "Content disposition type cannot be null.");

            return new Builder()
                    .setType(type)
                    .setFileExtension(EXTENSION_XLSX);
        }

        public static Builder zip() {
            return zip(DispositionType.ATTACHMENT);
        }

        public static Builder zip(
                final DispositionType type) {
            checkNotNull(type, "Content disposition type cannot be null.");

            return new Builder()
                    .setType(type)
                    .setFileExtension(EXTENSION_ZIP);
        }

    }

}
