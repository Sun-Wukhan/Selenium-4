package api.tests;

import api.client.ShopClient;
import api.data.ShopCatalog;
import api.model.Product;
import api.parsing.CatalogPageParser;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contract for the two catalogue endpoints. Each page is a fresh random
 * sample of the canonical pool, so the assertions are the sampling
 * invariants: exactly six cards, every product a pool member at its
 * canonical price, no duplicates within a page.
 */
public class CatalogApiTest {

    private final ShopClient shop = new ShopClient();

    @Test
    public void moisturizerPageIsAPoolSample() {
        Response response = shop.moisturizers();
        Assert.assertEquals(response.statusCode(), 200);
        List<Product> products = CatalogPageParser.parse(response.body().asString());
        assertPoolSample(products, ShopCatalog.MOISTURIZERS, "moisturizer");
    }

    @Test
    public void moisturizerPageAlwaysOffersBothIngredients() {
        List<Product> products = CatalogPageParser.parse(shop.moisturizers().body().asString());
        // The pool is five Aloe and five Almond; a six-product sample
        // cannot avoid either ingredient. The UI suite's cheapest-Aloe
        // and cheapest-Almond tasks silently depend on this.
        Assert.assertTrue(products.stream().anyMatch(p -> p.name().contains("Aloe")),
                "moisturizer page should always include an Aloe product");
        Assert.assertTrue(products.stream().anyMatch(p -> p.name().contains("Almond")),
                "moisturizer page should always include an Almond product");
    }

    @Test
    public void sunscreenPageIsAPoolSample() {
        Response response = shop.sunscreens();
        Assert.assertEquals(response.statusCode(), 200);
        List<Product> products = CatalogPageParser.parse(response.body().asString());
        assertPoolSample(products, ShopCatalog.SUNSCREENS, "sunscreen");
    }

    @Test
    public void sunscreenPageNamesCarryTheirRating() {
        List<Product> products = CatalogPageParser.parse(shop.sunscreens().body().asString());
        for (Product product : products) {
            Assert.assertTrue(product.name().toLowerCase().contains("spf"),
                    "sunscreen name should carry its SPF rating: " + product.name());
        }
    }

    private void assertPoolSample(List<Product> products, Map<String, Integer> pool, String label) {
        Assert.assertEquals(products.size(), ShopCatalog.PAGE_SIZE,
                label + " page should show exactly " + ShopCatalog.PAGE_SIZE + " products");
        Set<String> names = products.stream().map(Product::name).collect(Collectors.toSet());
        Assert.assertEquals(names.size(), products.size(),
                label + " page should not repeat a product");
        for (Product product : products) {
            Assert.assertTrue(pool.containsKey(product.name()),
                    label + " page returned a product outside the known pool: " + product.name());
            Assert.assertEquals(product.price(), pool.get(product.name()).intValue(),
                    label + " price drifted from the pool for " + product.name());
        }
    }
}
