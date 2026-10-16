package FoodSeer.service;

import FoodSeer.entity.User;

public interface UserService {

    /**
     * Returns the currently authenticated user.
     */
    public User getCurrentUser ();

    public User getByUsername ( String username );

    /**
     * Returns all users in the system.
     */
    public java.util.List<FoodSeer.entity.User> listUsers();

    /**
     * Find user by id.
     */
    public FoodSeer.entity.User findById(Long id);

    /**
     * Update a user's role and return updated user.
     */
    public FoodSeer.entity.User updateUserRole(Long id, String role);

    /**
     * Delete a user by id.
     */
    public void deleteUser(Long id);
    
    /**
     * Update user preferences (cost preference, dietary restrictions, and daily calorie goal).
     */
    public User updateUserPreferences(String username, String costPreference, String dietaryRestrictions, Integer dailyCalorieGoal);

}
