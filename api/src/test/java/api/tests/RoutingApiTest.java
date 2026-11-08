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

    @Test
    public void aboutPageIsServed() {
        Assert.assertEquals(shop.about().statusCode(), 200);
    }

    @Test
    public void unknownPathIs404() {
        Assert.assertEquals(shop.get("/no-such-page").statusCode(), 404);
    }

    @Test
    public void cataloguesRejectPost() {
        Assert.assertEquals(shop.post("/moisturizer").statusCode(), 405);
        Assert.assertEquals(shop.post("/sunscreen").statusCode(), 405);
    }

    @Test
    public void confirmationRejectsGet() {
        Assert.assertEquals(shop.get("/confirmation").statusCode(), 405);
    }
}
