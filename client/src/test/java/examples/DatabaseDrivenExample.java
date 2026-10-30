package examples;

import database_utilities.DBDataProvider;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import webmanager.WebManager;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Example: feed a TestNG data provider from PostgreSQL via {@link DBDataProvider}.
 *
 * <p>{@code DBDataProvider.dataSupplier} runs two queries — one for the rows,
 * one for the row count — and returns the rows as the {@code String[][]} a
 * TestNG {@code @DataProvider} needs. Whatever a test would otherwise hardcode
 * or keep in a sheet can live in a table instead, so QA can change the data
 * without a code change. The provider's dataset is the framework's
 * {@code public.testdata} table: {@code email} and {@code message} columns,
 * the shape a contact-form test consumes.
 *
 * <p>The test below is {@code enabled = false} because it needs a live
 * database. Before enabling it:
 * <ul>
 *   <li>Create the table, e.g.
 *       {@code CREATE TABLE public.testdata (email text, message text);}</li>
 *   <li>Point {@code DBUtilities.loadProperties()} at your own
 *       {@code app.properties}. As committed, it reads a hardcoded absolute
 *       path from the author's machine; the file must define
 *       {@code url.test} (a {@code jdbc:postgresql://host:port/database} URL),
 *       {@code userName.test} and {@code password.test}.</li>
 * </ul>
 *
 * <p>As written, the test validates each row pulled from the database before
 * any form test spends a browser session on it: a well-formed email and a
 * non-blank message. In production use the same provider feeds the form test
 * directly — type {@code email} into the email field, {@code message} into
 * the message field, submit, assert.
 */
public class DatabaseDrivenExample extends WebManager {

    /**
     * Reads every row of {@code public.testdata} through {@link DBDataProvider}.
     *
     * @return one {@code {email, message}} pair per table row
     * @throws IOException  when the properties file cannot be read
     * @throws SQLException when either query fails
     */
    @DataProvider(name = "testDataFromDb")
    public Object[][] testDataFromDb() throws IOException, SQLException {
        return DBDataProvider.dataSupplier(
                "SELECT email, message FROM public.testdata",
                "SELECT count(*) FROM public.testdata");
    }

    /**
     * Checks one database row is usable contact-form data.
     *
     * @param email   value of the row's {@code email} column
     * @param message value of the row's {@code message} column
     */
    @Test(dataProvider = "testDataFromDb", enabled = false)
    public void databaseRowIsUsableFormData(String email, String message) {
        Assert.assertNotNull(email, "email column must not be null");
        Assert.assertTrue(email.contains("@"), "email should be an address, was: " + email);
        Assert.assertNotNull(message, "message column must not be null");
        Assert.assertFalse(message.isBlank(), "message should not be blank for " + email);
    }
}
