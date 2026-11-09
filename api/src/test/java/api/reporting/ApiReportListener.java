package api.reporting;

import api.config.ApiConfig;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds the API suite's HTML report (Extent Reports, the same look as
 * the browser suite's report) at {@code target/api-report/index.html}.
 * Every test gets an entry carrying its description, its outcome and
 * duration, and — via {@link ExchangeCapture} — the HTTP exchanges it
 * made, with response body excerpts for the first few. Reporting must
 * never break the suite: any reporting failure is printed and the
 * tests carry on without a report.
 */
public class ApiReportListener implements ITestListener {

    private static final int MAX_CODE_BLOCKS = 3;

    private ExtentReports extent;
    private final Map<String, ExtentTest> tests = new ConcurrentHashMap<>();

    @Override
    public void onStart(ITestContext context) {
        try {
            var spark = new com.aventstack.extentreports.reporter.ExtentSparkReporter(
                    new File("target/api-report/index.html"));
            spark.config().setReportName("Weather Shopper - API Contract Report");
            spark.config().setDocumentTitle("Weather Shopper API Tests");
            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Shop base URL", ApiConfig.BASE_URL);
            extent.setSystemInfo("Java", System.getProperty("java.version"));
            extent.setSystemInfo("OS", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
            extent.setSystemInfo("Suite", context.getSuite().getName());
        } catch (RuntimeException | Error failure) {
            System.err.println("API report could not be initialised: " + failure);
            extent = null;
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        if (extent == null) {
            return;
        }
        String className = result.getTestClass().getRealClass().getSimpleName();
        String description = result.getMethod().getDescription();
        ExtentTest test = extent.createTest(className + "." + result.getMethod().getMethodName(),
                description == null ? "" : description);
        test.assignCategory(className);
        tests.put(key(result), test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = tests.remove(key(result));
        if (test == null) {
            return;
        }
        logExchanges(test);
        test.pass("Passed in " + durationMillis(result) + " ms");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = tests.remove(key(result));
        if (test == null) {
            return;
        }
        logExchanges(test);
        if (result.getThrowable() != null) {
            test.fail(result.getThrowable());
        }
        test.fail("Failed after " + durationMillis(result) + " ms");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = tests.remove(key(result));
        if (test == null) {
            return;
        }
        logExchanges(test);
        test.skip("Skipped");
    }

    @Override
    public void onFinish(ITestContext context) {
        if (extent != null) {
            extent.flush();
        }
    }

    private void logExchanges(ExtentTest test) {
        List<ExchangeCapture.Exchange> exchanges = ExchangeCapture.drain();
        int blocks = 0;
        for (ExchangeCapture.Exchange exchange : exchanges) {
            test.info(exchange.summary());
            if (!exchange.bodyExcerpt().isBlank() && blocks < MAX_CODE_BLOCKS) {
                test.info(MarkupHelper.createCodeBlock(exchange.bodyExcerpt()));
                blocks++;
            }
        }
        if (exchanges.size() > MAX_CODE_BLOCKS && blocks == MAX_CODE_BLOCKS) {
            test.info("Response bodies shown for the first " + MAX_CODE_BLOCKS
                    + " of " + exchanges.size() + " exchanges.");
        }
    }

    private static long durationMillis(ITestResult result) {
        return result.getEndMillis() - result.getStartMillis();
    }

    private static String key(ITestResult result) {
        return result.getTestClass().getRealClass().getName() + "#" + result.getMethod().getMethodName()
                + "@" + System.identityHashCode(result);
    }
}
