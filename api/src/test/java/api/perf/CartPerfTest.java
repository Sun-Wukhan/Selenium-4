package api.perf;

import api.model.Product;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Load budget for cart submissions: the shop's only state-building
 * POST short of payment. Same shape as the catalogue runs.
 */
public class CartPerfTest {

    private final PerfClient shop = new PerfClient();
    private final Product item = new Product("Vassily Aloe Attack", 199);

    @Test
    public void cartSubmissionHoldsUpUnderLoad() {
        LoadHarness.Stats stats = LoadHarness.run(
                PerfBudgets.REQUESTS, PerfBudgets.THREADS, PerfBudgets.WARMUP,
                () -> shop.submitCart(item));
        PerfReport.record("POST /cart", stats, PerfBudgets.CART_P95_MS);
        Assert.assertEquals(stats.errors(), 0, "cart submissions should not error under load");
        Assert.assertTrue(stats.p95Millis() < PerfBudgets.CART_P95_MS,
                "cart p95 " + stats.p95Millis() + "ms exceeds budget "
                        + PerfBudgets.CART_P95_MS + "ms");
    }
}
