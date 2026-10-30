package page_objects_test;

import org.testng.Assert;
import org.testng.annotations.Test;
import page_objects.HomePage;
import webmanager.WebManager;

/**
 * Checks the home page: the temperature it shows and the catalogue each of
 * its buy buttons leads to.
 *
 * <p>The site's rule is to shop for moisturizers below 19 degrees and for
 * sunscreens above 34 degrees, and the page only ever serves a temperature
 * in one of those two bands (1–18 or 35–49), so the rule always applies.
 */
public class HomePageTest extends WebManager {

    /**
     * Reads the temperature, applies the site's rule and follows it to the
     * catalogue it selects, which must offer products.
     */
    @Test
    public void temperatureSelectsTheCatalogue() {
        HomePage home = HomePage.open(getDriver());
        int temperature = home.temperature();

        Assert.assertTrue(temperature < 19 || temperature > 34,
                "site should serve a decisive temperature, was " + temperature);
        if (home.shouldBuyMoisturizers()) {
            Assert.assertFalse(home.shouldBuySunscreens(),
                    "a winter temperature must not also select sunscreens");
            Assert.assertNotNull(home.buyMoisturizers().cheapest(),
                    "moisturizer catalogue should offer products");
            Assert.assertTrue(getDriver().getCurrentUrl().contains("/moisturizer"),
                    "rule should land on the moisturizer catalogue");
        } else {
            Assert.assertTrue(home.shouldBuySunscreens(),
                    "a summer temperature must select sunscreens");
            Assert.assertNotNull(home.buySunscreens().cheapestMatching("SPF-50"),
                    "sunscreen catalogue should offer SPF-50 products");
            Assert.assertTrue(getDriver().getCurrentUrl().contains("/sunscreen"),
                    "rule should land on the sunscreen catalogue");
        }
    }

    /**
     * Both buy buttons reach a working catalogue, whichever one the
     * temperature rule did not select.
     */
    @Test
    public void bothBuyButtonsReachTheirCatalogues() {
        Assert.assertNotNull(HomePage.open(getDriver()).buyMoisturizers().cheapest(),
                "moisturizer catalogue should offer products");
        Assert.assertTrue(getDriver().getCurrentUrl().contains("/moisturizer"));

        Assert.assertNotNull(HomePage.open(getDriver()).buySunscreens().cheapest(),
                "sunscreen catalogue should offer products");
        Assert.assertTrue(getDriver().getCurrentUrl().contains("/sunscreen"));
    }
}
