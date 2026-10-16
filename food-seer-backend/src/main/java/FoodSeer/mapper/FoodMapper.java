package FoodSeer.mapper;

import FoodSeer.dto.FoodDto;
import FoodSeer.entity.Food;

/**
 * Mapper class for foods
 */
public class FoodMapper {

    /**
     * Maps food to DTO
     *
     * @param food
     * The food to map
     * @return The mapped food DTO.
     */
    public static FoodDto mapToFoodDto ( final Food food ) {
        final FoodDto foodDto = new FoodDto();
        foodDto.setId( food.getId() );
        foodDto.setFoodName( food.getFoodName() );
        foodDto.setAmount( food.getAmount() );
        foodDto.setPrice( food.getPrice() );
        foodDto.setAllergies( food.getAllergies() );

        // --- ADD THESE TWO LINES ---
        foodDto.setRating( food.getRating() );
        foodDto.setNumberOfRatings( food.getNumberOfRatings() );
        // ---------------------------

        // carry the calorie value through to the DTO, same as every other field above
        foodDto.setCalories( food.getCalories() );

        return foodDto;
    }

    /**
     * Maps food DTO to food
     *
     * @param foodDto
     * The DTO to map
     * @return The mapped food
     */
    public static Food mapToFood ( final FoodDto foodDto ) {
        final Food food = new Food();
        food.setId( foodDto.getId() );
        food.setFoodName( foodDto.getFoodName() );
        food.setAmount( foodDto.getAmount() );
        food.setPrice( foodDto.getPrice() );
        food.setAllergies( foodDto.getAllergies() );
        // carry the calorie value back into the entity when converting DTO -> entity
        food.setCalories( foodDto.getCalories() );

        // Note: We do NOT map rating here.
        // The Food Entity automatically initializes rating to 0.0 for new items.
        
        return food;
    }

}