package reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import webmanager.WebManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Owns the single ExtentReports session and writes the Spark HTML report.
 */
public final class ExtentManager {

    private static ExtentReports extent;

    private ExtentManager() {
    }

    /**
     * Returns the shared report, creating the Spark HTML file on first use.
     *
     * @return the process-wide ExtentReports instance
     */
    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            Path report = Path.of(System.getProperty("user.dir"), "target", "extent-report", "index.html");
            try {
                Files.createDirectories(report.getParent());
            } catch (IOException exception) {
                throw new IllegalStateException("Could not create Extent report directory", exception);
            }
            ExtentSparkReporter spark = new ExtentSparkReporter(report.toFile());
            spark.config().setDocumentTitle("Weather Shopper");
            spark.config().setReportName("Moisturizer tests");
            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Environment", "QA");
            extent.setSystemInfo("Browser", WebManager.browser());
        }
        return extent;
    }

    /**
     * Writes the HTML report to disk.
     */
    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
        }
    }
}
