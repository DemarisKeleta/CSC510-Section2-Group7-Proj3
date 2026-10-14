package FoodSeer.service;

import FoodSeer.dto.CalorieBudgetDto;
import FoodSeer.dto.FoodRecommendationDto;
import FoodSeer.entity.User;

/**
 * Computes a customer's remaining calorie budget for today and uses it
 * to recommend menu items, per the Fitness-Based Calorie Tracking use case.
 */
// contract for budget computation, food recommendation, and meal logging; implemented by CalorieBudgetServiceImpl
public interface CalorieBudgetService {

    /**
     * Computes the calorie budget for the given user.
     */
    // returns burned/goal/consumed/remaining for the given user, for today
    CalorieBudgetDto getBudgetFor(User user);

    /**
     * Returns menu items that fit within the given user's remaining
     * calorie budget. Falls back to the full menu, with budgetApplied=false,
     * if the budget cannot be computed (no fitness data, or no goal set).
     */
    // returns the budget-filtered (or, if not computable, full) menu for the given user
    FoodRecommendationDto getRecommendedFoodsFor(User user);

    /**
     * Logs (or updates) the calories consumed for one meal slot today.
     *
     * @param user the user logging the meal
     * @param mealType one of FoodSeer.constant.MealType's values
     * @param calories calories consumed for this meal
     * @return the updated budget, reflecting the new total
     * @throws IllegalArgumentException if mealType is not a recognized value or calories is negative
     */
    // upserts today's entry for one meal slot and returns the recomputed budget
    CalorieBudgetDto logMeal(User user, String mealType, int calories);
}
