package FoodSeer.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Food. This class mirrors the fields of the
 * Food entity without annotations.
 */
public class FoodDto {

    /** Food Id */
    private Long id;

    /**
     * Name of the food
     */
    private String foodName;

    /**
     * Field representing the amount of the food
     */
    private int amount;

    /**
     * Price of the food
     */
    private int price;

    /**
     * Current Average Rating
     */
    private Double rating;

    /**
     * Total number of ratings
     */
    private Integer numberOfRatings;

    /**
     * List representing allergies associated with the food
     */
    private List<String> allergies = new ArrayList<>();

    /**
     * Calorie count for this food item. Nullable: not every item has one recorded.
     */
    // mirrors Food.calories - Integer so a missing value serializes as JSON null, not 0
    private Integer calories;

    /**
     * Default constructor
     */
    public FoodDto () {
        super();
    }

    /**
     * Constructor with params
     * * Note: We do not include rating/numberOfRatings here because 
     * new items always start at 0.
     *
     * @param name
     * Name of food
     * @param amount
     * The amount of the food
     * @param price
     * The price of the food
     * @param allergies
     * The allergies associated with the food
     */
    public FoodDto ( final String name, final int amount, final int price, final List<String> allergies ) {
        super();
        this.foodName = name.toUpperCase();
        this.amount = amount;
        this.price = price;
        this.allergies = new ArrayList<>();
        if (allergies != null) {
            for ( final String allergy : allergies ) {
                this.allergies.add( allergy.toUpperCase() );
            }
        }
    }

    /**
     * Get food name
     *
     * @return the food name
     */
    public String getFoodName () {
        return foodName;
    }

    /**
     * Sets the food name to @param name
     *
     * @param name
     * The name to set
     */
    public void setFoodName ( final String name ) {
        this.foodName = name;
    }

    /**
     * Gets the amount field
     *
     * @return The amount field
     */
    public int getAmount () {
        return amount;
    }

    /**
     * Sets the amount field to @param amount
     *
     * @param amount
     * The amount to set
     */
    public void setAmount ( final int amount ) {
        this.amount = amount;
    }

    /**
     * Gets the price field
     *
     * @return The price of the food
     */
    public int getPrice () {
        return price;
    }

    /**
     * Sets the price field to @param price
     *
     * @param price
     * The price to set
     */
    public void setPrice ( final int price ) {
        this.price = price;
    }

    /**
     * Gets the allergies associated with the food
     *
     * @return The allergies list
     */
    public List<String> getAllergies () {
        return allergies;
    }

    /**
     * Sets the allergies list to @param allergies
     *
     * @param allergies
     * The allergies to set
     */
    public void setAllergies ( final List<String> allergies ) {
        this.allergies = allergies;
    }

    /**
     * Get ID
     *
     * @return The id
     */
    public Long getId () {
        return id;
    }

    /**
     * Set Id to @param id
     *
     * @param id
     * The id to set
     */
    public void setId ( final Long id ) {
        this.id = id;
    }

    // --- NEW GETTERS AND SETTERS ---

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getNumberOfRatings() {
        return numberOfRatings;
    }

    public void setNumberOfRatings(Integer numberOfRatings) {
        this.numberOfRatings = numberOfRatings;
    }

    // getter for the calories field above
    public Integer getCalories() {
        return calories;
    }

    // setter for the calories field above
    public void setCalories(Integer calories) {
        this.calories = calories;
    }
}