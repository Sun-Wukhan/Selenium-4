package examples;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import page_objects.CatalogProduct;
import page_objects.MoisturizerPage;
import screenrecording.MyScreenRecorder;
import webmanager.WebManager;

import java.lang.reflect.Method;

/**
 * Example: record a video of a test run with {@link MyScreenRecorder}.
 *
 * <p>The recorder wraps the test method: recording starts before the test and
 * stops after it, named after the method. The {@code .avi} files under
 * {@code client/recordings/} in this repository's first commit were captured
 * exactly this way. Recordings land in {@code ./recordings/} relative to the
 * working directory.
 *
 * <p>The test is {@code enabled = false} on purpose: Monte's screen recorder
 * captures the real display through AWT, so it needs a headed session on a
 * desktop. It cannot record a headless browser and will fail in CI — enable
 * it for local, headed debugging runs only ({@code make test}), where a video
 * of a failure is worth more than a screenshot.
 *
 * <p>Ordering note: TestNG runs the superclass {@code @BeforeMethod}
 * ({@link WebManager#setUp}) first, so the browser is open before recording
 * starts; the subclass {@code @AfterMethod} runs before
 * {@link WebManager#afterEachTestMethod}, so recording stops just before the
 * browser quits.
 */
public class ScreenRecordingExample extends WebManager {

    /**
     * Starts recording, named after the test method about to run.
     *
     * @param method the test method about to run
     * @throws Exception when the recorder cannot start
     */
    @BeforeMethod(alwaysRun = true)
    public void startRecording(Method method) throws Exception {
        MyScreenRecorder.startRecording(method.getName());
    }

    /**
     * Stops recording and writes the {@code .avi} file.
     *
     * @throws Exception when the recorder cannot stop
     */
    @AfterMethod(alwaysRun = true)
    public void stopRecording() throws Exception {
        MyScreenRecorder.stopRecording();
    }

    /**
     * The cheapest-moisturizer purchase, recorded end to end.
     */
    @Test(enabled = false)
    public void recordedCheapestPurchase() {
        MoisturizerPage moisturizerPage = new MoisturizerPage(getDriver());
        CatalogProduct product = moisturizerPage.cheapest();
        moisturizerPage.add(product);
        moisturizerPage.openCart();
    }
}
