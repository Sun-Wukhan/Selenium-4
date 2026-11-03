package page_objects_test;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.testng.Assert;
import org.testng.annotations.Test;
import page_objects.HomePage;
import page_objects.MoisturizerPage;
import page_objects.SunscreenPage;
import webmanager.WebManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Visual checks on the catalogue layout.
 *
 * <p>Classic pixel-diff regression is the wrong tool for this shop: the
 * catalogue is re-randomised on every load, so no two screenshots ever
 * match and a stored baseline would flake constantly. The deterministic
 * visual surface is geometric instead — the shop serves exactly six
 * product cards per load, cards tile a grid without overlapping, and
 * each Add button sits inside its own card — plus a sanity check that
 * the rendered page is a real, non-blank image at the expected size.
 *
 * <p>Page objects are local variables, never fields: TestNG runs this
 * class's methods in parallel on one shared instance.
 */
public class VisualLayoutTest extends WebManager {

    private static final int PRODUCTS_PER_LOAD = 6;

    /**
     * The moisturiser cards form a clean grid and the cheapest product's
     * Add button sits inside its card.
     */
    @Test
    public void moisturizerCardsTileWithoutOverlap() {
        MoisturizerPage page = new MoisturizerPage(getDriver());
        assertGridLayout(page, "moisturiser catalogue");
    }

    /**
     * The sunscreen cards form the same clean grid.
     */
    @Test
    public void sunscreenCardsTileWithoutOverlap() {
        SunscreenPage page = HomePage.open(getDriver()).buySunscreens();
        assertGridLayout(page, "sunscreen catalogue");
    }

    /**
     * A screenshot of the catalogue is a full-size, non-blank render.
     */
    @Test
    public void catalogueScreenshotIsARealRender() throws IOException {
        new MoisturizerPage(getDriver()).productCount();
        byte[] png = ((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.BYTES);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));

        Assert.assertNotNull(image, "screenshot should decode as an image");
        Assert.assertTrue(image.getWidth() >= 1200,
                "screenshot should be desktop-width, was " + image.getWidth());
        Assert.assertTrue(image.getHeight() >= 600,
                "screenshot should be desktop-height, was " + image.getHeight());

        Set<Integer> colours = new HashSet<>();
        for (int x = 0; x < image.getWidth(); x += 31) {
            for (int y = 0; y < image.getHeight(); y += 31) {
                colours.add(image.getRGB(x, y));
            }
        }
        // The catalogue page is mostly white space, so a healthy render
        // samples only a few dozen distinct colours at this stride (62
        // observed); a blank or flat render samples a handful at most.
        Assert.assertTrue(colours.size() > 25,
                "screenshot looks blank or near-flat: only " + colours.size()
                        + " distinct sampled colours");
    }

    private void assertGridLayout(MoisturizerPage page, String pageName) {
        List<Rectangle> cards = page.cardRectangles();
        Assert.assertEquals(cards.size(), PRODUCTS_PER_LOAD,
                pageName + " should serve " + PRODUCTS_PER_LOAD + " product cards per load");
        for (int i = 0; i < cards.size(); i++) {
            Assert.assertTrue(cards.get(i).getWidth() > 0 && cards.get(i).getHeight() > 0,
                    pageName + ": card " + i + " should have a real size");
            for (int j = i + 1; j < cards.size(); j++) {
                Assert.assertFalse(overlaps(cards.get(i), cards.get(j)),
                        pageName + ": cards " + i + " and " + j + " overlap");
            }
        }
        Rectangle button = page.cheapest().addButton().getRect();
        boolean insideACard = cards.stream().anyMatch(card -> contains(card, button));
        Assert.assertTrue(insideACard,
                pageName + ": the cheapest product's Add button should sit inside its card");
    }

    private boolean overlaps(Rectangle a, Rectangle b) {
        return a.getX() < b.getX() + b.getWidth()
                && b.getX() < a.getX() + a.getWidth()
                && a.getY() < b.getY() + b.getHeight()
                && b.getY() < a.getY() + a.getHeight();
    }

    private boolean contains(Rectangle outer, Rectangle inner) {
        return inner.getX() >= outer.getX()
                && inner.getY() >= outer.getY()
                && inner.getX() + inner.getWidth() <= outer.getX() + outer.getWidth()
                && inner.getY() + inner.getHeight() <= outer.getY() + outer.getHeight();
    }
}
