package page_objects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Checkout cart that lists each added moisturizer and the rupee total.
 */
public class CartPage {

    private static final By LINE = By.cssSelector("table.table tbody tr");
    private static final By TOTAL = By.id("total");

    private final WebDriver driver;

    /**
     * Binds this page to a browser already showing the cart.
     *
     * @param driver active browser
     */
    public CartPage(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Returns whether a product name is listed in the cart.
     *
     * @param productName name captured from the catalog
     * @return true when a matching row is present
     */
    public boolean contains(String productName) {
        return lines().stream().anyMatch(line -> line.name().equals(productName));
    }

    /**
     * Returns the cart price for a product.
     *
     * @param productName name captured from the catalog
     * @return price in rupees
     */
    public int priceFor(String productName) {
        return lines().stream()
                .filter(line -> line.name().equals(productName))
                .map(CartLine::price)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cart has no line for " + productName));
    }

    /**
     * Returns the checkout total in rupees.
     *
     * @return value from the total label
     */
    public int total() {
        return MoisturizerPage.parsePrice(driver.findElement(TOTAL).getText());
    }

    private List<CartLine> lines() {
        List<CartLine> lines = new ArrayList<>();
        for (WebElement row : driver.findElements(LINE)) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (cells.size() < 2) {
                continue;
            }
            String name = cells.get(0).getText().trim();
            int price = MoisturizerPage.parsePrice(cells.get(1).getText());
            lines.add(new CartLine(name, price));
        }
        return lines;
    }

    /**
     * One row in the cart table.
     *
     * @param name  item name
     * @param price item price in rupees
     */
    private record CartLine(String name, int price) {
    }
}
