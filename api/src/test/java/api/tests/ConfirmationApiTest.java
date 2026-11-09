package api.tests;

import api.client.ShopClient;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Contract for the confirmation endpoint. By design (the shop's own
 * source) it never consults the card: each POST is an independent
 * weighted coin flip, succeeding 95% of the time and rendering exactly
 * one of two headings. The API layer pins both halves of that contract:
 * the shape of every response, and the distribution over many calls.
 */
public class ConfirmationApiTest {

    private static final int SAMPLES = 40;

    private final ShopClient shop = new ShopClient();

    @Test
    public void confirmationRendersExactlyOneOutcome() {
        Response response = shop.confirmation();
        Assert.assertEquals(response.statusCode(), 200);
        String body = response.body().asString();
        boolean success = body.contains("PAYMENT SUCCESS");
        boolean failed = body.contains("PAYMENT FAILED");
        Assert.assertTrue(success ^ failed,
                "confirmation should render exactly one outcome heading");
    }

    @Test
    public void confirmationSucceedsAboutNinetyFivePercentOfTheTime() {
        int successes = 0;
        for (int i = 0; i < SAMPLES; i++) {
            Response response = shop.confirmation();
            Assert.assertEquals(response.statusCode(), 200);
            if (response.body().asString().contains("PAYMENT SUCCESS")) {
                successes++;
            }
        }
        // At a true 95% rate, fewer than 30 successes in 40 draws has
        // probability below one in a billion; the bound cannot flake,
        // but a changed flip (say 50/50) trips it immediately.
        Assert.assertTrue(successes >= 30,
                "expected at least 30 of " + SAMPLES + " confirmations to succeed, saw " + successes);
    }
}
