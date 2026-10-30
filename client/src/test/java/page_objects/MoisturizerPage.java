package page_objects;

import application_page_actions.NavBot;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
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
        return products().stream()
                .filter(product -> product.name().toLowerCase(Locale.ROOT).contains(needle))
                .filter(product -> !excludeNames.contains(product.name()))
                .min(Comparator.comparingInt(CatalogProduct::price))
                .orElseThrow(() -> new IllegalStateException("No moisturizer matching " + ingredient));
    }

    /**
     * Clicks Add and waits until the cart count changes.
     *
     * @param product card to add
     */
    public void add(CatalogProduct product) {
        String before = cartCountText();
        NavBot.click(product.addButton(), product.name());
        new WebDriverWait(driver, TIMEOUT).until(webDriver -> !cartCountText().equals(before));
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
