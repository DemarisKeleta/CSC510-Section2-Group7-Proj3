package FoodSeer.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import FoodSeer.dto.DriverStatsDto;
import FoodSeer.entity.DriverStats;
import FoodSeer.mapper.DriverStatsMapper;
import FoodSeer.service.AllergyHandoffService;
import FoodSeer.service.DriverStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import FoodSeer.dto.InventoryDto;
import FoodSeer.dto.OrderDto;
import FoodSeer.entity.Food;
import FoodSeer.entity.Order;
import FoodSeer.exception.ResourceNotFoundException;
import FoodSeer.mapper.OrderMapper;
import FoodSeer.entity.User;
import FoodSeer.repositories.FoodRepository;
import FoodSeer.repositories.OrderRepository;
import FoodSeer.service.InventoryService;
import FoodSeer.service.OrderService;
import FoodSeer.service.UserService;

/**
 * Implementation of the OrderService interface for managing food orders.
 */
@Service
public class OrderServiceImpl implements OrderService {

    Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);
    /** Repository for food items. */
    @Autowired
    private FoodRepository foodRepository;

    /** Repository for allergy handoff */
    @Autowired
    private AllergyHandoffService allergyHandoffService;

    /** Repository for orders. */
    @Autowired
    private OrderRepository orderRepository;

    /** Inventory service for stock management. */
    @Autowired
    private InventoryService inventoryService;

    /** User service for getting current user. */
    @Autowired
    private UserService userService;

    @Autowired
    private DriverStatsService driverStatsService;

    /**
     * Creates an order with the given information.
     *
     * @param orderDto order to create
     * @return created order
     */
    @Override
    public OrderDto createOrder(final OrderDto orderDto) {
        // Load actual Food entities from database (managed entities)
        final List<Food> foods = new ArrayList<>();
        for (final Food food : orderDto.getFoods()) {
            final Food f = foodRepository.findById(food.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("A Food item does not exist within the order."));
            foods.add(f);
        }

        // Create order entity directly (not using mapper to avoid creating new Food objects)
        final Order order = new Order();
        order.setName(orderDto.getName());
        order.setFoods(foods);
        order.setIsFulfilled(false);
        order.setCost(orderDto.getCost());
        order.setStatus(orderDto.getStatus());
        order.setDeliveryCost(orderDto.getDeliveryCost());
        // stamp the creation time so CalorieBudgetServiceImpl can later tell which orders were placed "today"
        order.setCreatedAt(java.time.Instant.now());
        
        // Set the current user as the owner of this order
        final User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        order.setUser(currentUser);
        order.setAllergyRestrictions(currentUser.getDietaryRestrictions());
        
        final Order savedOrder = orderRepository.save(order);
        return OrderMapper.mapToOrderDto(savedOrder);
    }

    /**
     * Returns the order with the given id.
     *
     * @param orderId order's id
     * @return the order with the given id
     * @throws ResourceNotFoundException if the order doesn't exist
     */
    @Override
    public OrderDto getOrderById(final Long orderId) {
        final Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order does not exist with id " + orderId));
        return OrderMapper.mapToOrderDto(order);
    }

    /**
     * Returns a list of all the orders.
     *
     * @return all the orders
     */
    @Override
    public List<OrderDto> getAllOrders() {
        final List<Order> orders = orderRepository.findAll();
        return orders.stream().map(OrderMapper::mapToOrderDto).collect(Collectors.toList());
    }

    /**
     * Fulfills the order by checking food availability and updating inventory.
     *
     * @param orderId The id of the order to fulfill
     * @return the updated OrderDto
     */
    @Override
    public OrderDto fulfillOrder(final long orderId) {
        final Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order does not exist with id " + orderId));

        // Count how many of each food is in the order
        final Map<Long, Long> foodCounts = order.getFoods().stream()
                .collect(Collectors.groupingBy(Food::getId, Collectors.counting()));

        // Check inventory and deduct quantities
        for (final Map.Entry<Long, Long> entry : foodCounts.entrySet()) {
            final Long foodId = entry.getKey();
            final Long quantityNeeded = entry.getValue();
            
            final Food food = foodRepository.findById(foodId)
                    .orElseThrow(() -> new ResourceNotFoundException("Food not found with id " + foodId));
            
            // Check if enough stock
            if (food.getAmount() < quantityNeeded) {
                throw new IllegalArgumentException("Not enough stock to fulfill the order for " + food.getFoodName() 
                        + ". Need: " + quantityNeeded + ", Available: " + food.getAmount());
            }
            
            // Deduct from inventory
            food.setAmount((int) (food.getAmount() - quantityNeeded));
            foodRepository.save(food);
        }

        order.setIsFulfilled(true);
        final Order savedOrder = orderRepository.save(order);
        return OrderMapper.mapToOrderDto(savedOrder);
    }

    /**
     * Checks if all ingredients/foods for an order item are available.
     *
     * @param inventoryDto The current inventory
     * @param food         The food item to check
     * @return true if available, false otherwise
     */
    private boolean checkFoodAvailability(final InventoryDto inventoryDto, final Food food) {
        final Map<String, Food> inventoryMap = inventoryDto.getFoods().stream()
                .collect(Collectors.toMap(Food::getFoodName, f -> f));

        final Food inventoryFood = inventoryMap.get(food.getFoodName());
        return inventoryFood != null && food.getAmount() <= inventoryFood.getAmount();
    }

    /**
     * Deducts the ordered food amount from the inventory.
     *
     * @param inventoryDto The inventory data
     * @param food         The food item to deduct
     */
    private void updateInventory(final InventoryDto inventoryDto, final Food food) {
        final Map<String, Food> inventoryMap = inventoryDto.getFoods().stream()
                .collect(Collectors.toMap(Food::getFoodName, f -> f));

        final Food inventoryFood = inventoryMap.get(food.getFoodName());
        if (inventoryFood != null) {
            inventoryFood.setAmount(inventoryFood.getAmount() - food.getAmount());
        }

        inventoryService.updateInventory(inventoryDto);
    }

    /**
     * Returns all fulfilled orders.
     *
     * @return list of fulfilled orders
     */
    @Override
    public List<OrderDto> getAllFulfilledOrders() {
        return getAllOrders().stream()
                .filter(OrderDto::getIsFulfilled)
                .collect(Collectors.toList());
    }

    /**
     * Returns all unfulfilled orders.
     *
     * @return list of unfulfilled orders
     */
    @Override
    public List<OrderDto> getAllUnfulfilledOrders() {
        return getAllOrders().stream()
                .filter(o -> !o.getIsFulfilled())
                .collect(Collectors.toList());
    }

    /**
     * Returns all orders for the current authenticated user.
     *
     * @return list of current user's orders
     */
    @Override
    public List<OrderDto> getCurrentUserOrders() {
        final User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        
        final List<Order> orders = orderRepository.findByUser(currentUser);
        return orders.stream().map(OrderMapper::mapToOrderDto).collect(Collectors.toList());
    }

    /**
     * Returns fulfilled orders for the current authenticated user.
     *
     * @return list of current user's fulfilled orders
     */
    @Override
    public List<OrderDto> getCurrentUserFulfilledOrders() {
        final User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        
        final List<Order> orders = orderRepository.findByUserAndIsFulfilled(currentUser, true);
        return orders.stream().map(OrderMapper::mapToOrderDto).collect(Collectors.toList());
    }

    /**
     * Returns unfulfilled orders for the current authenticated user.
     *
     * @return list of current user's unfulfilled orders
     */
    @Override
    public List<OrderDto> getCurrentUserUnfulfilledOrders() {
        final User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        
        final List<Order> orders = orderRepository.findByUserAndIsFulfilled(currentUser, false);
        return orders.stream().map(OrderMapper::mapToOrderDto).collect(Collectors.toList());
    }

    @Override
    public OrderDto claimOrder(final Long orderId) {
        final User currentUser = userService.getCurrentUser();

        if (currentUser == null
                || !"ROLE_DRIVER".equals(currentUser.getRole())) {
            throw new SecurityException("Only drivers can claim orders");
        }

        final Order order = orderRepository.findById(orderId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Order does not exist with id " + orderId));

        if (!"Placed".equals(order.getStatus())
                || order.getDriver() != null) {
            throw new IllegalStateException(
                "Order is not available to claim");
        }

        order.setDriver(
            DriverStatsMapper.mapToDriverStats(
                    driverStatsService.getDriverStats(
                            currentUser.getUsername())));

        order.setStatus("Claimed");

        return OrderMapper.mapToOrderDto(orderRepository.save(order));
    }

    @Override
    public OrderDto updateOrder(final Long orderId, String username, String status) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Order not found: " + orderId));

        User currentUser = userService.getCurrentUser();

        if (currentUser == null
                || !"ROLE_DRIVER".equals(currentUser.getRole())) {
            throw new SecurityException("Only drivers can update delivery status");
        }

        String authenticatedUsername = currentUser.getUsername();

        if (order.getDriver() == null
                || !authenticatedUsername.equals(order.getDriver().getUsername())) {
            throw new SecurityException("You are not assigned to this order");
        }

        if ("Picked Up".equals(status)) {
            if (!"Claimed".equals(order.getStatus())) {
                throw new IllegalStateException("Order must be claimed before pickup");
            }

            boolean hasRestrictions =
                order.getAllergyRestrictions() != null
                && !order.getAllergyRestrictions().isBlank();

            if (hasRestrictions) {
                if (!allergyHandoffService.isAcknowledged(orderId, "STAFF")) {
                    throw new IllegalStateException(
                        "Restaurant staff must acknowledge allergy warning first");
                }

                if (!allergyHandoffService.isAcknowledged(orderId, "DRIVER")) {
                    throw new IllegalStateException(
                        "Driver must acknowledge allergy warning before pickup");
                    }
                }

                order.setStatus("Picked Up");

            } else if ("Delivered".equals(status)) {
                if (!"Picked Up".equals(order.getStatus())) {
                    throw new IllegalStateException(
                        "Order must be picked up before delivery");
                }

                order.setStatus("Delivered");
                order.setIsFulfilled(true);

                driverStatsService.updateTotalEarnings(
                    authenticatedUsername, order.getDeliveryCost());

            } else {
                throw new IllegalArgumentException("Invalid delivery status");
            }

            orderRepository.save(order);
            return OrderMapper.mapToOrderDto(order);
        }

    @Override
    public List<OrderDto> getAvailableOrders(){
        List<Order> availableOrders = orderRepository.findByStatus("Placed");
        return availableOrders.stream().map(OrderMapper::mapToOrderDto).collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getActiveOrders(String username) {
        List<Order> activeOrders =
            orderRepository.findByDriverUsernameAndStatusIn(
                username,
                List.of("Claimed", "Picked Up")
            );

        return activeOrders.stream()
            .map(OrderMapper::mapToOrderDto)
            .collect(Collectors.toList());
    }
}
