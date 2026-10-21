package FoodSeer.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column; // Added this import
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * This class represents a Food entity.
 */
@Entity
@Table ( name = "foods" )
public class Food {

    /**
     * Id for the class
     */
    @Id
    @GeneratedValue ( strategy = GenerationType.IDENTITY )
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
     * Current Average Rating (Stars)
     * Default is 0.0. columnDefinition handles existing MySQL rows.
     */
    @Column(name = "rating", nullable = false, columnDefinition = "double default 0.0")
    private Double rating = 0.0;

    /**
     * Total number of people who rated this item
     * Default is 0.
     */
    @Column(name = "number_of_ratings", nullable = false, columnDefinition = "int default 0")
    private Integer numberOfRatings = 0;

    /**
     * List representing allergies associated with the food
     */
    @ElementCollection
    private List<String> allergies = new ArrayList<>();

    /**
     * Calorie count for this food item. Nullable: staff may not have
     * recorded a value yet, which is distinct from a food having 0 calories.
     */
    // Integer (not int) so "no value recorded" can be represented as null
    private Integer calories;

    /**
     * Constructor for Hibernate
     */
    public Food () {
        super();
    }

    /**
     * Constructor with params
     * Note: We do NOT pass rating here, it defaults to 0 automatically.
     *
     * @param name
     * Name of the food
     * @param amount
     * The amount of the food
     * @param price
     * The price of the food
     * @param allergies
     * The list of allergies associated with the food
     */
    public Food ( final String name, final int amount, final int price, final List<String> allergies ) {
        super();
        this.foodName = name.toUpperCase();
        this.amount = amount;
        this.price = price;
        // rating and numberOfRatings are automatically set to 0 via the field definition

        this.allergies = new ArrayList<>();
        if (allergies != null) {
            for ( final String allergy : allergies ) {
                this.allergies.add( allergy.toUpperCase() );
            }
        }
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

    // --- NEW GETTERS AND SETTERS ---

    /**
     * Gets the current rating
     * @return rating
     */
    public Double getRating() {
        return rating;
    }

    /**
     * Sets the rating
     * @param rating
     */
    public void setRating(final Double rating) {
        this.rating = rating;
    }

    /**
     * Gets the number of ratings
     * @return number of ratings
     */
    public Integer getNumberOfRatings() {
        return numberOfRatings;
    }

    /**
     * Sets the number of ratings
     * @param numberOfRatings
     */
    public void setNumberOfRatings(final Integer numberOfRatings) {
        this.numberOfRatings = numberOfRatings;
    }

    /**
     * Gets the calorie count
     * @return calories, or null if not recorded
     */
    // returns the raw field, null included, so callers can detect "not recorded"
    public Integer getCalories() {
        return calories;
    }

    /**
     * Sets the calorie count
     * @param calories
     */
    // plain field setter, used by FoodMapper and the service layer
    public void setCalories(final Integer calories) {
        this.calories = calories;
    }
}