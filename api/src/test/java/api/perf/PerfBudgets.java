package api.perf;

/**
 * The performance budgets the perf suite asserts, and the load shape
 * used to measure them. Every value is overridable with a
 * {@code -Dperf.*} system property, so a slower environment can loosen
 * a budget without editing code — and a faster one can tighten it.
 *
 * <p>The defaults are regression tripwires, not capacity claims: the
 * shop is a Flask development server, so the budgets sit well above its
 * healthy latencies and exist to catch a change that makes an endpoint
 * dramatically slower or starts erroring under modest concurrency.
 */
public final class PerfBudgets {

    /** Concurrent callers per load run. */
    public static final int THREADS = Integer.getInteger("perf.threads", 12);

    /** Requests per endpoint run (warm-up included). */
    public static final int REQUESTS = Integer.getInteger("perf.requests", 240);

    /** Requests in the mixed-workload run. */
    public static final int MIXED_REQUESTS = Integer.getInteger("perf.mixedRequests", 300);

    /** Leading calls excluded from statistics per run. */
    public static final int WARMUP = Integer.getInteger("perf.warmup", 24);

    /** p95 budget for the catalogue pages, milliseconds. */
    public static final long CATALOG_P95_MS = Long.getLong("perf.catalog.p95ms", 750);

    /** p95 budget for cart submissions, milliseconds. */
    public static final long CART_P95_MS = Long.getLong("perf.cart.p95ms", 750);

    /** p95 budget across the mixed workload, milliseconds. */
    public static final long MIXED_P95_MS = Long.getLong("perf.mixed.p95ms", 750);

    private PerfBudgets() {
    }
}
