package FoodSeer.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import FoodSeer.dto.OrderDto;
import FoodSeer.entity.Order;
import FoodSeer.entity.Food;
import FoodSeer.entity.DriverStats;

public class OrderMapperTest {

    private Order order;
    private OrderDto orderDto;
    private List<Food> foods;
    private Set<Long> ratedFoodIds;

    @BeforeEach
    public void setUp() {
        // Initialize test data
        foods = new ArrayList<>();

        List<String> pizzaAllergies = Arrays.asList("Gluten", "Dairy");
        Food food1 = new Food("Pizza", 2, 15, pizzaAllergies);
        food1.setId(1L);
        food1.setRating(4.5);
        food1.setNumberOfRatings(10);

        List<String> burgerAllergies = Arrays.asList("Gluten");
        Food food2 = new Food("Burger", 1, 9, burgerAllergies);
        food2.setId(2L);

        foods.add(food1);
        foods.add(food2);

        ratedFoodIds = new HashSet<>(Arrays.asList(1L, 2L));

        // Create test Order
        order = new Order(1L, "Test Order");
        order.setFoods(foods);
        order.setIsFulfilled(false);
        order.setCost(new BigDecimal("25.00"));
        order.setStatus("Placed");
        order.setDeliveryCost(new BigDecimal("5.00"));
        order.setRating(new BigDecimal("4.8"));
        order.setRatedFoodIds(ratedFoodIds);
    }


    @Test
    public void testMapToOrder_Complete() {
        orderDto = new OrderDto(1L, "DTO Order");
        orderDto.setIsFulfilled(true);
        orderDto.setDeliveryCost(new BigDecimal("4.50"));
        orderDto.setFoods(foods);

        DriverStats driverStats = new DriverStats();
        orderDto.setDriverStats(driverStats);

        Order mappedOrder = OrderMapper.mapToOrder(orderDto);

        // Basic fields
        assertNotNull(mappedOrder);
        assertEquals(orderDto.getId(), mappedOrder.getId());
        assertEquals(orderDto.getName(), mappedOrder.getName());
        assertEquals(orderDto.getIsFulfilled(), mappedOrder.getIsFulfilled());
        assertEquals(orderDto.getDeliveryCost(), mappedOrder.getDeliveryCost());
        assertEquals(driverStats, mappedOrder.getDriver());

        // Foods mapping
        assertNotNull(mappedOrder.getFoods());
        assertEquals(2, mappedOrder.getFoods().size());
        assertEquals("PIZZA", mappedOrder.getFoods().get(0).getFoodName());
        assertEquals("BURGER", mappedOrder.getFoods().get(1).getFoodName());
    }

    @Test
    public void testMapToOrderDto_EdgeCases() {
        // Empty foods list
        order.setFoods(new ArrayList<>());
        order.setRatedFoodIds(null);

        OrderDto dto = OrderMapper.mapToOrderDto(order);

        assertNotNull(dto.getFoods());
        assertEquals(0, dto.getFoods().size());
        assertNull(dto.getRatedFoodIds());

        // Food with null allergies
        Food foodWithNullAllergies = new Food("Salad", 1, 7, null);
        foodWithNullAllergies.setId(3L);
        order.getFoods().add(foodWithNullAllergies);

        dto = OrderMapper.mapToOrderDto(order);

        assertEquals(1, dto.getFoods().size());
        assertNotNull(dto.getFoods().get(0).getAllergies());
        assertEquals(0, dto.getFoods().get(0).getAllergies().size());
    }

    /**
     * mapToOrderDto must carry Order.rating through to the DTO, the same as
     * it already does for cost/status/deliveryCost - it was previously
     * silently dropped, the same bug class as the Food fields fixed earlier.
     */
    @Test
    public void testMapToOrderDto_CarriesRatingThrough() {
        // order.rating is set to 4.8 in setUp()
        final OrderDto dto = OrderMapper.mapToOrderDto(order);

        assertEquals(new BigDecimal("4.8"), dto.getRating());
    }

    @Test
    public void testOrder_RatingHelperMethods() {
        assertTrue(order.hasFoodBeenRated(1L));
        assertTrue(order.hasFoodBeenRated(2L));
        assertFalse(order.hasFoodBeenRated(3L));

        order.addRatedFoodId(3L);
        assertTrue(order.hasFoodBeenRated(3L));
        assertEquals(3, order.getRatedFoodIds().size());

        // Adding duplicate should not increase size
        order.addRatedFoodId(3L);
        assertEquals(3, order.getRatedFoodIds().size());
    }

    @Test
    public void testOrderDto_InitializationAndMethods() {
        OrderDto dto = new OrderDto();

        assertNotNull(dto.getFoods());
        assertNotNull(dto.getRatedFoodIds());
        assertEquals(0, dto.getFoods().size());
        assertFalse(dto.getIsFulfilled());

        Food newFood = foods.get(0);
        dto.addFood(newFood);

        assertEquals(1, dto.getFoods().size());
        assertEquals(newFood, dto.getFoods().get(0));
    }
}