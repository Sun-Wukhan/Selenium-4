package page_objects_test;

import org.openqa.selenium.support.PageFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import page_objects.PurchasePage;
import screenrecording.MyScreenRecorder;
import webmanager.WebManager;

public class PurchaseTest extends WebManager {

    PurchasePage purchasePage;

    @BeforeMethod
    public void initialization (){

        purchasePage = PageFactory.initElements(driver, PurchasePage.class);
    }

    @Test
    public void validatePriceMatches() throws Exception {
        MyScreenRecorder.startRecording("Recording");
        purchasePage.getItemsTest();
        purchasePage.addItemToCart();
        Thread.sleep(3000);
        purchasePage.goToCart();

        String initialPrice = purchasePage.validateInitialPrice();
        String finalPrice = purchasePage.validateFinalPrice();

        String cleanInitial = initialPrice.replaceAll("^\\s+", "");
        String cleanFinal = finalPrice.replaceAll("^\\s+", "");

        WebManager.captureScreenshot(driver, "Demo");
        Assert.assertEquals(cleanInitial, cleanFinal);
        MyScreenRecorder.stopRecording();
    }

    @Test
    public void validateQuantityMatches() throws Exception {
        MyScreenRecorder.startRecording("Recording");
        purchasePage.getItemsTest();
        purchasePage.addItemToCart();
        Thread.sleep(3000);
        purchasePage.goToCart();

        String actual = purchasePage.changeQuantity();
        Assert.assertEquals(actual, "Subtotal (3 items):");
        WebManager.captureScreenshot(driver, "Quantity");
        MyScreenRecorder.stopRecording();
    }
}