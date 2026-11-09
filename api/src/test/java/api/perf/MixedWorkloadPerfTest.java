package api.perf;

import api.model.Product;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A shopper-shaped mix rather than one endpoint at a time: roughly
 * 70% catalogue browsing, 20% cart submissions, 10% confirmations —
 * the proportions a real session produces, and the run that catches
 * endpoints degrading each other under shared load.
 */
public class MixedWorkloadPerfTest {

    private final PerfClient shop = new PerfClient();
    private final Product item = new Product("Boris Almond and Honey", 128);
    private final AtomicInteger sequence = new AtomicInteger();

    @Test
    public void mixedWorkloadHoldsUp() {
        LoadHarness.Stats stats = LoadHarness.run(
                PerfBudgets.MIXED_REQUESTS, PerfBudgets.THREADS, PerfBudgets.WARMUP,
                () -> {
                    int slot = Math.floorMod(sequence.getAndIncrement(), 10);
                    if (slot < 7) {
                        return slot % 2 == 0 ? shop.moisturizers() : shop.sunscreens();
                    }
                    if (slot < 9) {
                        return shop.submitCart(item);
                    }
                    return shop.confirmation();
                });
        PerfReport.record("Mixed workload (70/20/10)", stats, PerfBudgets.MIXED_P95_MS);
        Assert.assertEquals(stats.errors(), 0, "mixed workload should not error under load");
        Assert.assertTrue(stats.p95Millis() < PerfBudgets.MIXED_P95_MS,
                "mixed workload p95 " + stats.p95Millis() + "ms exceeds budget "
                        + PerfBudgets.MIXED_P95_MS + "ms");
    }
}
