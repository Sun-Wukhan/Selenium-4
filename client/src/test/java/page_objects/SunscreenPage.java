package page_objects;

import org.openqa.selenium.WebDriver;

/**
 * Catalog of sunscreens at weathershopper.pythonanywhere.com/sunscreen.
 *
 * <p>The sunscreen catalog uses exactly the same card markup, cart button and
 * add-to-cart behaviour as the moisturizer catalog, so this page inherits the
 * whole implementation from {@link MoisturizerPage}. The difference is only
 * in what the tests look for: product names carry their rating
 * ({@code SPF-30}, {@code SPF-40}, {@code SPF-50}), so
 * {@code cheapestMatching("SPF-50")} finds the cheapest SPF-50 sunscreen.
 */
public class SunscreenPage extends MoisturizerPage {

    /**
     * Binds this page to an open browser already on the sunscreen catalog.
     *
     * @param driver active browser
     */
    public SunscreenPage(WebDriver driver) {
        super(driver);
    }
}
