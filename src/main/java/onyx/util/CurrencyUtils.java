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

package onyx.util;

import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.util.Currency;

import java.math.BigDecimal;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;

public final class CurrencyUtils {

    private static final String CURRENCY_ISO_CODE_USD = "USD";

    private static final LocalizedNumberFormatter USD_FORMATTER =
            NumberFormatter.withLocale(Locale.US)
            .unit(Currency.getInstance(CURRENCY_ISO_CODE_USD))
            .precision(Precision.fixedFraction(2));

    // Cannot instantiate
    private CurrencyUtils() {
    }

    /**
     * Formats a {@link BigDecimal} amount as a human-readable USD currency string.
     * Always formats with exactly 2 decimal places for cents.
     *
     * <p>Examples: {@code "$0.00"}, {@code "$12.34"}, {@code "$1,234.56"}
     */
    public static String humanReadableCost(
            final BigDecimal amount) {
        checkNotNull(amount, "Amount cannot be null.");

        return USD_FORMATTER.format(amount).toString();
    }

}
