/*
 * Copyright (c) 2023 Mark S. Kolich
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

package onyx.mappers.request.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import curacao.mappers.request.types.body.AbstractInputStreamReaderRequestBodyMapper;
import onyx.exceptions.api.ApiBadRequestException;

import java.io.InputStreamReader;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractApiArgumentRequestMapper<T>
        extends AbstractInputStreamReaderRequestBodyMapper<T> {

    private final Class<T> clazz_;

    private final ObjectMapper objectMapper_;

    public AbstractApiArgumentRequestMapper(
            final Class<T> clazz,
            final ObjectMapper objectMapper) {
        clazz_ = checkNotNull(clazz, "API argument request mapper entity class cannot be null.");
        objectMapper_ = checkNotNull(objectMapper,
                "API argument request Jackson object mapper cannot be null.");
    }

    @Override
    public final T resolveWithReader(
            final InputStreamReader reader) throws Exception {
        try {
            return objectMapper_.readValue(reader, clazz_);
        } catch (final Exception e) {
            throw new ApiBadRequestException("Failed to parse request JSON.", e);
        }
    }

}
