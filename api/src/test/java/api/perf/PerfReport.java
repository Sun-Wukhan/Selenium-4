package api.perf;

import api.config.ApiConfig;
import api.config.TestContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes the performance results after every load run, in two forms
 * that are regenerated from the runs recorded so far:
 *
 * <ul>
 *   <li>{@code target/perf-report.md} — the Markdown table CI posts to
 *       the run summary;</li>
 *   <li>{@code target/perf-report.html} — a self-contained styled page
 *       (published to GitHub Pages by CI) with the run's environment,
 *       per-workload latency detail and p95-versus-budget bars.</li>
 * </ul>
 */
public final class PerfReport {

    private record Row(String label, LoadHarness.Stats stats, long p95BudgetMs) {
        boolean withinBudget() {
            return stats.errors() == 0 && stats.p95Millis() <= p95BudgetMs;
        }
    }

    private static final Path MARKDOWN = Path.of("target", "perf-report.md");
    private static final Path HTML = Path.of("target", "perf-report.html");
    private static final List<Row> ROWS = new ArrayList<>();
    private static final ZonedDateTime STARTED = ZonedDateTime.now();

    private PerfReport() {
    }

    public static synchronized void record(String label, LoadHarness.Stats stats, long p95BudgetMs) {
        ROWS.add(new Row(label, stats, p95BudgetMs));
        try {
            Files.createDirectories(MARKDOWN.getParent());
            Files.writeString(MARKDOWN, markdown());
            Files.writeString(HTML, html());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the performance report", e);
        }
        System.out.println(stats.describe(label) + ", budget p95 " + p95BudgetMs + "ms");
    }

    private static String markdown() {
        StringBuilder out = new StringBuilder("# Performance run\n\n"
                + "- Environment: " + TestContext.environmentName() + "\n"
                + "- Target: " + ApiConfig.BASE_URL + "\n"
                + "- Started: " + TestContext.startedAt() + "\n\n"
                + "| Workload | Requests | Errors | Mean | p50 | p95 | p99 | Max | Throughput | p95 budget |\n"
                + "| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |\n");
        for (Row row : ROWS) {
            LoadHarness.Stats s = row.stats();
            out.append(String.format(Locale.ROOT,
                    "| %s | %d | %d | %.0fms | %.0fms | %.0fms | %.0fms | %.0fms | %.1f req/s | %dms |%n",
                    row.label(), s.requests(), s.errors(), s.meanMillis(), s.p50Millis(),
                    s.p95Millis(), s.p99Millis(), s.maxMillis(), s.requestsPerSecond(),
                    row.p95BudgetMs()));
        }
        return out.toString();
    }

    private static String html() {
        long totalRequests = ROWS.stream().mapToLong(row -> row.stats().requests()).sum();
        long totalErrors = ROWS.stream().mapToLong(row -> row.stats().errors()).sum();
        boolean allWithin = ROWS.stream().allMatch(Row::withinBudget);
        StringBuilder out = new StringBuilder();
        out.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Weather Shopper - Performance Report</title>
                <style>
                  :root { color-scheme: dark; }
                  body { margin: 0; font-family: -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                         background: #0f141a; color: #e6edf3; }
                  .wrap { max-width: 960px; margin: 0 auto; padding: 32px 20px 48px; }
                  h1 { font-size: 26px; margin: 0 0 4px; }
                  .sub { color: #9aa7b4; font-size: 14px; margin-bottom: 24px; }
                  .cards { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 28px; }
                  .card { background: #171e26; border: 1px solid #26303c; border-radius: 10px;
                          padding: 14px 18px; min-width: 140px; }
                  .card .k { color: #9aa7b4; font-size: 12px; text-transform: uppercase; letter-spacing: .06em; }
                  .card .v { font-size: 22px; font-weight: 600; margin-top: 4px; }
                  .ok { color: #3fb950; } .bad { color: #f85149; }
                  table { width: 100%; border-collapse: collapse; background: #171e26;
                          border: 1px solid #26303c; border-radius: 10px; overflow: hidden; }
                  th, td { padding: 10px 12px; text-align: right; font-size: 14px; }
                  th { background: #1d2631; color: #9aa7b4; font-weight: 600; }
                  td:first-child, th:first-child { text-align: left; }
                  tr + tr td { border-top: 1px solid #232d38; }
                  .bar { height: 8px; background: #26303c; border-radius: 4px; margin-top: 6px; width: 180px; }
                  .bar i { display: block; height: 100%; border-radius: 4px; background: #3fb950; }
                  .bar i.over { background: #f85149; }
                  .chip { font-size: 12px; font-weight: 700; padding: 2px 8px; border-radius: 999px; }
                  .chip.pass { background: #12351f; color: #3fb950; }
                  .chip.fail { background: #3d1719; color: #f85149; }
                  .foot { color: #9aa7b4; font-size: 13px; margin-top: 24px; line-height: 1.5; }
                  code { background: #1d2631; padding: 1px 5px; border-radius: 4px; }
                </style>
                </head>
                <body>
                <div class="wrap">
                """);
        out.append("<h1>Weather Shopper &mdash; Performance Report</h1>\n");
        out.append("<div class=\"sub\">Generated ")
                .append(STARTED.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")))
                .append(" &middot; environment <strong>").append(escape(TestContext.environmentName())).append("</strong>")
                .append(" &middot; target <code>").append(escape(ApiConfig.BASE_URL)).append("</code>")
                .append(" &middot; Java ").append(escape(System.getProperty("java.version")))
                .append(" &middot; ").append(escape(System.getProperty("os.name")))
                .append("</div>\n");
        out.append("<div class=\"cards\">")
                .append(card("Environment", escape(TestContext.environmentName())))
                .append(card("Workloads", String.valueOf(ROWS.size())))
                .append(card("Requests", String.valueOf(totalRequests)))
                .append(card("Errors", String.valueOf(totalErrors)))
                .append(card("Load shape", PerfBudgets.THREADS + " threads, " + PerfBudgets.WARMUP + " warm-up"))
                .append(card("Verdict", allWithin
                        ? "<span class=\"ok\">ALL WITHIN BUDGET</span>"
                        : "<span class=\"bad\">BUDGET EXCEEDED</span>"))
                .append("</div>\n");
        java.util.Map<String, String> context = TestContext.entries();
        if (context.containsKey("Commit")) {
            out.append("<div class=\"sub\">CI: branch ").append(escape(context.getOrDefault("Branch", "")))
                    .append(" &middot; commit <code>").append(escape(context.get("Commit"))).append("</code>")
                    .append(" &middot; run ").append(escape(context.getOrDefault("Run ID", "")))
                    .append("</div>\n");
        }
        out.append("<table>\n<tr><th>Workload</th><th>Requests</th><th>Errors</th><th>Mean</th>"
                + "<th>p50</th><th>p95 vs budget</th><th>p99</th><th>Max</th><th>Throughput</th><th>Verdict</th></tr>\n");
        for (Row row : ROWS) {
            LoadHarness.Stats s = row.stats();
            double pct = row.p95BudgetMs() == 0 ? 100.0
                    : Math.min(100.0, s.p95Millis() / row.p95BudgetMs() * 100.0);
            out.append(String.format(Locale.ROOT,
                    "<tr><td>%s</td><td>%d</td><td>%d</td><td>%.0f ms</td><td>%.0f ms</td>"
                            + "<td>%.0f ms of %d ms<div class=\"bar\"><i style=\"width:%.1f%%\"%s></i></div></td>"
                            + "<td>%.0f ms</td><td>%.0f ms</td><td>%.1f req/s</td>"
                            + "<td><span class=\"chip %s\">%s</span></td></tr>%n",
                    escape(row.label()), s.requests(), s.errors(), s.meanMillis(), s.p50Millis(),
                    s.p95Millis(), row.p95BudgetMs(), pct, row.withinBudget() ? "" : " class=\"over\"",
                    s.p99Millis(), s.maxMillis(), s.requestsPerSecond(),
                    row.withinBudget() ? "pass" : "fail", row.withinBudget() ? "PASS" : "FAIL"));
        }
        out.append("</table>\n");
        out.append("<p class=\"foot\">Budgets are regression tripwires, not capacity claims: "
                + "the shop is a Flask development server, so the p95 budgets sit well above its healthy "
                + "latencies and exist to catch an endpoint becoming dramatically slower or erroring under "
                + "modest concurrency. Warm-up requests are excluded from every statistic above. "
                + "Full per-run numbers also land in <code>perf-report.md</code>.</p>\n");
        out.append("</div>\n</body>\n</html>\n");
        return out.toString();
    }

    private static String card(String key, String valueHtml) {
        return "<div class=\"card\"><div class=\"k\">" + key + "</div><div class=\"v\">" + valueHtml + "</div></div>";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
