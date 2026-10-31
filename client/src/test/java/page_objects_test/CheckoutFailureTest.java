package page_objects_test;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;
import page_objects.CartPage;
import page_objects.CatalogProduct;
import page_objects.MoisturizerPage;
import webmanager.WebManager;

import java.time.Duration;

/**
 * The payment failure paths that can be exercised deterministically.
 *
 * <p>The shop's {@code /confirmation} endpoint never charges the card at
 * all — its source flips a weighted coin ({@code random.uniform(0,1) < 0.95})
 * and renders success or failure from that, so no card, including Stripe's
 * decline cards, can force the {@code PAYMENT FAILED} page. What a card
 * <em>does</em> control deterministically is the Stripe overlay's own
 * validation: a number that fails the card checksum, or an expiry in the
 * past, is rejected inside the overlay and the payment is never submitted.
 * These tests assert exactly that — after submitting bad details the
 * browser is still on the cart with the overlay open.
 *
 * <p>Page objects are local variables, never fields: TestNG runs this
 * class's methods in parallel on one shared instance.
 */
public class CheckoutFailureTest extends WebManager {

    private static final String EMAIL = "qa.shopper@example.com";
    private static final String GOOD_CARD = "4242424242424242";
    private static final String BAD_CHECKSUM_CARD = "4242424242424241";
    private static final String FUTURE_EXPIRY = "12/34";
    private static final String PAST_EXPIRY = "12/20";
    private static final String CVC = "123";
    private static final String ZIP = "12345";

    /**
     * Pays with a card number whose checksum digit is wrong and checks the
     * overlay rejects it without submitting the payment.
     */
    @Test
    public void incorrectCardNumberIsRejectedByTheOverlay() {
        CartPage cart = cartWithCheapestMoisturizer();

        cart.fillAndSubmitPaymentForm(EMAIL, BAD_CHECKSUM_CARD, FUTURE_EXPIRY, CVC, ZIP);

        assertPaymentNeverSubmitted(cart);
    }

    /**
     * Pays with an expiry date in the past and checks the overlay rejects
     * it without submitting the payment.
     */
    @Test
    public void expiredCardIsRejectedByTheOverlay() {
        CartPage cart = cartWithCheapestMoisturizer();

        cart.fillAndSubmitPaymentForm(EMAIL, GOOD_CARD, PAST_EXPIRY, CVC, ZIP);

        assertPaymentNeverSubmitted(cart);
    }

    /**
     * Builds a cart holding the single cheapest moisturizer.
     *
     * @return the cart page for this method's own browser
     */
    private CartPage cartWithCheapestMoisturizer() {
        MoisturizerPage moisturizerPage = new MoisturizerPage(getDriver());
        CatalogProduct product = moisturizerPage.cheapest();
        moisturizerPage.add(product);
        return moisturizerPage.openCart();
    }

    /**
     * Checks that no payment was submitted: the browser never reaches the
     * confirmation page and the Stripe overlay is still open on the cart.
     *
     * @param cart the cart whose payment was just attempted
     */
    private void assertPaymentNeverSubmitted(CartPage cart) {
        boolean navigated;
        try {
            new WebDriverWait(getDriver(), Duration.ofSeconds(5))
                    .until(ExpectedConditions.urlContains("/confirmation"));
            navigated = true;
        } catch (TimeoutException expected) {
            navigated = false;
        }
        Assert.assertFalse(navigated,
                "a payment the overlay rejects must not reach the confirmation page, URL: "
                        + getDriver().getCurrentUrl());
        Assert.assertTrue(cart.isStripeOverlayOpen(),
                "the Stripe overlay should still be open after rejecting the payment");
    }
}
