package webmanager;

import com.relevantcodes.extentreports.ExtentReports;
import com.relevantcodes.extentreports.LogStatus;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.codehaus.plexus.util.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.opera.OperaOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.*;
import reporting.ExtentManager;
import reporting.ExtentTestManager;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;

public class WebManager {

    public static WebDriver driver = null;
    public static String browserName = System.getProperty("browserName", "chrome");
    public static final String url = System.getProperty("url", "https://www.amazon.ca/ref=nav_logo");
    public static String os = System.getProperty("os", "linux");
    public static  String cloudPlatformName = System.getProperty("cloudPlatformName", "browserstack");
    public static final String AUTOMATE_USERNAME = System.getProperty("AUTOMATE_USERNAME","/////");
    public static final String AUTOMATE_ACCESS_KEY = System.getProperty("AUTOMATE_ACCESS_KEY", "/////");
    public static String platform = System.getProperty("platform", "local");

    /**
     * **************************************************
     * ********** Start Of Reporting Utilities **********
     * **************************************************
    **/

    public static ExtentReports extent;

    @BeforeSuite
    public void extentSetup(ITestContext context) {
        ExtentManager.setOutputDirectory(context);
        extent = ExtentManager.getInstance();

    }

    @BeforeMethod
    public void startExtent(Method method) {
        String className = method.getDeclaringClass().getSimpleName();
        ExtentTestManager.startTest(method.getName());
        ExtentTestManager.getTest().assignCategory(className);
    }

    protected String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    public static void captureFailScreenshot(WebDriver driver, String screenshotName){

        DateFormat df = new SimpleDateFormat("(MM.dd.yyyy-HH mma)");
        Date date = new Date();
        df.format(date);

        File file = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(file, new File(System.getProperty("user.dir")+ "/Failedscreenshots/"+screenshotName+" "+df.format(date)+".png"));
            System.out.println("Screenshot captured");
        } catch (Exception e) {
            System.out.println("Exception while taking screenshot "+e.getMessage());;
        }
    }

    public static void captureScreenshot(WebDriver driver, String screenshotName){

        DateFormat df = new SimpleDateFormat("(MM.dd.yyyy-HH mma)");
        Date date = new Date();
        df.format(date);

        File file = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(file, new File(System.getProperty("user.dir")+ "/screenshots/"+screenshotName+" "+df.format(date)+".png"));
            System.out.println("Screenshot captured");
        } catch (Exception e) {
            System.out.println("Exception while taking screenshot "+e.getMessage());;
        }
    }

    @AfterMethod
    public void afterEachTestMethod(ITestResult result) throws Exception {

        ExtentTestManager.getTest().getTest().setStartedTime(getTime(result.getStartMillis()));
        ExtentTestManager.getTest().getTest().setEndedTime(getTime(result.getEndMillis()));

        for (String group : result.getMethod().getGroups()) {
            ExtentTestManager.getTest().assignCategory(group);
        }

        if (result.getStatus() == ITestResult.SUCCESS) {
            ExtentTestManager.getTest().log(LogStatus.PASS, "Test Passed");
        } else if (result.getStatus() == ITestResult.FAILURE) {
            ExtentTestManager.getTest().log(LogStatus.FAIL, getStackTrace(result.getThrowable()));
            captureFailScreenshot(driver, result.getName());
        } else if (result.getStatus() == ITestResult.SKIP) {
            ExtentTestManager.getTest().log(LogStatus.SKIP, "Test Skipped");
        }

        driver.quit(); 
    }

    @AfterTest
    public void endReport(){
        ExtentTestManager.endTest();
    }



    @AfterSuite
    public void generateReport() {
        extent.close();
    }
    private Date getTime(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar.getTime();
    }

    @BeforeMethod
    public void setUp() throws MalformedURLException {

        if(platform.equals("local")) {
            if (browserName.equals("chrome")) {
                getChromeDriver();
            } else if (browserName.equals("firefox")) {
                getFireFoxDriver();
            } else if (browserName.equals("explorer")) {
                getInternetExplorerDriver();
            } else if (browserName.equals("safari")) {
                getSafariDriver();
            } else if (browserName.equals("opera")) {
                getOperaDriver();
            } else if (browserName.equals("edge")) {
                getEdgeDriver();
            }
        }

        else if(platform.equals("cloud")) {
            if(cloudPlatformName.equals("browserstack")) {
                getDriverForBrowserStack();
            }
            else if(cloudPlatformName.equals("saucelab")){
                getDriverForSauceLab();
            }
        }


        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(60));
        driver.manage().timeouts().scriptTimeout(Duration.ofMinutes(1));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
        driver.manage().window().maximize();
        driver.get(url);

    }

    public static WebDriver getChromeDriver() {

        ChromeDriverService cdservice = new ChromeDriverService.Builder()
                .withSilent(true)
                .usingAnyFreePort()
                .build();

        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.setCapability("browserVersion", "108.0.5359.124");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--disable-infobars"); // disabling infobars
        options.addArguments("--disable-extensions"); // disabling extensions
        options.addArguments("enable-automation");
        options.addArguments("--disable-browser-side-navigation");
        options.addArguments("--disable-gpu");
        options.addArguments("--dns-prefetch-disable");
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.addArguments("--disable-dev-shm-usage"); // overcome limited resource problems
        options.addArguments("--no-sandbox"); // Bypass OS security model
        options.addArguments("--disable-setuid-sandbox");


        if(os.equals("mac")) {
            WebDriverManager.chromedriver().setup();
            WebDriverManager.chromedriver().driverVersion("101.0.4951.64-1").setup();
            driver = new ChromeDriver(cdservice, options);
        }
        else if(os.equals("windows")){
            WebDriverManager.chromedriver().setup();
            WebDriverManager.chromedriver().driverVersion("101.0.4951.64-1").setup();
            driver = new ChromeDriver(options);
        } else if(os.equals("linux")) {
            WebDriverManager.chromedriver().setup();
            WebDriverManager.chromedriver().driverVersion("101.0.4951.64-1").setup();
            driver = new ChromeDriver(options);
        } else {
            System.out.println("No OS found");
        }
     
        return driver;
    }

    public static WebDriver getFireFoxDriver() {

        FirefoxOptions options = new FirefoxOptions();
        options.setAcceptInsecureCerts(true);
        options.setCapability("browserVersion", "latest");
        options.addArguments("--incognito");
        options.addArguments("--start-maximized");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--ignore-ssl-errors");
        options.addArguments("--disable-web-security");
        options.addArguments("--private");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        if(os.equals("mac")) {
            WebDriverManager.firefoxdriver().setup();
            WebDriverManager.firefoxdriver().driverVersion("92.0").setup();
            driver = new FirefoxDriver(options);
        }
        else if(os.equals("windows")){
            WebDriverManager.firefoxdriver().setup();
            WebDriverManager.firefoxdriver().driverVersion("92.0").setup();
            driver = new FirefoxDriver(options);
        }

        return driver;
    }

    public static WebDriver getInternetExplorerDriver() {

        InternetExplorerOptions options = new InternetExplorerOptions();
        options.setAcceptInsecureCerts(true);
        options.setCapability("browserVersion", "latest");
        options.ignoreZoomSettings();
        options.useCreateProcessApiToLaunchIe();
        options.addCommandSwitches("-k");

        if(os.equals("mac")) {
            WebDriverManager.iedriver().setup();
            WebDriverManager.iedriver().driverVersion("11").setup();
            driver = new InternetExplorerDriver(options);
        }

        else if(os.equals("windows")){
            WebDriverManager.firefoxdriver().setup();
            WebDriverManager.firefoxdriver().driverVersion("11").setup();
            driver = new InternetExplorerDriver(options);
        }

        return driver;
    }

    public static WebDriver getSafariDriver() {

        SafariOptions options = new SafariOptions();
        options.setAcceptInsecureCerts(true);
        options.setCapability("browserVersion", "latest");

        if(os.equals("mac")) {
            WebDriverManager.safaridriver().setup();
            WebDriverManager.safaridriver().driverVersion("11").setup();
            driver = new SafariDriver(options);
        }

        else if(os.equals("windows")){
            WebDriverManager.safaridriver().setup();
            WebDriverManager.safaridriver().driverVersion("15").setup();
            driver = new SafariDriver(options);
        }

        return driver;
    }

    public static WebDriver getOperaDriver() {

        OperaOptions options = new OperaOptions();
        options.setAcceptInsecureCerts(true);
        options.setCapability("browserVersion", "latest");

        if(os.equals("mac")) {
            WebDriverManager.operadriver().setup();
            WebDriverManager.operadriver().driverVersion("latest").setup();
            driver = new OperaDriver(options);
        }

        else if(os.equals("windows")){
            WebDriverManager.operadriver().setup();
            WebDriverManager.operadriver().driverVersion("latest").setup();
            driver = new OperaDriver(options);
        }

        return driver;
    }

    public static WebDriver getEdgeDriver() {

        EdgeOptions options = new EdgeOptions();
        options.setAcceptInsecureCerts(true);
        options.setCapability("browserVersion", "latest");
        options.addArguments("--incognito");
        options.addArguments("--start-maximized");
        options.addArguments("ignore-certificate-errors");
        options.addArguments("--ignore-ssl-errors");
        options.addArguments("--disable-web-security");

        if(os.equals("mac")) {
            WebDriverManager.chromedriver().setup();
            WebDriverManager.chromedriver().driverVersion("92.0").setup();
            driver = new EdgeDriver(options);
        }
        else if(os.equals("windows")){
            WebDriverManager.chromedriver().setup();
            WebDriverManager.chromedriver().driverVersion("92.0").setup();
            driver = new EdgeDriver(options);
        }

        return driver;
    }

    public static WebDriver getDriverForBrowserStack() throws MalformedURLException {
        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("os_version", "10");
        caps.setCapability("resolution", "2048x1536");
        caps.setCapability("browser", "Chrome");
        caps.setCapability("browser_version", "latest-beta");
        caps.setCapability("os", "Windows");

        driver = new RemoteWebDriver(new URL("https://" + AUTOMATE_USERNAME + ":" + AUTOMATE_ACCESS_KEY + "@hub-cloud.browserstack.com/wd/hub"),caps );

        return driver;
    }


    public static WebDriver getDriverForSauceLab() throws MalformedURLException {

        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("os_version", "10");
        caps.setCapability("browser", "Chrome");
        caps.setCapability("browser_version", "latest-beta");
        caps.setCapability("os", "Windows");

        driver = new RemoteWebDriver(new URL("http://" + AUTOMATE_USERNAME + ":" + AUTOMATE_ACCESS_KEY + "@ondemand.saucelabs.com:80/wd/hub"), caps);

        return driver;
    }

}
