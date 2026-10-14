package FoodSeer.dto;

/** One logged meal's calorie total for today. */
// small record used inside CalorieBudgetDto.loggedMealsToday to report one meal's type and calories
public record MealLogEntryDto(String mealType, int calories) {
}
