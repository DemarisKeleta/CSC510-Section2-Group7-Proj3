package FoodSeer.dto;

// dailyCalorieGoal added as a 3rd, optional field; Integer so an omitted value deserializes as null
public record UserPreferencesDto ( String costPreference, String dietaryRestrictions, Integer dailyCalorieGoal ) {
}
