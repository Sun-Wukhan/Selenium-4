package api.config;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The context every API and performance report displays about the run:
 * which environment was tested, the target URL, when the run started,
 * and — when the run happens in GitHub Actions — the branch, commit
 * and run it belongs to.
 *
 * <p>The environment name is settable: pass
 * {@code -Dtest.environment="Staging"} (or set the
 * {@code TEST_ENVIRONMENT} variable) to label a run; without either,
 * the name is derived from the target URL, so a report always says
 * what it measured.
 */
public final class TestContext {

    private static final ZonedDateTime STARTED = ZonedDateTime.now();
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    private TestContext() {
    }

    /** The run's start time, formatted for display. */
    public static String startedAt() {
        return STARTED.format(FORMAT);
    }

    /**
     * The environment label for this run: the explicit
     * {@code test.environment} property or {@code TEST_ENVIRONMENT}
     * variable when set, otherwise a name derived from the target URL.
     *
     * @return the environment name shown in the reports
     */
    public static String environmentName() {
        String explicit = System.getProperty("test.environment");
        if (explicit == null || explicit.isBlank()) {
            explicit = System.getenv("TEST_ENVIRONMENT");
        }
        if (explicit != null && !explicit.isBlank()) {
            return explicit.trim();
        }
        String url = ApiConfig.BASE_URL;
        if (url.contains("127.0.0.1") || url.contains("localhost")) {
            return "Self-hosted shop (local / CI)";
        }
        if (url.contains("pythonanywhere")) {
            return "Live demo site (pythonanywhere)";
        }
        return "Custom deployment";
    }

    /**
     * The full context as an ordered label/value map, ready to render
     * into a report. CI entries appear only when the corresponding
     * GitHub Actions variables are present.
     *
     * @return environment, target, timing, platform and CI details
     */
    public static Map<String, String> entries() {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("Environment", environmentName());
        entries.put("Target URL", ApiConfig.BASE_URL);
        entries.put("Run started", startedAt());
        entries.put("Java", System.getProperty("java.version"));
        entries.put("OS", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        putIfPresent(entries, "Branch", "GITHUB_REF_NAME");
        putIfPresent(entries, "Workflow", "GITHUB_WORKFLOW");
        putIfPresent(entries, "Run ID", "GITHUB_RUN_ID");
        String commit = System.getenv("GITHUB_SHA");
        if (commit != null && !commit.isBlank()) {
            entries.put("Commit", commit.length() > 7 ? commit.substring(0, 7) : commit);
        }
        putIfPresent(entries, "Triggered by", "GITHUB_ACTOR");
        return entries;
    }

    private static void putIfPresent(Map<String, String> entries, String label, String variable) {
        String value = System.getenv(variable);
        if (value != null && !value.isBlank()) {
            entries.put(label, value);
        }
    }
}
