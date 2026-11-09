package page_objects;

import application_page_actions.NavBot;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Catalog of moisturizers at weathershopper.pythonanywhere.com/moisturizer.
 */
public class MoisturizerPage {

    private static final By PRODUCT_CARD = By.cssSelector("div.text-center.col-4");
    private static final By PRODUCT_NAME = By.cssSelector("p.font-weight-bold");
    private static final By PRODUCT_PRICE = By.cssSelector("p:not(.font-weight-bold)");
    private static final By ADD_BUTTON = By.cssSelector("button.btn-primary");
    private static final By CART_BUTTON = By.cssSelector("button.nav-link");
    private static final By CART_COUNT = By.id("cart");
    private static final Pattern PRICE_DIGITS = Pattern.compile("(\\d+)");
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_CATALOGUE_LOADS = 6;
    private static final int MAX_JOINT_LOADS = 25;

    private final WebDriver driver;

    /**
     * Binds this page to an open browser already on the moisturizer catalog.
     *
     * @param driver active browser
     */
    public MoisturizerPage(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Returns the cheapest product on the page.
     *
     * @return cheapest catalog card
     */
    public CatalogProduct cheapest() {
        return products().stream()
                .min(Comparator.comparingInt(CatalogProduct::price))
                .orElseThrow(() -> new IllegalStateException("No moisturizers found"));
    }

    /**
     * Returns the cheapest product whose name contains the ingredient.
     *
     * @param ingredient word to match, such as Aloe or Almond
     * @return cheapest matching card
     */
    public CatalogProduct cheapestMatching(String ingredient) {
        return cheapestMatching(ingredient, Set.of());
    }

    /**
     * Returns the cheapest product whose name contains the ingredient, skipping names already chosen.
     *
     * @param ingredient   word to match, such as Aloe or Almond
     * @param excludeNames product names that must not be selected
     * @return cheapest matching card that is not excluded
     */
    public CatalogProduct cheapestMatching(String ingredient, Set<String> excludeNames) {
        String needle = ingredient.toLowerCase(Locale.ROOT);
        // The shop re-randomises its catalogue on every page load, so a given
        // load may contain no matching product at all (seen in CI: a sunscreen
        // load with no SPF-50). Reload for a fresh catalogue before giving up.
        // Note the shop keeps its cart in page memory, so a reload also wipes
        // the cart: callers adding several products must pick them all from a
        // single load first (see cheapestMatchingAll) instead of reloading
        // between adds.
        for (int load = 1; ; load++) {
            var match = products().stream()
                    .filter(product -> product.name().toLowerCase(Locale.ROOT).contains(needle))
                    .filter(product -> !excludeNames.contains(product.name()))
                    .min(Comparator.comparingInt(CatalogProduct::price));
            if (match.isPresent()) {
                return match.get();
            }
            if (load == MAX_CATALOGUE_LOADS) {
                throw new IllegalStateException("No moisturizer matching " + ingredient
                        + " after " + MAX_CATALOGUE_LOADS + " catalogue loads");
            }
            driver.navigate().refresh();
        }
    }

    /**
     * Returns the cheapest product for each filter, all picked from ONE
     * catalogue load. The shop keeps its cart in page memory, so a refresh
     * wipes it: choosing one product, adding it, then reloading to choose
     * the next silently loses the first (seen in CI on Edge: the sunscreen
     * case [SPF-30, SPF-40] ended with one product in the cart). Picking
     * every product from a single load first, then adding them all, keeps
     * the cart intact; loads repeat until one load satisfies every filter.
     *
     * @param filters words to match, such as Aloe and Almond
     * @return one cheapest product per filter, in filter order
     */
    public List<CatalogProduct> cheapestMatchingAll(List<String> filters) {
        for (int load = 1; ; load++) {
            List<CatalogProduct> sample = products();
            List<CatalogProduct> picks = new ArrayList<>();
            Set<String> usedNames = new HashSet<>();
            boolean allMatched = true;
            for (String filter : filters) {
                String needle = filter.toLowerCase(Locale.ROOT);
                var match = sample.stream()
                        .filter(product -> product.name().toLowerCase(Locale.ROOT).contains(needle))
                        .filter(product -> !usedNames.contains(product.name()))
                        .min(Comparator.comparingInt(CatalogProduct::price));
                if (match.isEmpty()) {
                    allMatched = false;
                    break;
                }
                picks.add(match.get());
                usedNames.add(match.get().name());
            }
            if (allMatched) {
                return picks;
            }
            if (load == MAX_JOINT_LOADS) {
                throw new IllegalStateException("No single catalogue load matched all of "
                        + filters + " after " + MAX_JOINT_LOADS + " loads");
            }
            driver.navigate().refresh();
        }
    }

    /**
     * Clicks Add and waits until the cart count reaches the expected
     * total. The catalogue re-renders its cards with scripts, and a click
     * can land while the button it hit is being replaced — the badge then
     * never moves even though the click "worked" (seen in CI: the whole
     * case added cleanly, yet the cart held one product fewer). When the
     * count does not move, the product is found again on the current page
     * and clicked once more; only a second miss fails the add.
     *
     * @param product card to add
     */
    public void add(CatalogProduct product) {
        int expected = cartCount() + 1;
        NavBot.click(product.addButton(), product.name());
        if (awaitCartCount(expected)) {
            return;
        }
        CatalogProduct fresh = findByName(product.name());
        NavBot.click(fresh.addButton(), fresh.name());
        if (!awaitCartCount(expected)) {
            throw new IllegalStateException("Cart count did not reach " + expected
                    + " after adding " + product.name());
        }
    }

    private boolean awaitCartCount(int expected) {
        try {
            new WebDriverWait(driver, TIMEOUT).until(webDriver -> cartCount() >= expected);
            return true;
        } catch (TimeoutException notYet) {
            return false;
        }
    }

    private CatalogProduct findByName(String name) {
        for (int load = 1; ; load++) {
            var match = products().stream()
                    .filter(product -> product.name().equals(name))
                    .findFirst();
            if (match.isPresent()) {
                return match.get();
            }
            if (load == MAX_CATALOGUE_LOADS) {
                throw new IllegalStateException("No product named " + name
                        + " after " + MAX_CATALOGUE_LOADS + " catalogue loads");
            }
            driver.navigate().refresh();
        }
    }

    private int cartCount() {
        // The badge reads "Cart: 2 items"; before the page's scripts run
        // it can carry no number at all, which counts as zero.
        Matcher matcher = PRICE_DIGITS.matcher(cartCountText());
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    /**
     * Opens the cart and waits for the checkout total.
     *
     * @return the cart page
     */
    public CartPage openCart() {
        NavBot.click(driver.findElement(CART_BUTTON), "Cart");
        new WebDriverWait(driver, TIMEOUT).until(ExpectedConditions.visibilityOfElementLocated(By.id("total")));
        return new CartPage(driver);
    }

    /**
     * Returns how many product cards the current catalogue load shows.
     *
     * @return number of cards on the page
     */
    public int productCount() {
        return products().size();
    }

    /**
     * Returns the on-screen rectangle of every product card, in DOM order.
     *
     * @return one rectangle per card
     */
    public List<Rectangle> cardRectangles() {
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.numberOfElementsToBeMoreThan(PRODUCT_CARD, 0));
        List<Rectangle> rectangles = new ArrayList<>();
        for (WebElement card : driver.findElements(PRODUCT_CARD)) {
            rectangles.add(card.getRect());
        }
        return rectangles;
    }

    private List<CatalogProduct> products() {
        // The page's scripts can re-render a card between finding it and
        // reading its text, which surfaces as a stale element. Re-read the
        // catalogue once before giving up.
        for (int attempt = 1; ; attempt++) {
            try {
                return readProducts();
            } catch (StaleElementReferenceException stale) {
                if (attempt == 2) {
                    throw stale;
                }
            }
        }
    }

    private List<CatalogProduct> readProducts() {
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.numberOfElementsToBeMoreThan(PRODUCT_CARD, 0));
        List<CatalogProduct> products = new ArrayList<>();
        for (WebElement card : driver.findElements(PRODUCT_CARD)) {
            String name = card.findElement(PRODUCT_NAME).getText().trim();
            String priceText = card.findElement(PRODUCT_PRICE).getText();
            WebElement addButton = card.findElement(ADD_BUTTON);
            products.add(new CatalogProduct(name, parsePrice(priceText), addButton));
        }
        return products;
    }

    private String cartCountText() {
        return driver.findElement(CART_COUNT).getText().trim();
    }

    /**
     * Reads the first integer from a price label.
     *
     * @param raw text such as {@code Price: Rs. 199} or {@code Total: Rupees 344}
     * @return the numeric price
     */
    static int parsePrice(String raw) {
        Matcher matcher = PRICE_DIGITS.matcher(raw);
        if (!matcher.find()) {
            throw new IllegalStateException("No price in: " + raw);
        }
        return Integer.parseInt(matcher.group(1));
    }
}
