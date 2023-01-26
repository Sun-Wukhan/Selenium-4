package page_objects;

import application_page_actions.NavBot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

public class PurchasePage {

    WebDriver driver;

    @FindBy(xpath = "//*[contains(text(),'Best Seller')]")
    WebElement bestSeller;

    @FindBy(xpath = "//li[@class='a-carousel-card']")
    List<WebElement> listOfBestSellers;

    @FindBy(id = "submit.add-to-cart")
    WebElement addToCart;

    @FindBy(id = "nav-cart-count")
    WebElement cart;

    @FindBy(xpath = "//p[@class='a-spacing-mini']")
    WebElement price;

    @FindBy(id = "sc-subtotal-amount-activecart")
    WebElement subTotalPrice;

    @FindBy(id = "sc-subtotal-label-activecart")
    WebElement quantityPrice;
    @FindBy(xpath = "//span[@class='a-dropdown-container']")
    WebElement quantityMatch;

    @FindBy(id = "quantity_3")
    WebElement quantityNumber;

    @FindBy(xpath = "/html/body/div[1]/div[2]/div[2]/div/div/div/div[2]/div/div[1]/div/div[1]/div/div/div/div/div[2]/div/div[2]/div/ol/li[1]/div[2]/div/div[2]/div/div/a/div/span/span")
    WebElement initPrice;
    public PurchasePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    public void getItemsTest() {
       NavBot.click(bestSeller, "Best Seller category ");
    }

    public void addItemToCart(){
        NavBot.click(listOfBestSellers.get(0), "Element");
        NavBot.click(addToCart, "Add to cart");
    }

    public void validateCartQuantity(){
       String number = NavBot.getText(cart, "Quantity in Cart");
       System.out.println(number);
    }
    public void goToCart(){
        NavBot.click(cart, "Cart");
    }

    public String validateInitialPrice(){
        String s = NavBot.getText(price, "Price");
        System.out.println(s);
        return s;

    }

    public String validateFinalPrice(){
        String s = NavBot.getText(subTotalPrice, "Subtotal Price");
        System.out.println(s);
        return s;
    }

    public String changeQuantity() throws InterruptedException {
        NavBot.click(quantityMatch, "Quantity");
        NavBot.click(quantityNumber, "number 3");
        Thread.sleep(4000);
        String text = NavBot.getText(quantityPrice, "Quantity");
        System.out.println(text);
        return text;

    }
}
