package api.tests;

import api.client.ShopClient;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Contract for the shop's home page. The temperature itself is drawn
 * client-side by an inline script, so the server contract is the page
 * shell: status, content type, heading, and the script that draws a
 * temperature in one of the two decisive bands (1–18 or 35–49) the
 * shopping rules rely on.
 */
public class HomeApiTest {

    private final ShopClient shop = new ShopClient();

    @Test
    public void homePageIsServedAsHtml() {
        Response response = shop.home();
        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.contentType().contains("text/html"),
                "home page should be HTML, was " + response.contentType());
        Assert.assertTrue(response.body().asString().contains("Current temperature"),
                "home page should carry the temperature heading");
    }

    @Test
    public void homePageDrawsTemperatureInADecisiveBand() {
        String body = shop.home().body().asString();
        // The inline script picks one band or the other; both bands must
        // be present in the served page for the shop's routing rule
        // (< 19 moisturizers, > 34 sunscreens) to always have an answer.
        Assert.assertTrue(body.contains("Math.random() * 18 + 1"),
                "winter band (1-18) script missing from home page");
        Assert.assertTrue(body.contains("Math.random() * 15 + 35"),
                "summer band (35-49) script missing from home page");
    }

    @Test
    public void homePageRespondsQuickly() {
        long millis = shop.home().time();
        Assert.assertTrue(millis < 2_000, "home page took " + millis + "ms");
    }
}
