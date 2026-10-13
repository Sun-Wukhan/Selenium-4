package page_objects;

import org.openqa.selenium.WebElement;

/**
 * One moisturizer card from the catalog: name, price in rupees, and its Add button.
 *
 * @param name      product name as shown on the card
 * @param price     price in rupees
 * @param addButton button that adds this product to the cart
 */
public record CatalogProduct(String name, int price, WebElement addButton) {
}
