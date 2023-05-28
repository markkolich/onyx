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

package onyx.entities.api.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import curacao.entities.CuracaoEntity;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static curacao.core.servlet.HttpStatus.SC_OK;

public interface OnyxApiResponseEntity extends CuracaoEntity {

    @JsonIgnore
    @Override
    default int getStatus() {
        return SC_OK;
    }

    @JsonIgnore
    @Override
    default String getContentType() {
        return JSON_UTF_8.toString();
    }

    @JsonIgnore
    default Charset getCharset() {
        return StandardCharsets.UTF_8;
    }

    @JsonIgnore
    ObjectMapper getMapper();

    @JsonIgnore
    @Override
    default void write(
            final OutputStream os) throws Exception {
        try (Writer w = new OutputStreamWriter(os, getCharset())) {
            getMapper().writeValue(w, this);
        }
    }

    abstract class AbstractOnyxApiResponseEntityBuilder {

        protected final ObjectMapper objectMapper_;

        public AbstractOnyxApiResponseEntityBuilder(
                final ObjectMapper objectMapper) {
            objectMapper_ = checkNotNull(objectMapper, "Object mapper cannot be null.");
        }

    }

}
