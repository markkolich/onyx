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

import com.google.common.collect.ImmutableList;
import onyx.components.storage.sizer.SizerConfig;
import onyx.entities.storage.aws.dynamodb.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CostAnalyzerTest {

    private static final long ONE_GB = 1_073_741_824L;

    private static final List<StorageTier> MOCK_TIERS = ImmutableList.of(
            new StorageTier.Builder().setName("t1").setDaysSinceLastAccess(0).setCostPerGbPerMonth(0.023D).build(),
            new StorageTier.Builder().setName("t2").setDaysSinceLastAccess(30).setCostPerGbPerMonth(0.0125D).build(),
            new StorageTier.Builder().setName("t3").setDaysSinceLastAccess(90).setCostPerGbPerMonth(0.004D).build());

    private final CostAnalyzer costAnalyzer_;

    public CostAnalyzerTest() {
        final SizerConfig sizerConfig = Mockito.mock(SizerConfig.class);
        Mockito.when(sizerConfig.getCostAnalysisStorageTiers()).thenReturn(MOCK_TIERS);
        costAnalyzer_ = new OnyxCostAnalyzer(sizerConfig);
    }

    @Test
    public void zeroSizeResourceReturnZeroCostTest() {
        final Resource resource = new Resource()
                .setSize(0)
                .setCreatedAt(Instant.now())
                .setLastAccessedAt(Instant.now());

        assertEquals(BigDecimal.ZERO, costAnalyzer_.computeResourceCost(resource));
    }

    @Test
    public void negativeSizeResourceReturnZeroCostTest() {
        final Resource resource = new Resource()
                .setSize(-1)
                .setCreatedAt(Instant.now());

        assertEquals(BigDecimal.ZERO, costAnalyzer_.computeResourceCost(resource));
    }

    @Test
    public void recentAccessMatchesFrequentTierTest() {
        final Resource resource = new Resource()
                .setSize(ONE_GB)
                .setCreatedAt(Instant.now())
                .setLastAccessedAt(Instant.now());

        final BigDecimal cost = costAnalyzer_.computeResourceCost(resource);

        // 1 GB * $0.023/GB/month = $0.023
        assertEquals(0, new BigDecimal("0.023").compareTo(cost));
    }

    @Test
    public void thirtyDaysMatchesInfrequentTierTest() {
        final Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        final Resource resource = new Resource()
                .setSize(ONE_GB)
                .setCreatedAt(thirtyDaysAgo)
                .setLastAccessedAt(thirtyDaysAgo);

        final BigDecimal cost = costAnalyzer_.computeResourceCost(resource);

        // 1 GB * $0.0125/GB/month = $0.0125
        assertEquals(0, new BigDecimal("0.0125").compareTo(cost));
    }

    @Test
    public void ninetyDaysMatchesArchiveInstantAccessTierTest() {
        final Instant ninetyDaysAgo = Instant.now().minus(90, ChronoUnit.DAYS);
        final Resource resource = new Resource()
                .setSize(ONE_GB)
                .setCreatedAt(ninetyDaysAgo)
                .setLastAccessedAt(ninetyDaysAgo);

        final BigDecimal cost = costAnalyzer_.computeResourceCost(resource);

        assertEquals(0, new BigDecimal("0.004").compareTo(cost));
    }

    @Test
    public void betweenTiersMatchesLowerThresholdTest() {
        // 60 days is between infrequent (30) and archive-instant-access (90),
        // so the infrequent tier should match.
        final Instant sixtyDaysAgo = Instant.now().minus(60, ChronoUnit.DAYS);
        final Resource resource = new Resource()
                .setSize(ONE_GB)
                .setCreatedAt(sixtyDaysAgo)
                .setLastAccessedAt(sixtyDaysAgo);

        final BigDecimal cost = costAnalyzer_.computeResourceCost(resource);

        assertEquals(0, new BigDecimal("0.0125").compareTo(cost));
    }

    @Test
    public void fractionalGbScalesLinearlyTest() {
        final Resource resource = new Resource()
                .setSize(536_870_912L)
                .setCreatedAt(Instant.now())
                .setLastAccessedAt(Instant.now());

        final BigDecimal cost = costAnalyzer_.computeResourceCost(resource);

        // (512 MB / 1 GB) * $0.023 = $0.0115
        final BigDecimal expected = BigDecimal.valueOf(resource.getSize())
                .divide(CostAnalyzer.BYTES_PER_GB, MathContext.DECIMAL128)
                .multiply(new BigDecimal("0.023"));
        assertEquals(0, expected.compareTo(cost));
    }

    @Test
    public void lastAccessedAtTakesPrecedenceOverCreatedAtTest() {
        // Created a year ago (would be archive), but accessed recently (should be frequent).
        final Resource resource = new Resource()
                .setSize(ONE_GB)
                .setCreatedAt(Instant.now().minus(365, ChronoUnit.DAYS))
                .setLastAccessedAt(Instant.now().minus(1, ChronoUnit.DAYS));

        final BigDecimal cost = costAnalyzer_.computeResourceCost(resource);

        // Should match the frequent tier ($0.023) because lastAccessedAt is recent.
        assertEquals(0, new BigDecimal("0.023").compareTo(cost));
    }

    @Test
    public void nullLastAccessedAtFallsBackToCreatedAtTest() {
        final Resource resource = new Resource()
                .setSize(ONE_GB)
                .setCreatedAt(Instant.now().minus(90, ChronoUnit.DAYS));

        final BigDecimal cost = costAnalyzer_.computeResourceCost(resource);

        // Should match archive-instant-access tier ($0.004).
        assertEquals(0, new BigDecimal("0.004").compareTo(cost));
    }

    @Test
    public void veryOldResourceStillMatchesHighestTierTest() {
        // 1000 days ago - well beyond all tier thresholds.
        final Instant longAgo = Instant.now().minus(1000, ChronoUnit.DAYS);
        final Resource resource = new Resource()
                .setSize(ONE_GB)
                .setCreatedAt(longAgo)
                .setLastAccessedAt(longAgo);

        final BigDecimal cost = costAnalyzer_.computeResourceCost(resource);

        // Should still match archive-instant-access ($0.004).
        assertEquals(0, new BigDecimal("0.004").compareTo(cost));
    }

    @Test
    public void costIsPositiveForNonZeroSizeTest() {
        final Resource resource = new Resource()
                .setSize(1)
                .setCreatedAt(Instant.now())
                .setLastAccessedAt(Instant.now());

        final BigDecimal cost = costAnalyzer_.computeResourceCost(resource);

        assertTrue(cost.compareTo(BigDecimal.ZERO) > 0);
    }

}
