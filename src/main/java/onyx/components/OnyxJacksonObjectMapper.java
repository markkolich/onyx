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

package onyx.components;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import curacao.annotations.Component;
import org.openapitools.jackson.nullable.JsonNullableModule;

@Component
public final class OnyxJacksonObjectMapper {

    private final ObjectMapper objectMapper_;

    public OnyxJacksonObjectMapper() {
        objectMapper_ = JsonMapper.builder()
                // Ignore unknown properties in anything Jackson deserializes.
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // Write out Instants and Dates as human readable ISO-8601 strings.
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Do not serialize null fields.
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                // Required to support Instant typed entity fields.
                .addModule(new JavaTimeModule())
                // For JsonNullable<T> support.
                .addModule(new JsonNullableModule())
                .build();
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper_;
    }

}
