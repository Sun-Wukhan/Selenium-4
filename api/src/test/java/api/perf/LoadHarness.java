package api.perf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;

/**
 * A minimal concurrent load driver: fires a fixed number of calls from
 * a fixed pool of threads, all released at once, and reduces the
 * timings to percentiles. Deliberately dependency-free — the point of
 * this layer is that the load generation is ordinary Java code in the
 * repo, readable and hackable, not an external tool's DSL.
 */
public final class LoadHarness {

    /**
     * Immutable summary of one load run.
     *
     * @param requests          measured requests (warm-up excluded)
     * @param errors            calls that threw or returned status &gt;= 400
     * @param meanMillis        mean latency
     * @param p50Millis         median latency
     * @param p95Millis         95th-percentile latency
     * @param p99Millis         99th-percentile latency
     * @param maxMillis         slowest single call
     * @param requestsPerSecond measured throughput over the run's wall time
     */
    public record Stats(int requests, int errors, double meanMillis, double p50Millis,
                        double p95Millis, double p99Millis, double maxMillis,
                        double requestsPerSecond) {

        String describe(String label) {
            return String.format(
                    "%s: %d reqs, %d errors, mean %.0fms, p50 %.0fms, p95 %.0fms, p99 %.0fms, max %.0fms, %.1f req/s",
                    label, requests, errors, meanMillis, p50Millis, p95Millis, p99Millis,
                    maxMillis, requestsPerSecond);
        }
    }

    private LoadHarness() {
    }

    /**
     * Runs {@code totalRequests} calls on {@code threads} threads.
     *
     * @param totalRequests total calls to make, warm-up included
     * @param threads       concurrent callers
     * @param warmup        leading calls (spread across threads) excluded
     *                      from the statistics, so connection setup and
     *                      first-hit costs do not pollute the budgets
     * @param call          one call; returns the HTTP status code
     * @return the run's statistics
     */
    public static Stats run(int totalRequests, int threads, int warmup, IntSupplier call) {
        List<Long> latenciesNanos = Collections.synchronizedList(new ArrayList<>());
        List<Integer> statuses = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(totalRequests);
        for (int i = 0; i < totalRequests; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    long started = System.nanoTime();
                    int status;
                    try {
                        status = call.getAsInt();
                    } catch (Throwable failure) {
                        // REST Assured sneaky-throws checked IOExceptions
                        // (connection refused, resets) past its RuntimeException
                        // signatures; a transport failure is an error sample,
                        // never a reason to drop the call from the statistics.
                        status = -1;
                    }
                    latenciesNanos.add(System.nanoTime() - started);
                    statuses.add(status);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        long wallStart = System.nanoTime();
        startGate.countDown();
        try {
            if (!done.await(10, TimeUnit.MINUTES)) {
                throw new IllegalStateException("Load run did not finish within 10 minutes");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the load run", e);
        } finally {
            pool.shutdownNow();
        }
        double wallSeconds = (System.nanoTime() - wallStart) / 1_000_000_000.0;

        // Drop the warm-up calls (the earliest submissions) from the stats.
        List<Long> measured = new ArrayList<>(latenciesNanos);
        List<Integer> measuredStatuses = new ArrayList<>(statuses);
        if (warmup > 0 && measured.size() > warmup) {
            measured = measured.subList(warmup, measured.size());
            measuredStatuses = measuredStatuses.subList(warmup, measuredStatuses.size());
        }
        List<Double> millis = measured.stream().map(n -> n / 1_000_000.0).sorted().toList();
        int errors = (int) measuredStatuses.stream().filter(s -> s < 200 || s >= 400).count();
        double mean = millis.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return new Stats(
                millis.size(),
                errors,
                mean,
                percentile(millis, 50),
                percentile(millis, 95),
                percentile(millis, 99),
                millis.isEmpty() ? 0 : millis.get(millis.size() - 1),
                wallSeconds == 0 ? 0 : millis.size() / wallSeconds);
    }

    private static double percentile(List<Double> sortedMillis, int percentile) {
        if (sortedMillis.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(percentile / 100.0 * sortedMillis.size()) - 1;
        return sortedMillis.get(Math.max(0, Math.min(index, sortedMillis.size() - 1)));
    }
}
