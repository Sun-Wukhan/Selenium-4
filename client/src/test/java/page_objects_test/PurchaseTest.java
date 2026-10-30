package page_objects_test;

import org.testng.Assert;
import org.testng.annotations.Test;
import page_objects.CartPage;
import page_objects.CatalogProduct;
import page_objects.MoisturizerPage;
import webmanager.WebManager;

import java.util.Set;

/**
 * Checks moisturizer prices on the catalog against the cart.
 *
 * <p>Page objects are local variables, never fields: TestNG runs this
 * class's methods in parallel on one shared instance, so a page-object
 * field would be overwritten by the other method's setup and both methods
 * would end up driving (and quitting) the same browser.
 */
public class PurchaseTest extends WebManager {

    /**
     * Adds the cheapest moisturizer and checks that the cart line uses the catalog price.
     */
    @Test
    public void catalogPriceMatchesCartLine() {
        MoisturizerPage moisturizerPage = new MoisturizerPage(getDriver());
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
        MoisturizerPage moisturizerPage = new MoisturizerPage(getDriver());
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
