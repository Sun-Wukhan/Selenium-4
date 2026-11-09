package api.config;

/**
 * Where the shop under test lives, and the paths of its endpoints.
 *
 * <p>The API suite targets the self-hosted shop by default
 * ({@code http://127.0.0.1:6464}, the port the Flask app serves on), so
 * it never depends on the public demo site's uptime or latency. Override
 * with {@code -Dshop.baseUrl} to aim it at any other deployment,
 * including the live demo.
 */
public final class ApiConfig {

    public static final String BASE_URL = System.getProperty("shop.baseUrl", "http://127.0.0.1:6464");

    public static final String HOME = "/";
    public static final String MOISTURIZERS = "/moisturizer";
    public static final String SUNSCREENS = "/sunscreen";
    public static final String CART = "/cart";
    public static final String CONFIRMATION = "/confirmation";
    public static final String ABOUT = "/about";

    private ApiConfig() {
    }
}
