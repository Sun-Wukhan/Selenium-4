package api.tests;

import api.client.ShopClient;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Routing contract: the pages that exist, the ones that do not, and the
 * methods each endpoint accepts.
 */
public class RoutingApiTest {

    private final ShopClient shop = new ShopClient();

    @Test(description = "GET /about returns 200")
    public void aboutPageIsServed() {
        Assert.assertEquals(shop.about().statusCode(), 200);
    }

    @Test(description = "An unknown path returns 404")
    public void unknownPathIs404() {
        Assert.assertEquals(shop.get("/no-such-page").statusCode(), 404);
    }

    @Test(description = "POST to a catalogue page is rejected with 405")
    public void cataloguesRejectPost() {
        Assert.assertEquals(shop.post("/moisturizer").statusCode(), 405);
        Assert.assertEquals(shop.post("/sunscreen").statusCode(), 405);
    }

    @Test(description = "GET /confirmation is rejected with 405")
    public void confirmationRejectsGet() {
        Assert.assertEquals(shop.get("/confirmation").statusCode(), 405);
    }
}
