package FoodSeer.entity;

import java.math.BigDecimal;
// Instant backs the new createdAt field below
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;

/**
 * Represents an Order in the FoodSeer system.
 * Each order can contain multiple foods, and can be marked as fulfilled or not.
 */
@Entity
@Table(name = "orders")
public class Order {

    /** Order ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Order name */
    private String name;

    /** User who created this order */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** List of foods in the order */
    @ManyToMany
    private List<Food> foods = new ArrayList<>();

    /** Boolean used to track if the order has been fulfilled */
    private boolean isFulfilled;

    private BigDecimal cost;

    private String status;

    private BigDecimal rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "driver_username",
            referencedColumnName = "username",
            foreignKey = @ForeignKey(name = "fk_orders_driver")
    )
    private DriverStats driver;

    private BigDecimal deliveryCost;

    /**
     * When this order was placed. Used to compute "calories consumed today"
     * for the Fitness-Based Calorie Tracking feature - set once at creation,
     * never updated afterward.
     */
    // no column annotation needed - plain Instant mapping is enough for ddl-auto=update to add the column

    @Column(length = 1000)
    private String allergyRestrictions;

    public String getAllergyRestrictions() {
        return allergyRestrictions;
    }

    public void setAllergyRestrictions(String allergyRestrictions) {
        this.allergyRestrictions = allergyRestrictions;
    }
    
    private Instant createdAt;

    // getter used by CalorieBudgetServiceImpl to filter today's orders
    public Instant getCreatedAt() {
        return createdAt;
    }

    // setter called once, from OrderServiceImpl.createOrder
    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    /**
     * A set of Food IDs that have already been rated in this order.
     * Using a Set prevents duplicates and allows fast lookups.
     */
    @ElementCollection
    private Set<Long> ratedFoodIds = new HashSet<>();

    /**
     * Default constructor for Hibernate.
     */
    public Order() {
        // Default constructor
    }

    /**
     * Creates an Order with the given ID and name.
     *
     * @param id   the order ID
     * @param name the order name
     */
    public Order(final Long id, final String name) {
        this.id = id;
        this.name = name;
        this.foods = new ArrayList<>();
        this.isFulfilled = false;
        this.ratedFoodIds = new HashSet<>();
    }

    /**
     * Gets the order ID.
     *
     * @return the order ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the order ID (used by Hibernate).
     *
     * @param id the order ID
     */
    @SuppressWarnings("unused")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Gets the name of the order.
     *
     * @return the order name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the order.
     *
     * @param name the order name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Adds a food item to the order.
     *
     * @param food the food to add
     */
    public void addFood(final Food food) {
        this.foods.add(food);
    }

    /**
     * Gets the list of foods in the order.
     *
     * @return the list of foods
     */
    public List<Food> getFoods() {
        return this.foods;
    }

    /**
     * Sets the list of foods in the order.
     *
     * @param foods the list of foods
     */
    public void setFoods(final List<Food> foods) {
        this.foods = foods;
    }

    /**
     * Checks if the order is fulfilled.
     *
     * @return true if fulfilled, false otherwise
     */
    public boolean getIsFulfilled() {
        return isFulfilled;
    }

    /**
     * Sets whether the order is fulfilled.
     *
     * @param isFulfilled true if fulfilled, false otherwise
     */
    public void setIsFulfilled(final boolean isFulfilled) {
        this.isFulfilled = isFulfilled;
    }

    /**
     * Gets the user who created this order.
     *
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the user who created this order.
     *
     * @param user the user
     */
    public void setUser(final User user) {
        this.user = user;
    }

    // --- NEW METHODS FOR RATING LOGIC ---

    /**
     * Gets the set of Food IDs that have already been rated.
     *
     * @return Set of Long IDs
     */
    public Set<Long> getRatedFoodIds() {
        return ratedFoodIds;
    }

    /**
     * Sets the list of rated food IDs.
     *
     * @param ratedFoodIds the set of IDs
     */
    public void setRatedFoodIds(final Set<Long> ratedFoodIds) {
        this.ratedFoodIds = ratedFoodIds;
    }

    /**
     * Marks a specific food ID as rated for this order.
     *
     * @param foodId The ID of the food
     */
    public void addRatedFoodId(final Long foodId) {
        this.ratedFoodIds.add(foodId);
    }

    /**
     * Checks if a specific food has already been rated in this order.
     *
     * @param foodId The ID to check
     * @return true if already rated, false otherwise
     */
    public boolean hasFoodBeenRated(final Long foodId) {
        return this.ratedFoodIds.contains(foodId);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public DriverStats getDriver() {
        return driver;
    }

    public void setDriver(DriverStats driver) {
        this.driver = driver;
    }

    public BigDecimal getDeliveryCost() {
        return deliveryCost;
    }

    public void setDeliveryCost(BigDecimal deliveryCost) {
        this.deliveryCost = deliveryCost;
    }
}