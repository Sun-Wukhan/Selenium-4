package examples;

import excelreader.MyDataReader;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import page_objects.CartPage;
import page_objects.CatalogProduct;
import page_objects.MoisturizerPage;
import webmanager.WebManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Example: drive the purchase test from an Excel sheet via {@link MyDataReader}.
 *
 * <p>{@code PurchaseTest} hardcodes its ingredients ("Aloe", "Almond"). This
 * example shows the data-driven version of the same test: each sheet row names
 * two ingredients, and TestNG runs the test once per row. Add a row to the
 * sheet and the test runs again with that data — no code change.
 *
 * <p>The workbook lives at {@code src/test/resources/data/ingredients.xlsx}
 * (sheet {@code Ingredients}, first row is a header, which
 * {@code MyDataReader.getExcelSheetData} skips). In a real project that file
 * is committed with the tests. So this example can run out of the box, the
 * data provider first seeds a small sample workbook when none exists yet;
 * replace the seeded file with your own sheet at the same path.
 *
 * <p>To run it, add this class to {@code src/test/java/runner/buy.xml}:
 * <pre>{@code
 * <class name="examples.ExcelDrivenPurchaseExample"/>
 * }</pre>
 */
public class ExcelDrivenPurchaseExample extends WebManager {

    private static final Path WORKBOOK = Path.of(
            System.getProperty("user.dir"), "src", "test", "resources", "data", "ingredients.xlsx");
    private static final String SHEET = "Ingredients";

    private MoisturizerPage moisturizerPage;

    /**
     * Opens the page object against the browser started by {@link WebManager}.
     */
    @BeforeMethod
    public void openCatalog() {
        moisturizerPage = new MoisturizerPage(getDriver());
    }

    /**
     * Supplies one {@code {ingredientA, ingredientB}} pair per sheet row.
     *
     * @return the sheet rows, header excluded
     * @throws Exception when the workbook cannot be read
     */
    @DataProvider(name = "ingredientPairs")
    public Object[][] ingredientPairs() throws Exception {
        seedWorkbookIfMissing();
        MyDataReader reader = new MyDataReader();
        reader.setExcelFile(WORKBOOK.toString());
        return reader.getExcelSheetData(SHEET);
    }

    /**
     * Adds the cheapest product for each ingredient and checks the cart total.
     *
     * @param firstIngredient  first ingredient from the sheet row
     * @param secondIngredient second ingredient from the sheet row
     */
    @Test(dataProvider = "ingredientPairs")
    public void cheapestPairFromSheetMatchesCartTotal(String firstIngredient, String secondIngredient) {
        CatalogProduct first = moisturizerPage.cheapestMatching(firstIngredient);
        CatalogProduct second = moisturizerPage.cheapestMatching(secondIngredient, Set.of(first.name()));
        moisturizerPage.add(first);
        moisturizerPage.add(second);

        CartPage cart = moisturizerPage.openCart();

        Assert.assertTrue(cart.contains(first.name()), "cart should list " + first.name());
        Assert.assertTrue(cart.contains(second.name()), "cart should list " + second.name());
        Assert.assertEquals(cart.total(), first.price() + second.price(),
                "cart total should be the sum of both prices");
    }

    /**
     * Writes the sample workbook (header plus two ingredient pairs) when the
     * test-resources file does not exist yet.
     *
     * @throws IOException when the workbook cannot be written
     */
    private static void seedWorkbookIfMissing() throws IOException {
        if (Files.exists(WORKBOOK)) {
            return;
        }
        Files.createDirectories(WORKBOOK.getParent());
        String[][] rows = {
                {"IngredientA", "IngredientB"},
                {"Aloe", "Almond"},
                {"Almond", "Aloe"},
        };
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream out = Files.newOutputStream(WORKBOOK)) {
            Sheet sheet = workbook.createSheet(SHEET);
            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i);
                for (int j = 0; j < rows[i].length; j++) {
                    row.createCell(j).setCellValue(rows[i][j]);
                }
            }
            workbook.write(out);
        }
    }
}
