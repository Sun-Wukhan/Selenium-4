package application_page_actions;

import org.codehaus.plexus.util.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import reporting.TestLogger;
import java.io.*;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.function.Function;

public class NavBot {

    WebDriver driver = null;
    public static File cookieFolder = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "cookies").toFile();


    public static void refresh(WebDriver driver) {
        driver.navigate().refresh();
        TestLogger.log("refreshed website:" + driver.getCurrentUrl());
    }


    public static void back(WebDriver driver) {
        TestLogger.log("Currently on website: " + driver.getCurrentUrl());
        driver.navigate().back();
        TestLogger.log("Back to Website:" + driver.getCurrentUrl());

    }

    public static boolean isAlertPresent(WebDriver driver){
        try {
            driver.switchTo().alert();
            return true;
        } catch(NoAlertPresentException E) {
        return false; 
        }
    }

    public static void forward(WebDriver driver) {
        TestLogger.log("Currently on website: " + driver.getCurrentUrl());
        driver.navigate().forward();
        TestLogger.log("Forward to website: " + driver.getCurrentUrl());
    }

    public static void moveToElement(WebDriver driver, WebElement element) {
        Actions actions = new Actions(driver);
        actions.moveToElement(element).build().perform();
        TestLogger.log("Mouse moved to: " + element);

    }

    public static void activeElement(WebDriver driver) {
        TestLogger.log("Currently on: " + driver.getCurrentUrl());
        driver.switchTo().activeElement();
        TestLogger.log("Focused on: " + driver.getCurrentUrl());
    }


    public static void sleep(int timeInMillionSecond) {
        try {
            Thread.sleep(timeInMillionSecond);
            TestLogger.log("Amount of time: " + timeInMillionSecond);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static boolean isClickable(WebElement element) {
        try {
            element.click();
            return true;
        } catch (ElementClickInterceptedException e) {
            return false;
        }
    }


    public static void sendKeys(String webElementName, WebElement element, String textToSend) {
        TestLogger.log("Send text to " + webElementName);
        element.sendKeys(textToSend);
        TestLogger.log("Text Sent to " + webElementName);
    }

    public static void click(WebElement element, String elementName) {
        TestLogger.log("Click " + elementName);
        element.click();
        TestLogger.log("Clicked " + elementName);
    }

    public static void selectByIndex(WebElement element, int i) {
        Select select = new Select(element);
        TestLogger.log("Select option at index " + i);
        select.selectByIndex(i);
        TestLogger.log("Selected option at index " + i);

    }

    public static String getText(WebElement webElement, String webElementName) {

        TestLogger.log("Getting text from " + webElementName);
        String actualText = webElement.getText();
        TestLogger.log("Actual text: " + actualText);
        return actualText;

    }

    public static String getCurrentUrl(String webElementName, WebDriver driver){
        TestLogger.log("Getting text from " + webElementName);
        String url = driver.getCurrentUrl();
        TestLogger.log("Actual URL: " + url);
        return url;

    }


    public static void getAttribute(WebElement webElement, String attributeString) {
        TestLogger.log("Getting attribute from " + webElement);
        String webElementAttribute = webElement.getAttribute(attributeString);
        TestLogger.log("Actual attribute: " + webElementAttribute);

    }


    public static void waitUntilElementIsPresentUsingIDLocator(WebDriver driver, WebElement elementID) {
        WebDriverWait wait = new WebDriverWait(driver, 30);
        wait.until(ExpectedConditions.presenceOfElementLocated((By) elementID));

    }

    public void fluentWait() {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
                .withTimeout(Duration.ofSeconds(30))
                .pollingEvery(Duration.ofSeconds(6))
                .ignoring(NoSuchElementException.class);
    }

    public static void saveCookie(String fileName, WebDriver driver) {
        // create cookie folder if not exist
        if (!cookieFolder.exists()) {
            cookieFolder.mkdirs();
        }
        File file = new File(cookieFolder, fileName);
        // remove cookie file if  existed
        if (file.exists()) {
            file.delete();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Cookie cookie : driver.manage().getCookies()) {
                writer.write((cookie.getName() + ";" + cookie.getValue() + ";" + cookie.getDomain() + ";" + cookie.getPath() + ";" + cookie.getExpiry() + ";" + cookie.isSecure()));
                writer.newLine();


            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void addAllCookies(String fileName, WebDriver driver) {
        File file = new File(cookieFolder, fileName);
        if (!file.exists()) {
            throw new IllegalArgumentException(fileName + " does not exist");
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = null;

            while ((line = reader.readLine()) != null) {
                StringTokenizer token = new StringTokenizer(line, ";");


                while (token.hasMoreTokens()) {
                    String name = token.nextToken();
                    String value = token.nextToken();
                    String domain = token.nextToken();
                    String path = token.nextToken();
                    Date expiry = null;

                    String val;

                    if (!(val = token.nextToken()).equals("null")) {
                        expiry = new Date(val);
                    }
                    boolean isSecure = Boolean.parseBoolean(token.nextToken());
                    Cookie cookie = new Cookie(name,
                            value, domain, path, expiry, isSecure);
                    driver.manage().addCookie(cookie);


                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void isEnabled(WebElement element){
        element.isEnabled();
        if(element.isEnabled()){
            TestLogger.log("Element is enabled");
        } else {
            TestLogger.log("Element is not disabled");
        }
    }

    public static void isDisplayed(WebElement element){
        element.isDisplayed();
        if(element.isDisplayed()){
            TestLogger.log("Element is displayed");
        } else {
            TestLogger.log("Element is not displayed");
        }
    }

    public static void snippetTool(WebDriver driver, String screenshotName){

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

    public static void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
        TestLogger.log("Time waiting: " + millis);
    }

    public static void waitForPageLoad(WebDriver driver) {
        Wait<WebDriver> wait = new WebDriverWait(driver, 30);
        wait.until(new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                TestLogger.log("Current Window State: "
                        + String.valueOf(((JavascriptExecutor) driver).executeScript("return document.readyState")));
                return String
                        .valueOf(((JavascriptExecutor) driver).executeScript("return document.readyState"))
                        .equals("complete");
            }
        });
    }

    public static WebElement waitForElementToBeClickable(WebDriver driver, WebElement webElement){
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(webElement));

        return element;

    }

    public static WebElement waitForElementToBeVisible(WebDriver driver, WebElement webElement, int seconds){
        WebDriverWait wait = new WebDriverWait(driver, 30);

        WebElement element = wait.until(ExpectedConditions.visibilityOf(webElement));

        return element;

    }



    /**
     *
     * @param paths   file paths without file separators that point to the file from /target/test-classes
     * @return a string that represents the absolute path of the file
     */
    public static String getTestResourceLocation(String... paths) {
        char separator = '/';
        String basePath = NavBot.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        StringBuilder builder = new StringBuilder(basePath);
         for(String path: paths){
             builder.append(path);
             builder.append(separator);
         }
       builder.deleteCharAt(builder.length()-1);
         File file = new File(builder.toString());
         if (!file.exists()){
             throw  new IllegalArgumentException(file.getAbsolutePath() +" doesn't exist");
         }
        return  file.getAbsolutePath();
        }
}

