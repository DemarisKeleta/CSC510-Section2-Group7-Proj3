package FoodSeer.dto;

// List type for the per-meal breakdown field
import java.util.List;

/**
 * A customer's computed calorie budget for today.
 *
 * caloriesBurnedToday / fitnessDataTracked: "not tracked" (fitnessDataTracked
 * false, caloriesBurnedToday null) is distinct from "tracked at zero" - no
 * generated fitness file was found or it was malformed.
 *
 * dailyCalorieGoal / goalSet: a customer who never set a goal has
 * goalSet=false, dailyCalorieGoal null - we do not invent a default goal.
 *
 * caloriesConsumedToday: total of (a) calorie values across every food in
 * every order the customer placed today, plus (b) every manually logged
 * meal in loggedMealsToday. Foods with no recorded calorie value do not
 * contribute (they are not assumed to be zero), so this number can
 * under-count if some ordered items lack calorie data.
 *
 * loggedMealsToday: the customer's self-reported breakfast/lunch/dinner
 * entries for today, in the order logged. The first meal logged on a given
 * day naturally gets the full budget (nothing has been subtracted yet);
 * each meal logged after that is reduced by everything logged before it,
 * since all of loggedMealsToday is summed into caloriesConsumedToday
 * regardless of meal type.
 *
 * remainingCalories is only ever non-null when both fitnessDataTracked and
 * goalSet are true; it is goal + calories burned - calories consumed today
 * (activity increases the budget, matching how fitness-tracking apps
 * compute it; orders and logged meals placed today reduce it).
 */
// response shape for GET /api/users/me/calorieBudget and the budget embedded in a food recommendation
public record CalorieBudgetDto(
        // calories burned today per the fitness file, or null if not tracked
        Integer caloriesBurnedToday,
        // true only when a fitness file was found and parsed successfully
        boolean fitnessDataTracked,
        // the customer's saved daily calorie goal, or null if never set
        Integer dailyCalorieGoal,
        // true only when dailyCalorieGoal is non-null
        boolean goalSet,
        // orders' calories + logged meals' calories, summed together
        Integer caloriesConsumedToday,
        // per-meal breakdown (breakfast/lunch/dinner) for today
        List<MealLogEntryDto> loggedMealsToday,
        // goal + burned - consumed, or null if it can't be computed yet
        Integer remainingCalories
) {
}
