package page_objects_test;

import org.testng.Assert;
import org.testng.annotations.Test;
import page_objects.CartPage;
import page_objects.CatalogProduct;
import page_objects.ConfirmationPage;
import page_objects.HomePage;
import page_objects.MoisturizerPage;
import reporting.TestLogger;
import webmanager.WebManager;

import java.util.Set;

/**
 * The full buy journey: catalogue, cart and payment, ending on the
 * confirmation page.
 *
 * <p>Pays with Stripe's test card {@code 4242 4242 4242 4242}. The shop is
 * built to fail about 5% of payments at random (its confirmation page says
 * so), so a {@code PAYMENT FAILED} result is retried once with a fresh cart
 * before the test gives up; two designed failures in a row have a 0.25%
 * chance.
 */
public class CheckoutTest extends WebManager {

    private static final String EMAIL = "qa.shopper@example.com";
    private static final String TEST_CARD = "4242424242424242";
    private static final String EXPIRY = "12/34";
    private static final String CVC = "123";
    private static final String ZIP = "12345";
    private static final int MAX_ATTEMPTS = 2;

    /**
     * Buys the cheapest Aloe and Almond moisturizers, pays for them and
     * checks the confirmation page reports a successful payment.
     */
    @Test
    public void fullCheckoutWithStripeTestCard() {
        ConfirmationPage confirmation = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            MoisturizerPage moisturizerPage = attempt == 1
                    ? new MoisturizerPage(getDriver())
                    : HomePage.open(getDriver()).buyMoisturizers();

            CatalogProduct aloe = moisturizerPage.cheapestMatching("Aloe");
            CatalogProduct almond = moisturizerPage.cheapestMatching("Almond", Set.of(aloe.name()));
            moisturizerPage.add(aloe);
            moisturizerPage.add(almond);

            CartPage cart = moisturizerPage.openCart();
            Assert.assertEquals(cart.total(), aloe.price() + almond.price(),
                    "cart total should be the sum of both prices before paying");

            confirmation = cart.payWithCard(EMAIL, TEST_CARD, EXPIRY, CVC, ZIP);
            if (confirmation.isSuccess()) {
                break;
            }
            TestLogger.log("Payment failed on attempt " + attempt
                    + " (the shop fails ~5% of payments by design); starting over");
        }

        Assert.assertNotNull(confirmation, "payment should have been attempted");
        Assert.assertTrue(confirmation.isSuccess(),
                "payment should succeed, page said: " + confirmation.heading());
        Assert.assertEquals(confirmation.heading(), "PAYMENT SUCCESS");
        Assert.assertTrue(confirmation.message().contains("Your payment was successful"),
                "confirmation message should report success, was: " + confirmation.message());
    }
}
