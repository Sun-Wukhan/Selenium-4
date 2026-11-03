package page_objects_test;

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;
import page_objects.HomePage;
import page_objects.MoisturizerPage;
import reporting.ExtentTestManager;
import webmanager.WebManager;

import java.util.List;

/**
 * Accessibility checks with axe-core (WCAG 2 A and AA rule tags) on the
 * shop's main pages.
 *
 * <p>The bar is zero <em>critical</em> violations — a critical result
 * means a user of assistive technology is blocked outright, which is
 * never acceptable. Serious and lower-impact violations are written to
 * the Extent report as warnings rather than failing the build: this is
 * a third-party demo shop the suite does not control, so the suite
 * reports its shortcomings without going red over them.
 *
 * <p>Page objects are local variables, never fields: TestNG runs this
 * class's methods in parallel on one shared instance.
 */
public class AccessibilityTest extends WebManager {

    private static final List<String> WCAG_TAGS = List.of("wcag2a", "wcag2aa");

    /**
     * The landing page (temperature and the two buy buttons).
     */
    @Test
    public void homePageHasNoCriticalViolations() {
        HomePage.open(getDriver());
        assertNoCriticalViolations("home page");
    }

    /**
     * The moisturiser catalogue.
     */
    @Test
    public void moisturizerPageHasNoCriticalViolations() {
        new MoisturizerPage(getDriver()).productCount();
        assertNoCriticalViolations("moisturiser catalogue");
    }

    /**
     * The sunscreen catalogue.
     */
    @Test
    public void sunscreenPageHasNoCriticalViolations() {
        HomePage.open(getDriver()).buySunscreens().productCount();
        assertNoCriticalViolations("sunscreen catalogue");
    }

    /**
     * The cart, with one product in it so the checkout table renders.
     */
    @Test
    public void cartPageHasNoCriticalViolations() {
        MoisturizerPage page = new MoisturizerPage(getDriver());
        page.add(page.cheapest());
        page.openCart();
        assertNoCriticalViolations("cart page");
    }

    private void assertNoCriticalViolations(String pageName) {
        Results results = new AxeBuilder()
                .withTags(WCAG_TAGS)
                .analyze(getDriver());
        List<Rule> violations = results.getViolations();
        for (Rule violation : violations) {
            if (!"critical".equals(violation.getImpact()) && ExtentTestManager.getTest() != null) {
                ExtentTestManager.getTest().warning("axe " + violation.getId()
                        + " (" + violation.getImpact() + ") on " + pageName + ": "
                        + violation.getHelp());
            }
        }
        List<Rule> critical = violations.stream()
                .filter(violation -> "critical".equals(violation.getImpact()))
                .toList();
        Assert.assertTrue(critical.isEmpty(),
                pageName + " has critical accessibility violations: " + describe(critical));
    }

    private String describe(List<Rule> violations) {
        StringBuilder text = new StringBuilder();
        for (Rule violation : violations) {
            text.append(violation.getId())
                    .append(" (").append(violation.getImpact()).append("): ")
                    .append(violation.getHelp())
                    .append(" — ").append(violation.getNodes().size()).append(" node(s); ");
        }
        return text.toString();
    }
}
