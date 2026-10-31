package page_objects_test;

import org.testng.Assert;
import org.testng.annotations.Test;
import page_objects.CartPage;
import page_objects.CatalogProduct;
import page_objects.ConfirmationPage;
import page_objects.MoisturizerPage;
import webmanager.WebManager;

/**
 * The payment failure path, exercised on purpose.
 *
 * <p>{@link CheckoutTest} proves a good card succeeds; these tests pay with
 * Stripe's deterministic decline cards, which pass the overlay's own checks
 * but are always refused when the shop tries to charge them:
 * {@code 4000 0000 0000 0002} (generic decline) and
 * {@code 4000 0000 0000 9995} (insufficient funds). Unlike the shop's random
 * ~5% failure, a decline card fails every time, so the {@code PAYMENT FAILED}
 * page can be asserted directly with no retry.
 *
 * <p>Page objects are local variables, never fields: TestNG runs this
 * class's methods in parallel on one shared instance.
 */
public class CheckoutFailureTest extends WebManager {

    private static final String EMAIL = "qa.shopper@example.com";
    private static final String GENERIC_DECLINE_CARD = "4000000000000002";
    private static final String INSUFFICIENT_FUNDS_CARD = "4000000000009995";
    private static final String EXPIRY = "12/34";
    private static final String CVC = "123";
    private static final String ZIP = "12345";

    /**
     * Pays with the generic decline card and checks the confirmation page
     * reports a failed payment.
     */
    @Test
    public void genericallyDeclinedCardShowsPaymentFailed() {
        ConfirmationPage confirmation = payWithCard(GENERIC_DECLINE_CARD);

        Assert.assertFalse(confirmation.isSuccess(),
                "a declined card must not report success, page said: " + confirmation.heading());
        Assert.assertEquals(confirmation.heading(), "PAYMENT FAILED");
    }

    /**
     * Pays with the insufficient-funds card and checks the confirmation page
     * reports a failed payment.
     */
    @Test
    public void insufficientFundsCardShowsPaymentFailed() {
        ConfirmationPage confirmation = payWithCard(INSUFFICIENT_FUNDS_CARD);

        Assert.assertFalse(confirmation.isSuccess(),
                "an insufficient-funds card must not report success, page said: "
                        + confirmation.heading());
        Assert.assertEquals(confirmation.heading(), "PAYMENT FAILED");
    }

    /**
     * Buys the single cheapest moisturizer and pays for it with the given card.
     *
     * @param cardNumber the card to pay with
     * @return the confirmation page the payment lands on
     */
    private ConfirmationPage payWithCard(String cardNumber) {
        MoisturizerPage moisturizerPage = new MoisturizerPage(getDriver());
        CatalogProduct product = moisturizerPage.cheapest();
        moisturizerPage.add(product);

        CartPage cart = moisturizerPage.openCart();
        Assert.assertEquals(cart.total(), product.price(),
                "cart total should be the single product's price before paying");

        return cart.payWithCard(EMAIL, cardNumber, EXPIRY, CVC, ZIP);
    }
}
