package api.parsing;

import api.model.Product;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts products from a catalogue page's HTML using the same
 * selectors the UI suite's page objects use, so an API failure and a UI
 * failure about the same page are directly comparable.
 */
public final class CatalogPageParser {

    private static final Pattern PRICE_DIGITS = Pattern.compile("(\\d+)");

    private CatalogPageParser() {
    }

    /**
     * Parses every product card on a catalogue page.
     *
     * @param html the page body
     * @return one product per card, in page order
     */
    public static List<Product> parse(String html) {
        Document doc = Jsoup.parse(html);
        List<Product> products = new ArrayList<>();
        for (Element card : doc.select("div.text-center.col-4")) {
            String name = card.select("p.font-weight-bold").text().trim();
            String priceText = card.select("p:not(.font-weight-bold)").text();
            products.add(new Product(name, parsePrice(priceText)));
        }
        return products;
    }

    /**
     * Reads the cart total from a cart page ("Total: Rupees 327").
     *
     * @param html the cart page body
     * @return the total in rupees
     */
    public static int parseCartTotal(String html) {
        Element total = Jsoup.parse(html).getElementById("total");
        if (total == null) {
            throw new IllegalStateException("Cart page has no #total element");
        }
        return parsePrice(total.text());
    }

    /**
     * Reads the item names listed on a cart page.
     *
     * @param html the cart page body
     * @return one name per cart row
     */
    public static List<String> parseCartItems(String html) {
        List<String> names = new ArrayList<>();
        for (Element row : Jsoup.parse(html).select("table tbody tr")) {
            names.add(row.select("td").first().text().trim());
        }
        return names;
    }

    private static int parsePrice(String raw) {
        Matcher matcher = PRICE_DIGITS.matcher(raw);
        if (!matcher.find()) {
            throw new IllegalStateException("No price in: " + raw);
        }
        return Integer.parseInt(matcher.group(1));
    }
}
