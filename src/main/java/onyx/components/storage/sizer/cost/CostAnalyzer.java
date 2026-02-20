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

import onyx.entities.storage.aws.dynamodb.Resource;

import java.math.BigDecimal;
import java.time.Instant;

public interface CostAnalyzer {

    BigDecimal BYTES_PER_GB = BigDecimal.valueOf(1_073_741_824L);

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
    BigDecimal computeResourceCost(
            final Resource resource);

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
    BigDecimal computeResourceCost(
            final long resourceSize,
            final Instant lastAccessed);

}
