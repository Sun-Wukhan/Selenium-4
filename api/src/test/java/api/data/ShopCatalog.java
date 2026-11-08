package api.data;

import java.util.Map;

/**
 * The shop's canonical product pools, transcribed from the shop's own
 * source (weather_shopper.py). Every catalogue response is a random
 * sample of one of these pools, so the API contract is: whatever a page
 * returns, each product must be a member of the pool at exactly its
 * canonical price.
 */
public final class ShopCatalog {

    public static final Map<String, Integer> MOISTURIZERS = Map.ofEntries(
            Map.entry("Wilhelm Aloe Hydration Lotion", 365),
            Map.entry("Emmanuel Aloe Vera Beauty Gel", 299),
            Map.entry("Jose Intensive Care Aloe Body Lotion", 216),
            Map.entry("Alexander Almond & Honey Moisturiser", 360),
            Map.entry("Max honey and almond moisturiser", 195),
            Map.entry("Mikhail Natural Almond Moisturizer", 220),
            Map.entry("Vassily Aloe Attack", 199),
            Map.entry("Mikhail Almond and Talc", 353),
            Map.entry("Tigran Aloe Isolani", 215),
            Map.entry("Boris Almond and Honey", 128));

    public static final Map<String, Integer> SUNSCREENS = Map.ofEntries(
            Map.entry("Robert Herbals Sunblock SPF-40", 350),
            Map.entry("Anatoly Ultra Sunblock SPF-50", 289),
            Map.entry("Gary Bio Sandalwood SPF-50", 250),
            Map.entry("Vladimir Sun Expert SPF-30", 160),
            Map.entry("Vishy La Shield Sunscreen spf-30", 195),
            Map.entry("Magnus Resistant Sunscreen SPF-30", 140),
            Map.entry("Paul Magnificient SPF-30", 121),
            Map.entry("Akiba Amazing SPF-50", 222),
            Map.entry("Vassily Brilliant SPF-30", 116));

    /** Products shown per catalogue page: the shop samples 6 per load. */
    public static final int PAGE_SIZE = 6;

    private ShopCatalog() {
    }
}
