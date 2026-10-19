package FoodSeer.dto;

/**
 * Body of a customer reaction report.
 *
 * @param orderId
 *            the customer's own order that the reaction followed
 * @param allergen
 *            the allergen the customer reacted to
 */
public record CustomerReactionRequestDto(Long orderId, String allergen) {
}
