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

package onyx.components.storage.sizer.cost;

import java.math.BigDecimal;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public final class StorageTier {

    private final String name_;
    private final int daysSinceLastAccess_;
    private final BigDecimal costPerGbPerMonth_;

    private StorageTier(
            final String name,
            final int daysSinceLastAccess,
            final BigDecimal costPerGbPerMonth) {
        name_ = checkNotNull(name, "Tier name cannot be null.");
        checkState(daysSinceLastAccess >= 0, "Days since last access must be >= 0.");
        daysSinceLastAccess_ = daysSinceLastAccess;
        costPerGbPerMonth_ = checkNotNull(costPerGbPerMonth, "Cost per GB per month cannot be null.");
    }

    public String getName() {
        return name_;
    }

    public int getDaysSinceLastAccess() {
        return daysSinceLastAccess_;
    }

    public BigDecimal getCostPerGbPerMonth() {
        return costPerGbPerMonth_;
    }

    public static final class Builder {

        private String name_;
        private int daysSinceLastAccess_;
        private BigDecimal costPerGbPerMonth_;

        public Builder setName(
                final String name) {
            name_ = checkNotNull(name, "Tier name cannot be null.");
            return this;
        }

        public Builder setDaysSinceLastAccess(
                final int daysSinceLastAccess) {
            checkState(daysSinceLastAccess >= 0, "Days since last access must be >= 0.");
            daysSinceLastAccess_ = daysSinceLastAccess;
            return this;
        }

        public Builder setCostPerGbPerMonth(
                final BigDecimal costPerGbPerMonth) {
            costPerGbPerMonth_ = checkNotNull(costPerGbPerMonth, "Cost per GB per month cannot be null.");
            return this;
        }

        public Builder setCostPerGbPerMonth(
                final double costPerGbPerMonth) {
            return setCostPerGbPerMonth(BigDecimal.valueOf(costPerGbPerMonth));
        }

        public StorageTier build() {
            checkNotNull(name_, "Tier name cannot be null.");
            checkState(daysSinceLastAccess_ >= 0, "Days since last access must be >= 0.");
            checkNotNull(costPerGbPerMonth_, "Cost per GB per month cannot be null.");

            return new StorageTier(name_, daysSinceLastAccess_, costPerGbPerMonth_);
        }

    }

}
