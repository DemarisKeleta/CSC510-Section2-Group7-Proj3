package FoodSeer.service;

import java.util.List;

import FoodSeer.dto.OrderDto;
import FoodSeer.exception.ResourceNotFoundException;

/**
 * Interface defining the behaviors for managing food orders in FoodSeer.
 */
public interface OrderService {

    /**
     * Creates a new order with the given information.
     *
     * @param orderDto
     *            the order to create
     * @return the created order as a DTO
     */
    OrderDto createOrder(OrderDto orderDto);

    /**
     * Retrieves an order by its ID.
     *
     * @param orderId
     *            the ID of the order
     * @return the order with the specified ID
     * @throws ResourceNotFoundException
     *             if no order exists with the provided ID
     */
    OrderDto getOrderById(Long orderId) throws ResourceNotFoundException;

    /**
     * Retrieves all orders in the system.
     *
     * @return a list of all orders
     */
    List<OrderDto> getAllOrders();

    /**
     * Marks an order as fulfilled.
     *
     * @param orderId
     *            the ID of the order to fulfill
     * @return the updated order as a DTO
     */
    OrderDto fulfillOrder(long orderId);

    /**
     * Retrieves all fulfilled orders.
     *
     * @return a list of fulfilled orders
     */
    List<OrderDto> getAllFulfilledOrders();

    /**
     * Retrieves all unfulfilled orders.
     *
     * @return a list of unfulfilled orders
     */
    List<OrderDto> getAllUnfulfilledOrders();

    /**
     * Retrieves all orders for the current authenticated user.
     *
     * @return a list of orders belonging to the current user
     */
    List<OrderDto> getCurrentUserOrders();

    /**
     * Retrieves fulfilled orders for the current authenticated user.
     *
     * @return a list of fulfilled orders belonging to the current user
     */
    List<OrderDto> getCurrentUserFulfilledOrders();

    /**
     * Retrieves unfulfilled orders for the current authenticated user.
     *
     * @return a list of unfulfilled orders belonging to the current user
     */
    List<OrderDto> getCurrentUserUnfulfilledOrders();

    OrderDto updateOrder(final Long id, String username, String status);
    OrderDto claimOrder(Long orderId);

    List<OrderDto> getAvailableOrders();

    List<OrderDto> getActiveOrders(String username);
}
