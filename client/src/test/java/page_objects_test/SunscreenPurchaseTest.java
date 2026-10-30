package page_objects_test;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import page_objects.CartPage;
import page_objects.CatalogProduct;
import page_objects.HomePage;
import page_objects.SunscreenPage;
import webmanager.WebManager;

/**
 * Checks sunscreen prices on the catalog against the cart, mirroring
 * {@link PurchaseTest} for the other catalogue. This is the site's sunscreen
 * task: the cheapest SPF-50 and the cheapest SPF-30.
 */
public class SunscreenPurchaseTest extends WebManager {

    private SunscreenPage sunscreenPage;

    /**
     * Reaches the sunscreen catalogue the way a shopper does: from the home
     * page's "Buy sunscreens" button.
     */
    @BeforeMethod
    public void openCatalog() {
        sunscreenPage = HomePage.open(getDriver()).buySunscreens();
    }

    /**
     * Adds the cheapest SPF-50 sunscreen and checks that the cart line uses
     * the catalog price.
     */
    @Test
    public void cheapestSpf50MatchesCartLine() {
        CatalogProduct product = sunscreenPage.cheapestMatching("SPF-50");
        sunscreenPage.add(product);

        CartPage cart = sunscreenPage.openCart();

        Assert.assertTrue(cart.contains(product.name()), "cart should list " + product.name());
        Assert.assertEquals(cart.priceFor(product.name()), product.price(),
                "cart price should match the catalog");
    }

    /**
     * Adds the cheapest SPF-50 and SPF-30 sunscreens and checks the cart total.
     */
    @Test
    public void cheapestSpf50AndSpf30MatchCartTotal() {
        CatalogProduct spf50 = sunscreenPage.cheapestMatching("SPF-50");
        CatalogProduct spf30 = sunscreenPage.cheapestMatching("SPF-30");
        sunscreenPage.add(spf50);
        sunscreenPage.add(spf30);

        CartPage cart = sunscreenPage.openCart();

        Assert.assertTrue(cart.contains(spf50.name()), "cart should list " + spf50.name());
        Assert.assertTrue(cart.contains(spf30.name()), "cart should list " + spf30.name());
        Assert.assertEquals(cart.total(), spf50.price() + spf30.price(),
                "cart total should be the sum of both prices");
    }
}
