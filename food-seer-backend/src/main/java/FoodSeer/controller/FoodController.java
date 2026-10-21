package FoodSeer.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam; // Added this import
import org.springframework.web.bind.annotation.RestController;

import FoodSeer.dto.FoodDto;
// response shape for the new withinCalorieBudget endpoint below
import FoodSeer.dto.FoodRecommendationDto;
// supplies the budget-aware recommendation logic
import FoodSeer.service.CalorieBudgetService;
import FoodSeer.service.FoodService;
// used to resolve the currently authenticated user for the new endpoint below
import FoodSeer.service.UserService;

/**
 * Controller class for food
 */
@CrossOrigin ( "*" )
@RestController
@RequestMapping ( "/api/foods" )
public class FoodController {

    /**
     * Food service to use
     */
    @Autowired
    private FoodService foodService;

    // computes the calorie-aware recommendation for getFoodsWithinCalorieBudget below
    @Autowired
    private CalorieBudgetService calorieBudgetService;

    // resolves the authenticated caller for getFoodsWithinCalorieBudget below
    @Autowired
    private UserService userService;

    /**
     * Gets the food based on the ID parameter
     *
     * @param id
     * The ID of the food to return
     * @return The food
     */
    @GetMapping ( "{id}" )
    public ResponseEntity<FoodDto> getFood ( @PathVariable ( "id" ) final Long id ) {
        final FoodDto foodDto = foodService.getFoodById( id );
        return ResponseEntity.ok( foodDto );
    }

    /**
     * POST mapping to create a food
     *
     * @param foodDto
     * The food DTO to create
     * @return response from creation
     */
    @PostMapping
    public ResponseEntity<FoodDto> createFood ( @RequestBody final FoodDto foodDto ) {
        if ( foodService.isDuplicateName( foodDto.getFoodName() ) ) {
            return new ResponseEntity<>( foodDto, HttpStatus.CONFLICT );
        }

        // Validate food before saving
        if ( !foodService.isValidFood( foodDto ) ) {
            return new ResponseEntity<>( foodDto, HttpStatus.BAD_REQUEST );
        }

        final FoodDto savedFoodDto = foodService.createFood( foodDto );
        return ResponseEntity.ok( savedFoodDto );
    }

    /**
     * REST API method to provide GET access to all foods in the system
     *
     * @return JSON representation of all foods
     */
    @GetMapping
    public List<FoodDto> getFoods () {
        return foodService.getAllFoods();
    }

    /**
     * Returns menu items that fit within the current customer's remaining
     * calorie budget for today, per the Fitness-Based Calorie Tracking use
     * case. Falls back to the full menu (with budgetApplied=false) if the
     * budget cannot be computed yet.
     *
     * @return the calorie-aware recommendation
     */
    // new endpoint: GET /api/foods/withinCalorieBudget
    @GetMapping ( "/withinCalorieBudget" )
    public ResponseEntity<FoodRecommendationDto> getFoodsWithinCalorieBudget () {
        // resolve who's calling, via Spring Security's context
        final var currentUser = userService.getCurrentUser();
        // no authenticated user means this request can't be fulfilled
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // delegate the actual filtering logic to CalorieBudgetService
        return ResponseEntity.ok( calorieBudgetService.getRecommendedFoodsFor( currentUser ) );
    }

    /**
     * Deletes the food based on params
     *
     * @param foodId
     * Id of the food to delete
     * @return Response Entity
     */
    @DeleteMapping ( "{id}" )
    public ResponseEntity<String> deleteFood ( @PathVariable ( "id" ) final Long foodId ) {
        try {
            foodService.deleteFood( foodId );
            return ResponseEntity.ok( "Food deleted successfully." );
        } catch (final IllegalStateException e) {
            // Food is part of unfulfilled orders
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /**
     * Updates an existing food's details (amount, price, and allergies).
     *
     * @param foodDto
     * FoodDto containing the updated fields
     * @return ResponseEntity containing the updated FoodDto
     */
    @PostMapping ( "/updateFood" )
    public ResponseEntity<FoodDto> updateFood ( @RequestBody final FoodDto foodDto ) {
        // Check if food exists first by ID or name
        final String name = foodDto.getFoodName();
        if (name == null || name.trim().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // Verify that food exists
        if (!foodService.isDuplicateName(name)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // Validate updated values
        if (foodDto.getAmount() < 0 || foodDto.getPrice() < 0) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // Perform the update using new service signature
        final FoodDto updatedFood = foodService.updateFood(
            name,
            foodDto.getAmount(),
            foodDto.getPrice(),
            foodDto.getAllergies(),
            // pass the incoming calories value through to the service layer
            foodDto.getCalories()
        );

        return ResponseEntity.ok(updatedFood);
    }

    // --- NEW METHOD ---

    /**
     * Rates a food item within a specific order context.
     * Endpoint: POST /api/foods/orders/{orderId}/{foodId}/rate?rating=5.0
     *
     * @param orderId The ID of the order
     * @param foodId The ID of the food
     * @param rating The rating value (0-5)
     * @return The updated FoodDto with new average
     */
    @PostMapping("/orders/{orderId}/{foodId}/rate")
    public ResponseEntity<?> rateFood(
            @PathVariable final Long orderId,
            @PathVariable final Long foodId,
            @RequestParam final Double rating) {
        
        try {
            final FoodDto updatedFood = foodService.rateFoodInOrder(orderId, foodId, rating);
            return ResponseEntity.ok(updatedFood);
        } catch (final IllegalStateException e) {
            // Returns 409 Conflict if order is not fulfilled
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (final IllegalArgumentException e) {
            // Returns 400 Bad Request if food is not in order or invalid rating
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (final Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while rating the food.");
        }
    }

}