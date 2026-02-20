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

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import onyx.components.storage.sizer.SizerConfig;
import onyx.entities.storage.aws.dynamodb.Resource;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public final class OnyxCostAnalyzer implements CostAnalyzer {

    private final List<StorageTier> storageTiers_;

    @Injectable
    public OnyxCostAnalyzer(
            final SizerConfig sizerConfig) {
        storageTiers_ = sizerConfig.getCostAnalysisStorageTiers();
    }

    /**
     * Computes the storage cost for a resource based on its size and the
     * applicable S3 tier. The tier is determined by the number of days
     * since the resource was last accessed (or created, if never accessed).
     *
     * <p>The tiers are sorted by {@code daysSinceLastAccess} ascending.
     * The highest matching threshold wins (list is walked in reverse).
     *
     * @param resource the resource to compute cost for
     * @return the computed cost as a {@link BigDecimal}
     */
    @Override
    public BigDecimal computeResourceCost(
            final Resource resource) {
        checkNotNull(resource, "Resource cannot be null.");

        if (resource.getSize() <= 0L) {
            return BigDecimal.ZERO;
        }

        // If last accessed is not set on the resource, then default to the created at.
        final Instant lastAccessed = resource.getLastAccessedAt() != null
                ? resource.getLastAccessedAt()
                : resource.getCreatedAt();

        return computeResourceCost(resource.getSize(), lastAccessed);
    }

    /**
     * Computes the storage cost for a resource based on its size and the
     * applicable S3 tier. The tier is determined by the number of days
     * since the resource was last accessed (or created, if never accessed).
     *
     * <p>The tiers are sorted by {@code daysSinceLastAccess} ascending.
     * The highest matching threshold wins (list is walked in reverse).
     *
     * @param resourceSize the byte size of the resource
     * @param lastAccessed the last accessed instant of the resource
     * @return the computed cost as a {@link BigDecimal}
     */
    @Override
    public BigDecimal computeResourceCost(
            final long resourceSize,
            final Instant lastAccessed) {
        checkNotNull(lastAccessed, "Last accessed instant cannot be null.");

        final long daysSinceLastAccess = ChronoUnit.DAYS.between(lastAccessed, Instant.now());

        // Walk tiers in reverse to find the highest matching threshold.
        StorageTier matchedTier = storageTiers_.get(0);
        for (int i = storageTiers_.size() - 1; i >= 0; i--) {
            final StorageTier tier = storageTiers_.get(i);
            if (daysSinceLastAccess >= tier.getDaysSinceLastAccess()) {
                matchedTier = tier;
                break;
            }
        }

        // cost = (size_bytes / bytes_per_gb) * cost_per_gb_per_month
        return BigDecimal.valueOf(resourceSize)
                .divide(BYTES_PER_GB, MathContext.DECIMAL128)
                .multiply(matchedTier.getCostPerGbPerMonth());
    }

}
