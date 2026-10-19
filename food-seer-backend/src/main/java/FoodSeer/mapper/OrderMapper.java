package FoodSeer.mapper;

import java.util.stream.Collectors;
import java.util.ArrayList; // Added simple list handling

import FoodSeer.dto.OrderDto;
import FoodSeer.dto.FoodDto;
import FoodSeer.entity.Order;
import FoodSeer.entity.Food;

/**
 * Mapper class for converting between Order and OrderDto.
 */
public class OrderMapper {

    /**
     * Converts an Order entity to an OrderDto.
     *
     * @param order the Order entity to convert
     * @return the corresponding OrderDto
     */
    public static OrderDto mapToOrderDto(final Order order) {
        final OrderDto dto = new OrderDto(order.getId(), order.getName());

        // Map Food entities to FoodDto and add them
        // Note: This complex stream recreates Food objects. 
        // Ideally, ensure these new Food objects have IDs so the frontend can use them!
        dto.setFoods(order.getFoods().stream()
                .map(FoodMapper::mapToFoodDto)
                .map(foodDto -> {
                    // We recreate the Food entity here to put inside the DTO
                    Food f = new Food(
                        foodDto.getFoodName(),
                        foodDto.getAmount(),
                        foodDto.getPrice(),
                        foodDto.getAllergies()
                    );
                    // CRITICAL: We must ensure the ID is preserved,
                    // otherwise the frontend cannot rate specific items!
                    f.setId(foodDto.getId());
                    // The 4-arg Food constructor above doesn't set these -
                    // copy them through so order history shows real values
                    // instead of silently dropping them.
                    // fixes a pre-existing data-loss bug: rating was silently dropped before
                    f.setRating(foodDto.getRating());
                    // same fix, for numberOfRatings
                    f.setNumberOfRatings(foodDto.getNumberOfRatings());
                    // same fix, for the new calories field
                    f.setCalories(foodDto.getCalories());
                    return f;
                })
                .collect(Collectors.toList()));

        dto.setIsFulfilled(order.getIsFulfilled());
        
        // --- ADDED THIS LINE ---
        // Copies the "Checklist" of rated items to the DTO
        dto.setRatedFoodIds(order.getRatedFoodIds());
        // -----------------------

        dto.setCost(order.getCost());
        dto.setStatus(order.getStatus());
        dto.setDeliveryCost(order.getDeliveryCost());
        // same class of bug as the Food fields above: Order.rating exists on both the
        // entity and this DTO but was never copied across, so it silently came back null
        dto.setRating(order.getRating());

//        dto.setDriverStats(DriverStatsMapper.mapToDriverStatsDto(order.getDriver()));

        return dto;
    }

    /**
     * Converts an OrderDto to an Order entity.
     *
     * @param orderDto the OrderDto to convert
     * @return the corresponding Order entity
     */
    public static Order mapToOrder(final OrderDto orderDto) {
        final Order order = new Order(orderDto.getId(), orderDto.getName());

        // Map FoodDto objects to Food entities
        order.setFoods(orderDto.getFoods().stream()
                .map(food -> new Food(
                        food.getFoodName(),
                        food.getAmount(),
                        food.getPrice(),
                        food.getAllergies()))
                .collect(Collectors.toList()));

        order.setIsFulfilled(orderDto.getIsFulfilled());

        order.setDriver(orderDto.getDriverStats());
        order.setDeliveryCost(orderDto.getDeliveryCost());
        
        // Note: We do NOT map ratedFoodIds back here.
        // The service layer handles adding IDs to the history one by one.
        
        return order;
    }
}