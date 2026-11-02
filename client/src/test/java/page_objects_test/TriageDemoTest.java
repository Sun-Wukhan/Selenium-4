package page_objects_test;

import org.testng.Assert;
import org.testng.annotations.Test;
import page_objects.HomePage;
import webmanager.WebManager;

/**
 * DEMO ONLY — a deliberately failing test that exists solely to exercise the
 * AI failure triage job in CI (see ci/ai-triage.py). It lives only on the
 * demo/ai-triage branch and must never be merged into master.
 *
 * <p>The home page only ever serves a temperature in one of two bands
 * (1–18 or 35–49), so asserting exactly 21 degrees fails deterministically
 * with the real served temperature in the assertion message.
 */
public class TriageDemoTest extends WebManager {

    @Test
    public void demoFailureForAiTriage() {
        int temperature = HomePage.open(getDriver()).temperature();

        Assert.assertEquals(temperature, 21,
                "DEMO FAILURE (deliberate): expected the home page to serve exactly 21 degrees");
    }
}
