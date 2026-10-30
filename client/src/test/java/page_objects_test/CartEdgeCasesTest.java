package page_objects_test;

import org.testng.Assert;
import org.testng.annotations.Test;
import page_objects.CartPage;
import page_objects.MoisturizerPage;
import webmanager.WebManager;

/**
 * Edge cases around an empty cart and products that do not exist.
 */
public class CartEdgeCasesTest extends WebManager {

    /**
     * Opening the cart before adding anything shows no lines and a zero
     * total, rather than stale items or an error.
     */
    @Test
    public void emptyCartShowsNoLinesAndZeroTotal() {
        MoisturizerPage moisturizerPage = new MoisturizerPage(getDriver());

        CartPage cart = moisturizerPage.openCart();

        Assert.assertTrue(cart.isEmpty(), "cart should start empty");
        Assert.assertEquals(cart.lineCount(), 0, "cart should list no lines");
        Assert.assertEquals(cart.total(), 0, "empty cart should total zero");
    }

    /**
     * Asking the catalogue for an ingredient it does not stock fails loudly
     * instead of silently picking an unrelated product.
     */
    @Test
    public void unknownIngredientIsRejected() {
        MoisturizerPage moisturizerPage = new MoisturizerPage(getDriver());

        Assert.assertThrows(IllegalStateException.class,
                () -> moisturizerPage.cheapestMatching("Unobtainium"));
    }
}
