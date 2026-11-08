package api.client;

import api.config.ApiConfig;
import api.model.Product;
import io.restassured.response.Response;

import java.util.List;

import static io.restassured.RestAssured.given;

/**
 * Thin REST Assured client over the shop's HTTP surface. Endpoints are
 * server-rendered pages and form posts (the shop predates JSON APIs);
 * the client returns raw responses and lets the tests assert on status,
 * headers and parsed bodies.
 */
public class ShopClient {

    public Response home() {
        return get(ApiConfig.HOME);
    }

    public Response moisturizers() {
        return get(ApiConfig.MOISTURIZERS);
    }

    public Response sunscreens() {
        return get(ApiConfig.SUNSCREENS);
    }

    public Response about() {
        return get(ApiConfig.ABOUT);
    }

    public Response get(String path) {
        return given().baseUri(ApiConfig.BASE_URL).when().get(path);
    }

    public Response post(String path) {
        return given().baseUri(ApiConfig.BASE_URL).when().post(path);
    }

    /**
     * Submits a cart exactly as the shop's own page does: one form field
     * per product, the field name being the product name and the value
     * its price.
     *
     * @param items products to put in the cart
     * @return the rendered cart page response
     */
    public Response submitCart(List<Product> items) {
        var request = given().baseUri(ApiConfig.BASE_URL)
                .contentType("application/x-www-form-urlencoded");
        for (Product item : items) {
            request.formParam(item.name(), item.price());
        }
        return request.when().post(ApiConfig.CART);
    }

    public Response confirmation() {
        return post(ApiConfig.CONFIRMATION);
    }
}
