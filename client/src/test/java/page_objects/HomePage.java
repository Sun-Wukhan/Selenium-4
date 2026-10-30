package page_objects;

import application_page_actions.NavBot;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URI;
import java.time.Duration;

/**
 * Landing page at weathershopper.pythonanywhere.com, showing the current
 * temperature and the two catalogues.
 *
 * <p>The site's own task defines the shopping rule this page supports: below
 * 19 degrees buy moisturizers, above 34 degrees buy sunscreens. The page
 * randomises between a winter (1–18) and a summer (35–49) temperature on
 * every load, so the rule always has a definite answer.
 */
public class HomePage {

    private static final By TEMPERATURE = By.id("temperature");
    private static final By BUY_MOISTURIZERS = By.cssSelector("a[href='/moisturizer'] button");
    private static final By BUY_SUNSCREENS = By.cssSelector("a[href='/sunscreen'] button");
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final WebDriver driver;

    private HomePage(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Navigates to the landing page of the site the browser is currently on
     * and waits for the temperature to render.
     *
     * @param driver active browser, on any page of the shop
     * @return the home page
     */
    public static HomePage open(WebDriver driver) {
        String base = URI.create(driver.getCurrentUrl()).resolve("/").toString();
        driver.get(base);
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.presenceOfElementLocated(TEMPERATURE));
        return new HomePage(driver);
    }

    /**
     * Returns the temperature shown on the page, in degrees Celsius.
     *
     * @return the displayed temperature
     */
    public int temperature() {
        String text = new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(TEMPERATURE))
                .getText();
        return MoisturizerPage.parsePrice(text);
    }

    /**
     * Follows the rule for cold weather: under 19 degrees, moisturizers.
     *
     * @return true when the displayed temperature calls for moisturizers
     */
    public boolean shouldBuyMoisturizers() {
        return temperature() < 19;
    }

    /**
     * Follows the rule for hot weather: over 34 degrees, sunscreens.
     *
     * @return true when the displayed temperature calls for sunscreens
     */
    public boolean shouldBuySunscreens() {
        return temperature() > 34;
    }

    /**
     * Clicks "Buy moisturizers" and waits for the catalogue.
     *
     * @return the moisturizer catalogue page
     */
    public MoisturizerPage buyMoisturizers() {
        NavBot.click(driver.findElement(BUY_MOISTURIZERS), "Buy moisturizers");
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.urlContains("/moisturizer"));
        return new MoisturizerPage(driver);
    }

    /**
     * Clicks "Buy sunscreens" and waits for the catalogue.
     *
     * @return the sunscreen catalogue page
     */
    public SunscreenPage buySunscreens() {
        NavBot.click(driver.findElement(BUY_SUNSCREENS), "Buy sunscreens");
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.urlContains("/sunscreen"));
        return new SunscreenPage(driver);
    }
}
