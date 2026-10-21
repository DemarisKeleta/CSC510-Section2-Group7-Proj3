package FoodSeer.dto;

/** Request body for logging (or updating) a meal's calories for today. */
// request shape for POST /api/users/me/mealLog; calories is Integer (not int) so a missing value can be detected as null
public record LogMealRequestDto(String mealType, Integer calories) {
}
