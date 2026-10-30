package page_objects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Checkout cart that lists each added moisturizer and the rupee total.
 */
public class CartPage {

    private static final By LINE = By.cssSelector("table.table tbody tr");
    private static final By TOTAL = By.id("total");
    private static final By PAY_BUTTON = By.cssSelector("button.stripe-button-el");
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final WebDriver driver;

    /**
     * Binds this page to a browser already showing the cart.
     *
     * @param driver active browser
     */
    public CartPage(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Returns whether a product name is listed in the cart.
     *
     * @param productName name captured from the catalog
     * @return true when a matching row is present
     */
    public boolean contains(String productName) {
        return lines().stream().anyMatch(line -> line.name().equals(productName));
    }

    /**
     * Returns the cart price for a product.
     *
     * @param productName name captured from the catalog
     * @return price in rupees
     */
    public int priceFor(String productName) {
        return lines().stream()
                .filter(line -> line.name().equals(productName))
                .map(CartLine::price)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cart has no line for " + productName));
    }

    /**
     * Returns how many line items the cart lists.
     *
     * @return number of rows in the cart table
     */
    public int lineCount() {
        return lines().size();
    }

    /**
     * Returns whether the cart lists no items.
     *
     * @return true when the cart table is empty
     */
    public boolean isEmpty() {
        return lines().isEmpty();
    }

    /**
     * Pays through the Stripe Checkout overlay the cart page embeds.
     *
     * <p>Clicks the "Pay with Card" button Stripe renders, fills the overlay
     * form (which lives in a Stripe iframe) and submits it. Use Stripe's test
     * card {@code 4242 4242 4242 4242}, any future expiry and any CVC. The
     * shop randomly fails about 5% of payments by design; check
     * {@link ConfirmationPage#isSuccess()} on the result.
     *
     * @param email      any syntactically valid email address
     * @param cardNumber a Stripe test card number
     * @param expiry     card expiry, {@code MM/YY} or later in the future
     * @param cvc        any three-digit code
     * @param zip        any ZIP code; the overlay does not currently ask for
     *                   one, so this is only typed when a ZIP field appears
     * @return the confirmation page the payment submits to
     */
    public ConfirmationPage payWithCard(String email, String cardNumber, String expiry, String cvc, String zip) {
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(PAY_BUTTON))
                .click();
        switchToStripeFrame();
        type(By.cssSelector("input[type='email']"), email);
        type(By.xpath("//input[@placeholder='Card number']"), cardNumber);
        type(By.xpath("//input[@placeholder='MM / YY']"), expiry);
        type(By.xpath("//input[@placeholder='CVC']"), cvc);
        typeIfPresent(By.xpath("//input[@placeholder='ZIP']"), zip);
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(normalize-space(.), 'Pay')]")))
                .click();
        driver.switchTo().defaultContent();
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.urlContains("/confirmation"));
        return new ConfirmationPage(driver);
    }

    private void switchToStripeFrame() {
        new WebDriverWait(driver, TIMEOUT).until(webDriver ->
                !webDriver.findElements(By.cssSelector(
                        "iframe[name='stripe_checkout_app'], iframe.stripe_checkout_app")).isEmpty());
        for (WebElement frame : driver.findElements(By.tagName("iframe"))) {
            driver.switchTo().frame(frame);
            if (!driver.findElements(By.cssSelector("input[type='email']")).isEmpty()) {
                return;
            }
            driver.switchTo().defaultContent();
        }
        throw new IllegalStateException("Stripe Checkout overlay did not expose an email field");
    }

    private void type(By locator, String text) {
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator))
                .sendKeys(text);
    }

    private void typeIfPresent(By locator, String text) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.visibilityOfElementLocated(locator))
                    .sendKeys(text);
        } catch (org.openqa.selenium.TimeoutException notAsked) {
            // The overlay did not ask for this field; nothing to type.
        }
    }

    /**
     * Returns the checkout total in rupees.
     *
     * @return value from the total label
     */
    public int total() {
        return MoisturizerPage.parsePrice(driver.findElement(TOTAL).getText());
    }

    private List<CartLine> lines() {
        List<CartLine> lines = new ArrayList<>();
        for (WebElement row : driver.findElements(LINE)) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (cells.size() < 2) {
                continue;
            }
            String name = cells.get(0).getText().trim();
            int price = MoisturizerPage.parsePrice(cells.get(1).getText());
            lines.add(new CartLine(name, price));
        }
        return lines;
    }

    /**
     * One row in the cart table.
     *
     * @param name  item name
     * @param price item price in rupees
     */
    private record CartLine(String name, int price) {
    }
}
