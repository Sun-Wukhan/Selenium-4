package api.perf;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Load budget for the two catalogue pages: the shop's hot path, hit by
 * every shopper and every UI test. 240 requests from 12 threads must
 * come back clean, with p95 inside the budget.
 */
public class CatalogPerfTest {

    private final PerfClient shop = new PerfClient();

    @Test
    public void moisturizerPageHoldsUpUnderLoad() {
        LoadHarness.Stats stats = LoadHarness.run(
                PerfBudgets.REQUESTS, PerfBudgets.THREADS, PerfBudgets.WARMUP, shop::moisturizers);
        PerfReport.record("GET /moisturizer", stats, PerfBudgets.CATALOG_P95_MS);
        Assert.assertEquals(stats.errors(), 0, "catalogue requests should not error under load");
        Assert.assertTrue(stats.p95Millis() < PerfBudgets.CATALOG_P95_MS,
                "moisturizer p95 " + stats.p95Millis() + "ms exceeds budget "
                        + PerfBudgets.CATALOG_P95_MS + "ms");
    }

    @Test
    public void sunscreenPageHoldsUpUnderLoad() {
        LoadHarness.Stats stats = LoadHarness.run(
                PerfBudgets.REQUESTS, PerfBudgets.THREADS, PerfBudgets.WARMUP, shop::sunscreens);
        PerfReport.record("GET /sunscreen", stats, PerfBudgets.CATALOG_P95_MS);
        Assert.assertEquals(stats.errors(), 0, "catalogue requests should not error under load");
        Assert.assertTrue(stats.p95Millis() < PerfBudgets.CATALOG_P95_MS,
                "sunscreen p95 " + stats.p95Millis() + "ms exceeds budget "
                        + PerfBudgets.CATALOG_P95_MS + "ms");
    }
}
