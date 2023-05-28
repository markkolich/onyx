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

package onyx.entities.shortlink.bitly.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonDeserialize(builder = BitlyShortenUrlResponse.Builder.class)
public interface BitlyShortenUrlResponse {

    @JsonProperty("id")
    String getId();

    @JsonProperty("link")
    String getLink();

    @JsonProperty("long_url")
    String getLongUrl();

    @JsonIgnore
    default Builder toBuilder() {
        return new BitlyShortenUrlResponse.Builder()
                .setId(getId())
                .setLink(getLink())
                .setLongUrl(getLongUrl());
    }

    final class Builder {

        private String id_;

        private String link_;
        private String longUrl_;

        @JsonProperty("id")
        public Builder setId(
                final String id) {
            id_ = id;
            return this;
        }

        @JsonProperty("link")
        public Builder setLink(
                final String link) {
            link_ = link;
            return this;
        }

        @JsonProperty("long_url")
        public Builder setLongUrl(
                final String longUrl) {
            longUrl_ = longUrl;
            return this;
        }

        public BitlyShortenUrlResponse build() {
            checkNotNull(id_, "Shortened link ID cannot be null.");
            checkNotNull(link_, "Shortened link URL cannot be null.");
            checkNotNull(longUrl_, "Shortened link long URL cannot be null.");

            return new BitlyShortenUrlResponse() {
                @Override
                public String getId() {
                    return id_;
                }

                @Override
                public String getLink() {
                    return link_;
                }

                @Override
                public String getLongUrl() {
                    return longUrl_;
                }
            };
        }

    }

}
