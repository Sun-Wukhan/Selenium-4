package reporting;

import com.aventstack.extentreports.ExtentTest;

/**
 * Keeps the Extent test for the current thread.
 */
public final class ExtentTestManager {

    private static final ThreadLocal<ExtentTest> TESTS = new ThreadLocal<>();

    private ExtentTestManager() {
    }

    /**
     * Starts an Extent test and stores it for this thread.
     *
     * @param testName    method name shown in the report
     * @param description class or scenario description
     * @return the created test
     */
    public static synchronized ExtentTest startTest(String testName, String description) {
        ExtentTest test = ExtentManager.getInstance().createTest(testName, description);
        TESTS.set(test);
        return test;
    }

    /**
     * Returns the Extent test started on this thread.
     *
     * @return the current test, or null when none has been started
     */
    public static synchronized ExtentTest getTest() {
        return TESTS.get();
    }

    /**
     * Drops the current thread's test so the next method starts clean.
     */
    public static synchronized void endTest() {
        TESTS.remove();
    }
}
