package reporting;

import com.aventstack.extentreports.ExtentTest;
import org.testng.ITestResult;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tallies which tests needed a retry, and publishes the tally to the
 * Extent report at the end of the suite.
 *
 * <p>{@link webmanager.WebManager} feeds every finished attempt in here.
 * A result TestNG marks as retried ({@code wasRetried}) counts as one
 * retried attempt; the method's final result then decides whether it was
 * a flaky pass (green only after a retry) or a hard failure (still red
 * after every attempt). The tally keeps those two apart, because they
 * need opposite responses: a flaky pass is a stability problem to fix,
 * a hard failure is a product or framework bug.
 */
public final class FlakeTally {

    private static final Map<String, Integer> RETRIED_ATTEMPTS = new ConcurrentHashMap<>();
    private static final Set<String> PASSED_AFTER_RETRY = ConcurrentHashMap.newKeySet();
    private static final Set<String> FAILED_AFTER_RETRIES = ConcurrentHashMap.newKeySet();

    private FlakeTally() {
    }

    /**
     * Records one finished attempt.
     *
     * @param result the TestNG result that just finished
     */
    public static void record(ITestResult result) {
        String name = result.getMethod().getQualifiedName();
        if (result.wasRetried()) {
            RETRIED_ATTEMPTS.merge(name, 1, Integer::sum);
            return;
        }
        if (!RETRIED_ATTEMPTS.containsKey(name)) {
            return;
        }
        if (result.getStatus() == ITestResult.SUCCESS) {
            PASSED_AFTER_RETRY.add(name);
        } else if (result.getStatus() == ITestResult.FAILURE) {
            FAILED_AFTER_RETRIES.add(name);
        }
    }

    /**
     * Summarises the tally in one line.
     *
     * @return human-readable counts of retried attempts and their outcomes
     */
    public static String summary() {
        int attempts = RETRIED_ATTEMPTS.values().stream().mapToInt(Integer::intValue).sum();
        if (attempts == 0) {
            return "No test needed a retry.";
        }
        return attempts + " retried attempt(s): "
                + PASSED_AFTER_RETRY.size() + " flaky pass(es), "
                + FAILED_AFTER_RETRIES.size() + " still failing after retries.";
    }

    /**
     * Adds the tally to the Extent report as system info and as its own
     * report entry. Call once, after the last test and before the report
     * is flushed.
     */
    public static void publish() {
        ExtentManager.getInstance().setSystemInfo("Flake tally", summary());
        ExtentTest entry = ExtentManager.getInstance()
                .createTest("Flake tally", "Retry outcomes for the whole suite");
        if (RETRIED_ATTEMPTS.isEmpty()) {
            entry.pass("No test needed a retry in this run.");
            return;
        }
        RETRIED_ATTEMPTS.forEach((name, count) ->
                entry.info(name + " needed " + count + " retried attempt(s)"));
        PASSED_AFTER_RETRY.forEach(name ->
                entry.warning("Flaky pass: " + name + " passed only after a retry"));
        FAILED_AFTER_RETRIES.forEach(name ->
                entry.fail("Hard failure: " + name + " still failed after every retry"));
        if (FAILED_AFTER_RETRIES.isEmpty()) {
            entry.warning(summary());
        } else {
            entry.fail(summary());
        }
    }
}
