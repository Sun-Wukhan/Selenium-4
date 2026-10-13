package reporting;

import com.aventstack.extentreports.ExtentTest;
import org.testng.Reporter;

/**
 * Writes a step to both the TestNG console and the current Extent test.
 */
public final class TestLogger {

    private TestLogger() {
    }

    /**
     * Logs an informational step.
     *
     * @param message text to record
     */
    public static void log(final String message) {
        Reporter.log(message, true);
        ExtentTest test = ExtentTestManager.getTest();
        if (test != null) {
            test.info(message);
        }
    }
}
