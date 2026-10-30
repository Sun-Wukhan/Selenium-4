package examples;

import email.SendingEmail;
import org.apache.commons.mail.EmailException;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.Reporter;

/**
 * Example: email the team when a test fails, via {@link SendingEmail}.
 *
 * <p>Registered as a TestNG listener, this sends one email per failed test
 * naming the method — {@code SendingEmail.sendSimpleEmail} composes it as
 * "The following test failed: &lt;method&gt;". The other two helpers on
 * {@code SendingEmail} cover the richer cases: {@code sendEmailWithAttachment}
 * for attaching the report or a screenshot, and {@code formattedHTMLEmail}
 * for an HTML message with an inline image.
 *
 * <p>Register it on a test class:
 * <pre>{@code
 * @Listeners(examples.FailureEmailListenerExample.class)
 * public class PurchaseTest extends WebManager { ... }
 * }</pre>
 * or for a whole suite, in {@code src/test/java/runner/buy.xml}:
 * <pre>{@code
 * <listeners>
 *     <listener class-name="examples.FailureEmailListenerExample"/>
 * </listeners>
 * }</pre>
 *
 * <p>Before relying on it, replace the placeholder values inside
 * {@code SendingEmail} — SMTP host, authenticator username/password, from and
 * to addresses are sample values as committed. A failed send is caught and
 * logged here: a broken mail server must never turn a passing test into a
 * failing one, or hide the real failure behind a mail error.
 */
public class FailureEmailListenerExample implements ITestListener {

    /**
     * Sends the failure email for the test that just failed.
     *
     * @param result the failed test's result
     */
    @Override
    public void onTestFailure(ITestResult result) {
        try {
            SendingEmail.sendSimpleEmail(result.getMethod().getMethodName());
        } catch (EmailException exception) {
            Reporter.log("Failure email could not be sent: " + exception.getMessage(), true);
        }
    }
}
