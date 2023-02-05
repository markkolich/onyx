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

package onyx.entities.freemarker;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import com.googlecode.htmlcompressor.compressor.YuiJavaScriptCompressor;
import curacao.entities.AbstractAppendableCuracaoEntity;
import onyx.util.HtmlUtils;
import org.apache.commons.codec.binary.StringUtils;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.net.MediaType.*;
import static curacao.core.servlet.HttpStatus.SC_OK;
import static org.apache.commons.io.IOUtils.copyLarge;

public final class Utf8TextEntity extends AbstractAppendableCuracaoEntity {

    private static final String HTML_UTF_8_STRING = HTML_UTF_8.toString();
    private static final String XML_UTF_8_STRING = XML_UTF_8.toString();
    private static final String TEXT_UTF_8_STRING = PLAIN_TEXT_UTF_8.toString();

    public enum EntityType {

        /**
         * HTML page; compressable.
         */
        HTML(HTML_UTF_8_STRING, true),

        /**
         * XML document, RSS feed, sitemap; compressable.
         */
        XML(XML_UTF_8_STRING, true),

        /**
         * Plain text document, robots.txt or humans.txt; not compressable.
         */
        TEXT(TEXT_UTF_8_STRING, false);

        /**
         * The HTTP Content-Type of the text entity.
         */
        private final String contentType_;

        /**
         * Whether or not the entity is compressable, meaning can it be fed through the HTML Compressor
         * to remove unnecessary new lines and other white space characters.  HTML and XML are "compressable"
         * but plain text is not.
         */
        private final boolean compressable_;

        EntityType(
                final String contentType,
                final boolean compressable) {
            contentType_ = contentType;
            compressable_ = compressable;
        }

        public String getType() {
            return contentType_;
        }

        public boolean isCompressable() {
            return compressable_;
        }

    }

    private final EntityType type_;
    private final int status_;
    private final String body_;

    public Utf8TextEntity(
            @Nonnull final EntityType type,
            @Nonnegative final int status,
            @Nonnull final String body) {
        type_ = checkNotNull(type, "HTML entity type cannot be null.");
        checkState(status >= SC_OK, "HTTP response status cannot be < " + SC_OK);
        status_ = status;
        body_ = checkNotNull(body, "HTML body cannot be null.");
    }

    public Utf8TextEntity(
            @Nonnull final EntityType type,
            @Nonnegative final int status,
            @Nonnull final byte[] body) {
        this(type, status, StringUtils.newStringUtf8(body));
    }

    public Utf8TextEntity(
            @Nonnull final EntityType type,
            @Nonnegative final int status,
            @Nonnull final ByteArrayOutputStream body) {
        this(type, status, body.toByteArray());
    }

    public Utf8TextEntity(
            @Nonnegative final EntityType type,
            @Nonnull final String body) {
        this(type, SC_OK, body);
    }

    public Utf8TextEntity(
            @Nonnull final EntityType type,
            @Nonnull final byte[] body) {
        this(type, SC_OK, body);
    }

    public Utf8TextEntity(
            @Nonnull final EntityType type,
            @Nonnull final ByteArrayOutputStream body) {
        this(type, SC_OK, body);
    }

    @Override
    public int getStatus() {
        return status_;
    }

    @Override
    public String getContentType() {
        return type_.getType();
    }

    @Override
    public void toWriter(
            final Writer writer) throws Exception {
        final String body = type_.isCompressable() ? compressHtml(body_) : body_;
        try (Reader reader = new StringReader(body)) {
            copyLarge(reader, writer);
        }
    }

    private static String compressHtml(
            final String uncompressed) {
        final String minified = HtmlUtils.minify(uncompressed);

        // Run the stripped result through the HTML compressor, compressing script blocks and
        // stripping out HTML comments and other junk.
        final HtmlCompressor compressor = new HtmlCompressor();
        compressor.setJavaScriptCompressor(new YuiJavaScriptCompressor());
        compressor.setCompressJavaScript(true);

        return compressor.compress(minified);
    }

}
