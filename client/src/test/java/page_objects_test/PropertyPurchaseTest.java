package page_objects_test;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import page_objects.CartPage;
import page_objects.CatalogProduct;
import page_objects.HomePage;
import page_objects.MoisturizerPage;
import page_objects.SunscreenPage;
import webmanager.WebManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Property-style purchase checks: instead of two hand-picked scenarios,
 * a seeded generator produces a batch of shopping cases and every case
 * must satisfy the same invariants — the cart lists exactly the products
 * chosen, every cart line carries the catalogue price, and the cart
 * total is the sum of the chosen prices.
 *
 * <p>The shop re-randomises its catalogue on every load, so fixed
 * examples only ever sample a few of the states the suite can meet.
 * Generated cases sample the space instead: any subset of one or two
 * moisturiser ingredients, any subset of one or two sunscreen ratings.
 * The generator is deterministic — the seed is {@code -Dproperty.seed}
 * (default {@value #DEFAULT_SEED}) — so a failing case reproduces
 * exactly by rerunning with the same seed, and the case itself is named
 * in every assertion message.
 *
 * <p>Page objects are local variables, never fields: TestNG runs this
 * class's methods in parallel on one shared instance.
 */
public class PropertyPurchaseTest extends WebManager {

    private static final long DEFAULT_SEED = 20261103L;
    private static final long SEED = Long.getLong("property.seed", DEFAULT_SEED);
    private static final int CASES = 6;
    private static final String[] MOISTURIZER_INGREDIENTS = {"Aloe", "Almond"};
    private static final String[] SUNSCREEN_FILTERS = {"SPF-30", "SPF-40", "SPF-50"};

    /**
     * Generates the moisturiser cases: shuffled subsets of one or two
     * distinct ingredients.
     *
     * @return one ingredient subset per case
     */
    @DataProvider(name = "moisturizerCases")
    public static Object[][] moisturizerCases() {
        return generateCases(new Random(SEED), MOISTURIZER_INGREDIENTS);
    }

    /**
     * Generates the sunscreen cases: shuffled subsets of one or two
     * distinct SPF ratings.
     *
     * @return one rating subset per case
     */
    @DataProvider(name = "sunscreenCases")
    public static Object[][] sunscreenCases() {
        return generateCases(new Random(SEED + 1), SUNSCREEN_FILTERS);
    }

    private static Object[][] generateCases(Random random, String[] filters) {
        Object[][] cases = new Object[CASES][1];
        for (int i = 0; i < CASES; i++) {
            List<String> shuffled = new ArrayList<>(List.of(filters));
            Collections.shuffle(shuffled, random);
            int picks = 1 + random.nextInt(Math.min(2, shuffled.size()));
            cases[i][0] = List.copyOf(shuffled.subList(0, picks));
        }
        return cases;
    }

    /**
     * Buys the cheapest moisturiser for each generated ingredient and
     * checks the cart invariants.
     *
     * @param ingredients the generated ingredient subset for this case
     */
    @Test(dataProvider = "moisturizerCases")
    public void randomMoisturizerCartSatisfiesInvariants(List<String> ingredients) {
        String caseName = "moisturizer case " + ingredients + " (seed " + SEED + ")";
        MoisturizerPage page = new MoisturizerPage(getDriver());
        List<CatalogProduct> chosen = chooseAndAdd(page, ingredients);
        WebManager.captureScreenshot("Added for " + caseName);

        assertCartInvariants(page.openCart(), chosen, caseName);
    }

    /**
     * Buys the cheapest sunscreen for each generated rating and checks
     * the cart invariants.
     *
     * @param filters the generated SPF subset for this case
     */
    @Test(dataProvider = "sunscreenCases")
    public void randomSunscreenCartSatisfiesInvariants(List<String> filters) {
        String caseName = "sunscreen case " + filters + " (seed " + (SEED + 1) + ")";
        SunscreenPage page = HomePage.open(getDriver()).buySunscreens();
        List<CatalogProduct> chosen = chooseAndAdd(page, filters);
        WebManager.captureScreenshot("Added for " + caseName);

        assertCartInvariants(page.openCart(), chosen, caseName);
    }

    private List<CatalogProduct> chooseAndAdd(MoisturizerPage page, List<String> filters) {
        // Pick every product from a single catalogue load first, then add
        // them all from that same load: the shop keeps its cart in page
        // memory, so a reload while choosing the next filter would wipe
        // whatever was already added, and element references from the
        // chosen load stay fresh because nothing reloads between the adds.
        List<CatalogProduct> chosen = page.cheapestMatchingAll(filters);
        for (CatalogProduct product : chosen) {
            page.add(product);
        }
        return chosen;
    }

    private void assertCartInvariants(CartPage cart, List<CatalogProduct> chosen, String caseName) {
        Assert.assertEquals(cart.lineCount(), chosen.size(),
                caseName + ": cart should hold exactly the chosen products");
        int expectedTotal = 0;
        for (CatalogProduct product : chosen) {
            Assert.assertTrue(cart.contains(product.name()),
                    caseName + ": cart should list " + product.name());
            Assert.assertEquals(cart.priceFor(product.name()), product.price(),
                    caseName + ": cart price should match the catalogue for " + product.name());
            expectedTotal += product.price();
        }
        Assert.assertEquals(cart.total(), expectedTotal,
                caseName + ": cart total should be the sum of the chosen prices");
    }
}
