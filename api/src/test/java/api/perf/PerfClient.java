package api.perf;

import api.config.ApiConfig;
import api.model.Product;

import static io.restassured.RestAssured.given;

/**
 * Bare-bones calls for the load runs: status code only, no body
 * materialisation beyond what the server sends, so the measurement is
 * the endpoint's cost rather than the client's parsing cost. The
 * contract suite's {@code ShopClient} remains the readable client;
 * this one is the stopwatch's.
 */
public class PerfClient {

    public int home() {
        return status(given().baseUri(ApiConfig.BASE_URL).when().get(ApiConfig.HOME).statusCode());
    }

    public int moisturizers() {
        return status(given().baseUri(ApiConfig.BASE_URL).when().get(ApiConfig.MOISTURIZERS).statusCode());
    }

    public int sunscreens() {
        return status(given().baseUri(ApiConfig.BASE_URL).when().get(ApiConfig.SUNSCREENS).statusCode());
    }

    public int submitCart(Product item) {
        return status(given().baseUri(ApiConfig.BASE_URL)
                .contentType("application/x-www-form-urlencoded")
                .formParam(item.name(), item.price())
                .when().post(ApiConfig.CART).statusCode());
    }

    public int confirmation() {
        return status(given().baseUri(ApiConfig.BASE_URL).when().post(ApiConfig.CONFIRMATION).statusCode());
    }

    private int status(int status) {
        return status;
    }
}
