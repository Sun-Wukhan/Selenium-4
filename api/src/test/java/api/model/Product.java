package api.model;

/**
 * One product as the shop presents it: a display name and a price in
 * rupees. The same shape the UI suite's catalogue cards carry, so API
 * and UI assertions speak about products identically.
 *
 * @param name  display name, exactly as rendered
 * @param price price in rupees
 */
public record Product(String name, int price) {
}
