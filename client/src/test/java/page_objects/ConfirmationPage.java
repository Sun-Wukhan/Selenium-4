package page_objects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Payment result at weathershopper.pythonanywhere.com/confirmation.
 *
 * <p>The heading is the outcome: {@code PAYMENT SUCCESS} or
 * {@code PAYMENT FAILED}. The shop is built to fail roughly 5% of payments
 * at random, so a failed payment is a property of the site, not of the card;
 * callers that need a success should be prepared to shop and pay again.
 */
public class ConfirmationPage {

    private static final By HEADING = By.tagName("h2");
    private static final By MESSAGE = By.cssSelector("p.text-justify");
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final WebDriver driver;

    /**
     * Binds this page to a browser that has submitted a payment, waiting
     * until the result heading renders.
     *
     * @param driver active browser
     */
    public ConfirmationPage(WebDriver driver) {
        this.driver = driver;
        new WebDriverWait(driver, TIMEOUT).until(webDriver -> {
            String heading = headingText();
            return heading.contains("PAYMENT SUCCESS") || heading.contains("PAYMENT FAILED");
        });
    }

    /**
     * Returns the result heading, {@code PAYMENT SUCCESS} or
     * {@code PAYMENT FAILED}.
     *
     * @return the heading text
     */
    public String heading() {
        return headingText();
    }

    /**
     * Returns the follow-up message shown under the heading.
     *
     * @return the message text
     */
    public String message() {
        return driver.findElement(MESSAGE).getText().trim();
    }

    /**
     * Returns whether the payment went through.
     *
     * @return true on {@code PAYMENT SUCCESS}
     */
    public boolean isSuccess() {
        return headingText().contains("PAYMENT SUCCESS");
    }

    private String headingText() {
        return driver.findElement(HEADING).getText().trim();
    }
}
