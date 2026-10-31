package page_objects_test;

import excelreader.MyDataReader;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import page_objects.CartPage;
import page_objects.CatalogProduct;
import page_objects.HomePage;
import page_objects.MoisturizerPage;
import page_objects.SunscreenPage;
import webmanager.WebManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * The purchase checks driven from an Excel workbook instead of hardcoded
 * ingredients: the suite version of {@code examples.ExcelDrivenPurchaseExample}.
 *
 * <p>Data lives in {@code src/test/resources/data/ingredients.xlsx}. Sheet
 * {@code Ingredients} feeds the moisturizer test, sheet {@code Sunscreens}
 * the sunscreen test; each row is an ingredient pair and TestNG runs the
 * test once per row. Add a row to the sheet and the suite covers it — no
 * code change. A sheet committed at that path is used exactly as found;
 * when none exists, the providers seed the sample workbook below first
 * (the same out-of-the-box convention as the example class).
 *
 * <p>Page objects are local variables, never fields: TestNG runs this
 * class's methods — including every data-provider row — in parallel on one
 * shared instance.
 */
public class DataDrivenPurchaseTest extends WebManager {

    private static final Path WORKBOOK = Path.of(
            System.getProperty("user.dir"), "src", "test", "resources", "data", "ingredients.xlsx");
    private static final String MOISTURIZER_SHEET = "Ingredients";
    private static final String SUNSCREEN_SHEET = "Sunscreens";

    /**
     * Supplies the moisturizer ingredient pairs from the workbook.
     *
     * @return one {@code {ingredientA, ingredientB}} pair per sheet row
     * @throws Exception when the workbook cannot be seeded or read
     */
    @DataProvider(name = "moisturizerPairs")
    public Object[][] moisturizerPairs() throws Exception {
        return sheetRows(MOISTURIZER_SHEET);
    }

    /**
     * Supplies the sunscreen ingredient pairs from the workbook.
     *
     * @return one {@code {ingredientA, ingredientB}} pair per sheet row
     * @throws Exception when the workbook cannot be seeded or read
     */
    @DataProvider(name = "sunscreenPairs")
    public Object[][] sunscreenPairs() throws Exception {
        return sheetRows(SUNSCREEN_SHEET);
    }

    /**
     * Adds the cheapest moisturizer for each ingredient in the row and
     * checks both cart lines and the total.
     *
     * @param firstIngredient  first ingredient from the sheet row
     * @param secondIngredient second ingredient from the sheet row
     */
    @Test(dataProvider = "moisturizerPairs")
    public void moisturizerPairFromSheetMatchesCart(String firstIngredient, String secondIngredient) {
        MoisturizerPage moisturizerPage = new MoisturizerPage(getDriver());
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
     * Adds the cheapest sunscreen for each ingredient in the row and checks
     * both cart lines and the total.
     *
     * @param firstIngredient  first ingredient from the sheet row
     * @param secondIngredient second ingredient from the sheet row
     */
    @Test(dataProvider = "sunscreenPairs")
    public void sunscreenPairFromSheetMatchesCart(String firstIngredient, String secondIngredient) {
        SunscreenPage sunscreenPage = HomePage.open(getDriver()).buySunscreens();
        CatalogProduct first = sunscreenPage.cheapestMatching(firstIngredient);
        CatalogProduct second = sunscreenPage.cheapestMatching(secondIngredient, Set.of(first.name()));
        sunscreenPage.add(first);
        sunscreenPage.add(second);

        CartPage cart = sunscreenPage.openCart();

        Assert.assertTrue(cart.contains(first.name()), "cart should list " + first.name());
        Assert.assertTrue(cart.contains(second.name()), "cart should list " + second.name());
        Assert.assertEquals(cart.total(), first.price() + second.price(),
                "cart total should be the sum of both prices");
    }

    private static Object[][] sheetRows(String sheet) throws Exception {
        seedWorkbookIfMissing();
        MyDataReader reader = new MyDataReader();
        reader.setExcelFile(WORKBOOK.toString());
        return reader.getExcelSheetData(sheet);
    }

    /**
     * Writes the sample workbook (a header plus two pairs per sheet) when
     * the test-resources file does not exist yet. Synchronized because the
     * data providers can be invoked in parallel: one thread writes while
     * the others wait, then read the finished file.
     *
     * @throws IOException when the workbook cannot be written
     */
    private static synchronized void seedWorkbookIfMissing() throws IOException {
        if (Files.exists(WORKBOOK)) {
            return;
        }
        Files.createDirectories(WORKBOOK.getParent());
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream out = Files.newOutputStream(WORKBOOK)) {
            writeSheet(workbook, MOISTURIZER_SHEET, new String[][]{
                    {"Aloe", "Almond"},
                    {"Almond", "Aloe"},
            });
            writeSheet(workbook, SUNSCREEN_SHEET, new String[][]{
                    {"SPF-50", "SPF-30"},
                    {"SPF-30", "SPF-50"},
            });
            workbook.write(out);
        }
    }

    private static void writeSheet(XSSFWorkbook workbook, String name, String[][] pairs) {
        Sheet sheet = workbook.createSheet(name);
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("IngredientA");
        header.createCell(1).setCellValue("IngredientB");
        for (int i = 0; i < pairs.length; i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(pairs[i][0]);
            row.createCell(1).setCellValue(pairs[i][1]);
        }
    }
}
