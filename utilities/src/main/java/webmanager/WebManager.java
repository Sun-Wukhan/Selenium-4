package webmanager;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.manager.SeleniumManager;
import org.openqa.selenium.manager.SeleniumManagerOutput;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import reporting.ExtentManager;
import reporting.ExtentTestManager;

import java.io.File;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;

/**
 * Starts a local browser with Selenium Manager and records each test in Extent.
 */
public class WebManager {

    private static final ThreadLocal<WebDriver> DRIVERS = new ThreadLocal<>();
    private static final String URL = System.getProperty(
            "url", "https://weathershopper.pythonanywhere.com/moisturizer");
    private static final String BROWSER = System.getProperty("browser", "chrome");
    private static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("headless", "false"));
    private static final Duration PAGE_LOAD_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Returns the browser selected with {@code -Dbrowser}.
     *
     * @return chrome, firefox, or edge
     */
    public static String browser() {
        return BROWSER;
    }

    /**
     * Returns the driver for the current thread.
     *
     * @return the active WebDriver
     */
    public static WebDriver getDriver() {
        return DRIVERS.get();
    }

    /**
     * Creates the Extent report before any test runs.
     */
    @BeforeSuite
    public void extentSetup() {
        ExtentManager.getInstance();
    }

    /**
     * Opens the catalog and starts the Extent test for this method.
     *
     * @param method the test method about to run
     */
    @BeforeMethod
    public void setUp(Method method) {
        WebDriver webDriver = createDriver();
        DRIVERS.set(webDriver);
        webDriver.manage().timeouts().pageLoadTimeout(PAGE_LOAD_TIMEOUT);
        if (!HEADLESS) {
            webDriver.manage().window().maximize();
        }
        webDriver.get(URL);

        String className = method.getDeclaringClass().getSimpleName();
        ExtentTest test = ExtentTestManager.startTest(method.getName(), className);
        test.assignCategory(className);
    }

    /**
     * Embeds a named screenshot of the current page in the Extent report.
     *
     * @param name caption shown beside the image
     */
    public static void captureScreenshot(String name) {
        ExtentTest test = ExtentTestManager.getTest();
        String base64 = screenshotBase64();
        if (test == null || base64 == null) {
            return;
        }
        test.info(name, MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
    }

    /**
     * Attaches a pass or fail screenshot, then closes the browser.
     *
     * @param result TestNG result for the method that just finished
     */
    @AfterMethod(alwaysRun = true)
    public void afterEachTestMethod(ITestResult result) {
        try {
            attachResult(result);
        } finally {
            WebDriver webDriver = getDriver();
            if (webDriver != null) {
                webDriver.quit();
            }
            DRIVERS.remove();
            ExtentTestManager.endTest();
        }
    }

    /**
     * Flushes the Extent HTML report after the suite.
     */
    @AfterSuite(alwaysRun = true)
    public void generateReport() {
        ExtentManager.flush();
    }

    private void attachResult(ITestResult result) {
        ExtentTest test = ExtentTestManager.getTest();
        if (test == null) {
            return;
        }
        for (String group : result.getMethod().getGroups()) {
            test.assignCategory(group);
        }
        String base64 = screenshotBase64();
        if (result.getStatus() == ITestResult.FAILURE) {
            if (base64 == null) {
                test.fail(result.getThrowable());
            } else {
                test.fail(result.getThrowable(),
                        MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
            }
        } else if (result.getStatus() == ITestResult.SUCCESS) {
            if (base64 == null) {
                test.pass("Test passed");
            } else {
                test.pass("Test passed",
                        MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
            }
        } else if (result.getStatus() == ITestResult.SKIP) {
            test.skip("Test skipped");
        }
    }

    private static String screenshotBase64() {
        WebDriver webDriver = getDriver();
        if (!(webDriver instanceof TakesScreenshot)) {
            return null;
        }
        try {
            return ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BASE64);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static WebDriver createDriver() {
        return switch (BROWSER.toLowerCase()) {
            case "chrome" -> createChromeDriver();
            case "firefox" -> createFirefoxDriver();
            case "edge" -> createEdgeDriver();
            default -> throw new IllegalArgumentException("Unsupported browser: " + BROWSER);
        };
    }

    private static WebDriver createChromeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.addArguments("--window-size=1440,900");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        if (HEADLESS) {
            options.addArguments("--headless=new");
        }
        SeleniumManagerOutput.Result managed = resolveBrowser("chrome");
        applyBrowserBinary(options, managed);
        ChromeDriverService service = new ChromeDriverService.Builder()
                .usingDriverExecutable(new File(managed.getDriverPath()))
                .build();
        return new ChromeDriver(service, options);
    }

    private static WebDriver createFirefoxDriver() {
        FirefoxOptions options = new FirefoxOptions();
        options.setAcceptInsecureCerts(true);
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.addArguments("--width=1440");
        options.addArguments("--height=900");
        if (HEADLESS) {
            options.addArguments("-headless");
        }
        SeleniumManagerOutput.Result managed = resolveBrowser("firefox");
        applyBrowserBinary(options, managed);
        GeckoDriverService service = new GeckoDriverService.Builder()
                .usingDriverExecutable(new File(managed.getDriverPath()))
                .build();
        return new FirefoxDriver(service, options);
    }

    private static WebDriver createEdgeDriver() {
        EdgeOptions options = new EdgeOptions();
        options.setAcceptInsecureCerts(true);
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.addArguments("--window-size=1440,900");
        if (HEADLESS) {
            options.addArguments("--headless=new");
        }
        SeleniumManagerOutput.Result managed = resolveBrowser("edge");
        applyBrowserBinary(options, managed);
        EdgeDriverService service = new EdgeDriverService.Builder()
                .usingDriverExecutable(new File(managed.getDriverPath()))
                .build();
        return new EdgeDriver(service, options);
    }

    /**
     * Resolves a browser and driver, ignoring outdated drivers already on PATH.
     *
     * @param browserName chrome, firefox, or edge
     * @return Selenium Manager paths for that browser
     */
    private static SeleniumManagerOutput.Result resolveBrowser(String browserName) {
        SeleniumManagerOutput.Result result = SeleniumManager.getInstance().getBinaryPaths(List.of(
                "--browser", browserName,
                "--skip-driver-in-path"));
        String driverPath = result.getDriverPath();
        if (driverPath == null || driverPath.isBlank()) {
            throw new IllegalStateException(
                    "Selenium Manager did not return a " + browserName + " driver: " + result.getMessage());
        }
        return result;
    }

    /**
     * Points Chrome or Edge at the binary Selenium Manager selected.
     *
     * @param options Chrome or Edge options
     * @param managed resolved browser and driver paths
     */
    private static void applyBrowserBinary(ChromiumOptions<?> options, SeleniumManagerOutput.Result managed) {
        String browserPath = managed.getBrowserPath();
        if (browserPath != null && !browserPath.isBlank()) {
            options.setBinary(browserPath);
        }
    }

    /**
     * Points Firefox at the binary Selenium Manager selected.
     *
     * @param options Firefox options
     * @param managed resolved browser and driver paths
     */
    private static void applyBrowserBinary(FirefoxOptions options, SeleniumManagerOutput.Result managed) {
        String browserPath = managed.getBrowserPath();
        if (browserPath != null && !browserPath.isBlank()) {
            options.setBinary(browserPath);
        }
    }
}
