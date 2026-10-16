package FoodSeer.dto;

// List type for the returned foods
import java.util.List;

/**
 * Result of a calorie-budget-aware food recommendation.
 *
 * budgetApplied is false whenever the budget could not be computed
 * (fitness data not tracked, or no goal set) - in that case foods is the
 * full, unfiltered menu rather than an empty or guessed-at list.
 *
 * excludedForMissingCalories counts foods omitted from a budget-applied
 * recommendation because they have no recorded calorie value - they are
 * not silently treated as zero calories.
 */
// response shape for GET /api/foods/withinCalorieBudget
public record FoodRecommendationDto(
        // the recommended (or, if budgetApplied is false, full) menu
        List<FoodDto> foods,
        // the budget this recommendation was computed against
        CalorieBudgetDto budget,
        // whether calorie filtering actually happened
        boolean budgetApplied,
        // count of foods skipped for having no recorded calorie value
        int excludedForMissingCalories
) {
}
