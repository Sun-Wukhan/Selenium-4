package page_objects_test;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import page_objects.CartPage;
import page_objects.CatalogProduct;
import page_objects.MoisturizerPage;
import webmanager.WebManager;

import java.util.Set;

/**
 * Checks moisturizer prices on the catalog against the cart.
 */
public class PurchaseTest extends WebManager {

    private MoisturizerPage moisturizerPage;

    /**
     * Opens the page object against the browser started by {@link WebManager}.
     */
    @BeforeMethod
    public void openCatalog() {
        moisturizerPage = new MoisturizerPage(getDriver());
    }

    /**
     * Adds the cheapest moisturizer and checks that the cart line uses the catalog price.
     */
    @Test
    public void catalogPriceMatchesCartLine() {
        CatalogProduct product = moisturizerPage.cheapest();
        moisturizerPage.add(product);
        WebManager.captureScreenshot("Added " + product.name());

        CartPage cart = moisturizerPage.openCart();
        WebManager.captureScreenshot("Cart for " + product.name());

        Assert.assertTrue(cart.contains(product.name()), "cart should list " + product.name());
        Assert.assertEquals(cart.priceFor(product.name()), product.price(), "cart price should match the catalog");
    }

    /**
     * Adds the cheapest aloe and almond moisturizers and checks the cart total.
     */
    @Test
    public void cheapestAloeAndAlmondMatchCartTotal() {
        CatalogProduct aloe = moisturizerPage.cheapestMatching("Aloe");
        CatalogProduct almond = moisturizerPage.cheapestMatching("Almond", Set.of(aloe.name()));
        moisturizerPage.add(aloe);
        moisturizerPage.add(almond);
        WebManager.captureScreenshot("Added " + aloe.name() + " and " + almond.name());

        CartPage cart = moisturizerPage.openCart();
        WebManager.captureScreenshot("Cart total");

        Assert.assertTrue(cart.contains(aloe.name()), "cart should list " + aloe.name());
        Assert.assertTrue(cart.contains(almond.name()), "cart should list " + almond.name());
        Assert.assertEquals(cart.total(), aloe.price() + almond.price(), "cart total should be the sum of both prices");
    }
}
