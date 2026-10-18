package FoodSeer.service;

import java.util.List;

/**
 * Manages the shared, site-wide list of known allergen names.
 */
public interface AllergenService {

    /**
     * Returns every known allergen name, sorted alphabetically.
     */
    List<String> getAllAllergenNames();

    /**
     * Adds a new allergen name to the shared list if it doesn't already
     * exist (case-insensitive). Normalizes the name to uppercase, matching
     * the vocabulary already used on Food.allergies and User.dietaryRestrictions.
     *
     * @param name the allergen name to add
     * @return the full, updated, sorted list of allergen names
     * @throws IllegalArgumentException if name is null or blank
     */
    List<String> addAllergenIfNotExists(String name);
}
