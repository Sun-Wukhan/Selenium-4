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
     * @param zip        any ZIP code; the overlay reveals its ZIP Code field
     *                   only after the card number is entered, and requires it
     * @return the confirmation page the payment submits to
     */
    public ConfirmationPage payWithCard(String email, String cardNumber, String expiry, String cvc, String zip) {
        fillStripeForm(email, cardNumber, expiry, cvc, zip);
        clickPay();
        driver.switchTo().defaultContent();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.urlContains("/confirmation"));
        } catch (org.openqa.selenium.TimeoutException firstAttempt) {
            switchToStripeFrame();
            fillZipWhenRevealed(zip);
            clickPay();
            driver.switchTo().defaultContent();
            try {
                new WebDriverWait(driver, TIMEOUT)
                        .until(ExpectedConditions.urlContains("/confirmation"));
            } catch (org.openqa.selenium.TimeoutException secondAttempt) {
                throw new IllegalStateException("Stripe payment never left the cart page. "
                        + "Overlay state at failure: " + describeOverlay(), secondAttempt);
            }
        }
        return new ConfirmationPage(driver);
    }

    /**
     * Fills the Stripe overlay and submits it once, without waiting for the
     * browser to navigate anywhere. For payment details the overlay itself
     * rejects (a card number that fails its checksum, an expiry in the
     * past), submission never happens: the overlay stays open on the cart
     * page, which is what the caller asserts.
     *
     * @param email      any syntactically valid email address
     * @param cardNumber the card number to enter
     * @param expiry     card expiry, {@code MM/YY}
     * @param cvc        the CVC to enter
     * @param zip        the ZIP code to enter when the overlay asks for one
     */
    public void fillAndSubmitPaymentForm(String email, String cardNumber, String expiry, String cvc, String zip) {
        fillStripeForm(email, cardNumber, expiry, cvc, zip);
        clickPay();
        driver.switchTo().defaultContent();
    }

    /**
     * Returns whether the Stripe Checkout overlay is currently open.
     *
     * @return true while the overlay's iframe is present on the page
     */
    public boolean isStripeOverlayOpen() {
        return !driver.findElements(By.cssSelector(
                "iframe[name='stripe_checkout_app'], iframe.stripe_checkout_app")).isEmpty();
    }

    /**
     * Clicks "Pay with Card" and fills every field of the overlay form,
     * ending inside the overlay's frame ready for {@link #clickPay()}.
     */
    private void fillStripeForm(String email, String cardNumber, String expiry, String cvc, String zip) {
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(PAY_BUTTON))
                .click();
        switchToStripeFrame();
        type(By.cssSelector("input[type='email']"), email);
        typeVerified(By.xpath("//input[@placeholder='Card number']"), cardNumber,
                cardNumber.replaceAll("(\\d{4})(?=\\d)", "$1 "));
        typeVerified(By.xpath("//input[@placeholder='MM / YY']"), expiry,
                expiry.replace("/", " / "));
        type(By.xpath("//input[@placeholder='CVC']"), cvc);
        // Entering the card number makes the overlay reveal a required
        // "ZIP Code" field; leaving it empty blocks the payment silently,
        // with no error message. It can be slow to appear, and a submit
        // gets one retry with the ZIP topped up, mirroring what a human
        // does when the first Pay click only flags the empty field.
        fillZipWhenRevealed(zip);
    }

    /**
     * Describes every input and button in the Stripe overlay, for diagnosing
     * a payment that will not submit.
     *
     * @return a one-line dump of the overlay's controls and their values
     */
    private String describeOverlay() {
        try {
            switchToStripeFrame();
            StringBuilder description = new StringBuilder();
            for (WebElement input : driver.findElements(By.tagName("input"))) {
                description.append("[input type=").append(input.getAttribute("type"))
                        .append(" placeholder=").append(input.getAttribute("placeholder"))
                        .append(" name=").append(input.getAttribute("name"))
                        .append(" id=").append(input.getAttribute("id"))
                        .append(" class=").append(input.getAttribute("class"))
                        .append(" value='").append(input.getAttribute("value"))
                        .append("' displayed=").append(input.isDisplayed()).append("] ");
            }
            for (WebElement button : driver.findElements(By.tagName("button"))) {
                description.append("[button text='").append(button.getText())
                        .append("' displayed=").append(button.isDisplayed())
                        .append(" enabled=").append(button.isEnabled()).append("] ");
            }
            driver.switchTo().defaultContent();
            return description.toString();
        } catch (RuntimeException unavailable) {
            return "unavailable (" + unavailable.getClass().getSimpleName() + ")";
        }
    }

    private void clickPay() {
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(normalize-space(.), 'Pay')]")))
                .click();
    }

    private void fillZipWhenRevealed(String zip) {
        // The ZIP input carries no usable placeholder, name or id (verified
        // against the live overlay: it is labelled "ZIP Code" but matches no
        // attribute locator), so after a brief direct attempt it is found by
        // elimination: the one visible input that is not a known field.
        By zipField = By.xpath("//input[contains(translate(@placeholder, 'zip', 'ZIP'), 'ZIP')"
                + " or @name='zip' or @id='zip']");
        try {
            WebElement field = new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.visibilityOfElementLocated(zipField));
            typeInto(field, zip);
            return;
        } catch (org.openqa.selenium.TimeoutException noDirectMatch) {
            // Fall through to the elimination scan.
        }
        List<By> knownFields = List.of(
                By.cssSelector("input[type='email']"),
                By.xpath("//input[@placeholder='Card number']"),
                By.xpath("//input[@placeholder='MM / YY']"),
                By.xpath("//input[@placeholder='CVC']"));
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15)).until(webDriver -> {
                for (WebElement input : webDriver.findElements(By.tagName("input"))) {
                    if (!input.isDisplayed() || isOneOf(input, knownFields)) {
                        continue;
                    }
                    String type = input.getAttribute("type");
                    if ("hidden".equals(type) || "submit".equals(type) || "button".equals(type)) {
                        continue;
                    }
                    typeInto(input, zip);
                    return zip.equals(input.getAttribute("value"));
                }
                return false;
            });
        } catch (org.openqa.selenium.TimeoutException notAsked) {
            // This overlay configuration does not collect a ZIP code.
        }
    }

    private boolean isOneOf(WebElement input, List<By> locators) {
        for (By locator : locators) {
            for (WebElement known : driver.findElements(locator)) {
                if (known.equals(input)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void typeInto(WebElement field, String text) {
        field.sendKeys(text);
        if (!text.equals(field.getAttribute("value"))) {
            field.clear();
            field.sendKeys(text);
        }
    }

    private void switchToStripeFrame() {
        // The overlay iframe can exist before its document has loaded the
        // form (Firefox in particular), so scan the frames repeatedly until
        // one of them actually exposes the email field.
        try {
            new WebDriverWait(driver, TIMEOUT).until(webDriver -> {
                for (WebElement frame : webDriver.findElements(By.tagName("iframe"))) {
                    webDriver.switchTo().frame(frame);
                    if (!webDriver.findElements(By.cssSelector("input[type='email']")).isEmpty()) {
                        return true;
                    }
                    webDriver.switchTo().defaultContent();
                }
                return false;
            });
        } catch (org.openqa.selenium.TimeoutException timeout) {
            driver.switchTo().defaultContent();
            throw new IllegalStateException(
                    "Stripe Checkout overlay did not expose an email field", timeout);
        }
    }

    private void type(By locator, String text) {
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator))
                .sendKeys(text);
    }

    /**
     * Types into a field whose widget reformats as you type (card number,
     * expiry), then checks the field actually holds the formatted value.
     * The overlay's formatter can drop keystrokes that arrive too fast, so
     * on a mismatch the value is re-typed one character at a time.
     *
     * @param locator       the field to fill
     * @param text          the raw value to type
     * @param expectedValue the formatted value the field should end up with
     */
    private void typeVerified(By locator, String text, String expectedValue) {
        WebElement field = new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
        field.sendKeys(text);
        if (expectedValue.equals(field.getAttribute("value"))) {
            return;
        }
        field.clear();
        for (char character : text.toCharArray()) {
            field.sendKeys(Character.toString(character));
            try {
                Thread.sleep(60);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
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
