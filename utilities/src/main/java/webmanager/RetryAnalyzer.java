package webmanager;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Retries a failed test before TestNG settles its result.
 *
 * <p>The suite runs against a live demo shop that re-randomises its
 * catalogue on every load and fails about 5% of payments by design, so a
 * single failure is weak evidence. A failed method is retried up to
 * {@code -Dretry.max} times (default 1); only a failure that survives
 * every attempt is reported as a failure. Attempts are counted per
 * method-and-parameters in a static map, because TestNG may share one
 * analyzer instance across methods, and {@link reporting.FlakeTally}
 * reports which tests needed the extra attempts.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final int MAX_RETRIES = Integer.getInteger("retry.max", 1);
    private static final Map<String, Integer> RETRIES_USED = new ConcurrentHashMap<>();

    @Override
    public boolean retry(ITestResult result) {
        if (result.isSuccess()) {
            return false;
        }
        String key = result.getMethod().getQualifiedName()
                + '|' + Arrays.toString(result.getParameters());
        int used = RETRIES_USED.merge(key, 1, Integer::sum);
        return used <= MAX_RETRIES;
    }
}
