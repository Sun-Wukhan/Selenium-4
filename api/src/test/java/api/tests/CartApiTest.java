package api.tests;

import api.client.ShopClient;
import api.model.Product;
import api.parsing.CatalogPageParser;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Contract for the cart endpoint: it accepts one form field per product
 * (name to price), lists exactly those products, and totals them.
 */
public class CartApiTest {

    private final ShopClient shop = new ShopClient();

    @Test(description = "POST /cart lists the submitted items and totals their prices (128 + 199 = 327)")
    public void cartListsSubmittedItemsAndTotalsThem() {
        List<Product> items = List.of(
                new Product("Boris Almond and Honey", 128),
                new Product("Vassily Aloe Attack", 199));
        Response response = shop.submitCart(items);
        Assert.assertEquals(response.statusCode(), 200);
        String body = response.body().asString();
        Assert.assertEquals(CatalogPageParser.parseCartItems(body),
                List.of("Boris Almond and Honey", "Vassily Aloe Attack"));
        Assert.assertEquals(CatalogPageParser.parseCartTotal(body), 327,
                "cart total should be the sum of the submitted prices");
    }

    @Test(description = "The cart total trusts the submitted price, not the catalogue pool price")
    public void cartTotalIsTheSubmittedPriceNotThePoolPrice() {
        // The endpoint trusts the posted prices (the browser posts the
        // catalogue price it rendered). Pin that behaviour: a drift
        // here is exactly what the UI cart assertions cannot localise.
        Response response = shop.submitCart(List.of(new Product("Vassily Aloe Attack", 1)));
        Assert.assertEquals(CatalogPageParser.parseCartTotal(response.body().asString()), 1);
    }

    @Test(description = "An empty cart submission totals 0")
    public void emptyCartTotalsZero() {
        Response response = shop.submitCart(List.of());
        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(CatalogPageParser.parseCartTotal(response.body().asString()), 0);
        Assert.assertTrue(CatalogPageParser.parseCartItems(response.body().asString()).isEmpty());
    }

    @Test(description = "GET /cart loads with 200 even without a prior submission")
    public void cartPageLoadsWithoutSubmission() {
        Response response = shop.get("/cart");
        Assert.assertEquals(response.statusCode(), 200);
    }
}
