// java
package FoodSeer.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import FoodSeer.service.FoodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import FoodSeer.dto.InventoryDto;
import FoodSeer.dto.OrderDto;
import FoodSeer.entity.Food;
import FoodSeer.entity.User;
import FoodSeer.service.InventoryService;
import FoodSeer.service.OrderService;

/**
 * Tests the OrderRepository for persistence and retrieval of food orders.
 */
@SpringBootTest
class OrderRepositoryTest {

    /** Reference to order repository */
    @Autowired
    private OrderRepository orderRepository;

    /** Reference to food repository */
    @Autowired
    private FoodRepository foodRepository;

    /** Reference to food service */
    @Autowired
    private FoodService foodService;

    /** Reference to order service */
    @Autowired
    private OrderService orderService;

    /** Reference to InventoryService */
    @Autowired
    private InventoryService inventoryService;

    /** Reference to User repository */
    @Autowired
    private UserRepository userRepository;

    /** Reference to MealLog repository */
    // needed to clear meal_logs below before userRepository.deleteAll() runs
    @Autowired
    private MealLogRepository mealLogRepository;

    /**
     * Sets up the test case by clearing tables and adding initial food/inventory data.
     */
    @BeforeEach
    public void setUp() throws Exception {
        orderRepository.deleteAll();
        // clear meal_logs before users, same reasoning as orders above: its FK blocks user deletion otherwise
        mealLogRepository.deleteAll();
        foodRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user that matches @WithMockUser username
        final User customer = User.builder()
                .username("customer")
                .email("customer@test.com")
                .password("password")
                .role("ROLE_CUSTOMER")
                .build();
        userRepository.save(customer);

        // Initialize inventory with foods
        final List<Food> foods = new ArrayList<>();

        final Food f1 = new Food("COFFEE", 10, 5, List.of("CAFFEINE"));
        final Food f2 = new Food("MILK", 8, 3, List.of("LACTOSE"));
        final Food f3 = new Food("SUGAR", 15, 2, List.of("GLUCOSE"));

        foods.add(f1);
        foods.add(f2);
        foods.add(f3);

        final InventoryDto inventoryDto = new InventoryDto(1L, foods);
        inventoryService.createInventory(inventoryDto);

        foodRepository.saveAll(foods);
    }

    /**
     * Tests that orders can be created, saved, and retrieved successfully.
     */
    @Test
    @Transactional
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void testOrderRepository() {
        final List<Food> foods = foodRepository.findAll();

        // Create an order with a subset of foods
        final OrderDto orderDto = new OrderDto(0L, "Order1");
        orderDto.setFoods(foods.subList(0, 2));

        final OrderDto savedOrder = orderService.createOrder(orderDto);

        // Verify that the order was persisted correctly
        assertEquals(savedOrder.getId(),
                orderRepository.getReferenceById(savedOrder.getId()).getId(),
                "The saved and retrieved order IDs should match");
    }

    /**
     * Tests repository query methods: findOrdersContainingFood and findByUser / findByUserAndIsFulfilled.
     */
    @Test
    @Transactional
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void testFindOrdersContainingFoodAndFindByUser() {
        final List<Food> foods = foodRepository.findAll();

        // Create an order that includes the first food item
        final OrderDto orderDto = new OrderDto(0L, "OrderWithCoffee");
        orderDto.setFoods(List.of(foods.get(0))); // COFFEE

        final OrderDto savedOrder = orderService.createOrder(orderDto);

        // Verify findOrdersContainingFood returns the created order when queried with that food
        List<Food> allFoods = foodRepository.findAll();
        Food coffee = allFoods.stream().filter(f -> "COFFEE".equals(f.getFoodName())).findFirst().orElseThrow();

        var ordersWithCoffee = orderRepository.findOrdersContainingFood(coffee);
        assertEquals(1, ordersWithCoffee.size(), "Should find one order containing COFFEE");
        assertTrue(ordersWithCoffee.stream().anyMatch(o -> o.getId().equals(savedOrder.getId())));

        // Verify findByUser and findByUserAndIsFulfilled
        User user = userRepository.findAll().get(0); // the created customer
        var ordersByUser = orderRepository.findByUser(user);
        assertTrue(ordersByUser.size() >= 1, "User should have at least one order");

        var unfulfilledByUser = orderRepository.findByUserAndIsFulfilled(user, false);
        assertTrue(unfulfilledByUser.stream().anyMatch(o -> o.getId().equals(savedOrder.getId())),
                "Saved order should appear as unfulfilled for the user");
    }
}
