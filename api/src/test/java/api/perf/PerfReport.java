package api.perf;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Appends one Markdown row per load run to
 * {@code target/perf-report.md}, so a CI run leaves a readable
 * performance summary as an artifact next to the pass/fail verdicts.
 */
public final class PerfReport {

    private static final Path REPORT = Path.of("target", "perf-report.md");
    private static boolean headerWritten = false;

    private PerfReport() {
    }

    public static synchronized void record(String label, LoadHarness.Stats stats, long p95BudgetMs) {
        try {
            if (!headerWritten) {
                Files.createDirectories(REPORT.getParent());
                Files.writeString(REPORT,
                        "# Performance run\n\n"
                                + "| Workload | Requests | Errors | Mean | p50 | p95 | p99 | Max | Throughput | p95 budget |\n"
                                + "| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |\n",
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                headerWritten = true;
            }
            Files.writeString(REPORT, String.format(
                    "| %s | %d | %d | %.0fms | %.0fms | %.0fms | %.0fms | %.0fms | %.1f req/s | %dms |\n",
                    label, stats.requests(), stats.errors(), stats.meanMillis(), stats.p50Millis(),
                    stats.p95Millis(), stats.p99Millis(), stats.maxMillis(), stats.requestsPerSecond(),
                    p95BudgetMs), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the performance report", e);
        }
        System.out.println(stats.describe(label) + ", budget p95 " + p95BudgetMs + "ms");
    }
}
