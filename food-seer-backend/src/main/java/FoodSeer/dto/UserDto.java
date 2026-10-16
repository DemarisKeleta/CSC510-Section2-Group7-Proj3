package FoodSeer.dto;

// dailyCalorieGoal added as the record's 7th component, alongside the existing fields
public record UserDto(Long id, String username, String email, String role, String costPreference, String dietaryRestrictions, Integer dailyCalorieGoal) {
    public static UserDto fromEntity(final FoodSeer.entity.User u) {
        if (u == null) return null;
        // pass the entity's calorie goal through as the new 7th constructor argument
        return new UserDto(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getCostPreference(), u.getDietaryRestrictions(), u.getDailyCalorieGoal());
    }
}
