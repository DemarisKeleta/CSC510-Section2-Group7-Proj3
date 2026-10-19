package FoodSeer.controller;

import java.util.List;

import FoodSeer.dto.UpdateOrderDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import FoodSeer.dto.OrderDto;
import FoodSeer.exception.ResourceNotFoundException;
import FoodSeer.service.OrderService;

/**
 * Controller for Orders in the FoodSeer system.
 * Provides endpoints for managing and fulfilling food orders.
 */
@CrossOrigin("*")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    Logger logger = LoggerFactory.getLogger(OrderController.class);
    /** Connection to OrderService */
    @Autowired
    private OrderService orderService;

    /**
     * Retrieves all orders in the system.
     *
     * @return JSON list of all orders
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @GetMapping
    public List<OrderDto> getOrders() {
        return orderService.getAllOrders();
    }

    /**
     * Retrieves all fulfilled orders.
     *
     * @return JSON list of fulfilled orders
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @GetMapping("/fulfilledOrders")
    public List<OrderDto> getFulfilledOrders() {
        return orderService.getAllFulfilledOrders();
    }

    /**
     * Retrieves all unfulfilled orders.
     *
     * @return JSON list of unfulfilled orders
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER', 'DRIVER')")
    @GetMapping("/unfulfilledOrders")
    public List<OrderDto> getUnfulfilledOrders() {
        return orderService.getAllUnfulfilledOrders();
    }

    /**
     * Creates a new order.
     *
     * @param orderDto the order to create
     * @return ResponseEntity containing the created order
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody final OrderDto orderDto) {
        logger.info("Creating order: {}", orderDto.toString());
        final OrderDto savedOrderDto = orderService.createOrder(orderDto);
        return ResponseEntity.ok(savedOrderDto);
    }

    /**
     * Marks an order as fulfilled.
     *
     * @param orderDto the order to fulfill
     * @return ResponseEntity with status depending on fulfillment result
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @PostMapping("/fulfillOrder")
    public ResponseEntity<OrderDto> fulfillOrder(@RequestBody final OrderDto orderDto) {
        try {
            orderService.getOrderById(orderDto.getId());
        } catch (final ResourceNotFoundException e) {
            return new ResponseEntity<>(orderDto, HttpStatus.PRECONDITION_FAILED);
        }

        final OrderDto existingOrder = orderService.getOrderById(orderDto.getId());

        if (existingOrder.getIsFulfilled()) {
            return new ResponseEntity<>(orderDto, HttpStatus.GONE);
        }

        try {
            final OrderDto updatedOrder = orderService.fulfillOrder(existingOrder.getId());
            return ResponseEntity.ok(updatedOrder);
        } catch (final Exception e) {
            return new ResponseEntity<>(orderDto, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Retrieves a specific order by ID.
     *
     * @param id the ID of the order
     * @return ResponseEntity containing the order or 404 if not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable("id") final Long id) {
        try {
            final OrderDto orderDto = orderService.getOrderById(id);
            return ResponseEntity.ok(orderDto);
        } catch (final ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * Retrieves all orders for the current authenticated user.
     *
     * @return JSON list of current user's orders
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my-orders")
    public List<OrderDto> getMyOrders() {
        return orderService.getCurrentUserOrders();
    }

    /**
     * Retrieves fulfilled orders for the current authenticated user.
     *
     * @return JSON list of current user's fulfilled orders
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my-orders/fulfilled")
    public List<OrderDto> getMyFulfilledOrders() {
        return orderService.getCurrentUserFulfilledOrders();
    }

    /**
     * Retrieves unfulfilled orders for the current authenticated user.
     *
     * @return JSON list of current user's unfulfilled orders
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my-orders/unfulfilled")
    public List<OrderDto> getMyUnfulfilledOrders() {
        return orderService.getCurrentUserUnfulfilledOrders();
    }

    // a DRIVER may only update orders under their own username; ADMIN may update any -
    // without this, any driver could pass a different driver's username and both
    // reassign that driver's deliveries and credit/discredit their earnings (IDOR)
    @PreAuthorize("hasRole('ADMIN') or (hasRole('DRIVER') and @driverStatsAccess.isOwner(authentication, #updateOrderDto.username()))")
    @PostMapping("/{id}")
    public ResponseEntity<?> updateOrderStatus(@PathVariable("id") final Long id, @RequestBody UpdateOrderDto updateOrderDto){
        logger.info("Updating order status for order ID: {}", id);
        try {
            final OrderDto updatedOrderDto = orderService.updateOrder(id, updateOrderDto.username(), updateOrderDto.status());
            return ResponseEntity.ok(updatedOrderDto);
        } catch (final IllegalArgumentException e) {
            // invalid state transition (e.g. "Delivered" before "Picked Up") - a 400, not a crash
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    @GetMapping("/availableOrders")
    public ResponseEntity<?> getAvailableOrders(){
        logger.info("Fetching all placed orders which are not picked up yet");
        List<OrderDto> availableOrders = orderService.getAvailableOrders();
        return ResponseEntity.ok(availableOrders);
    }

    // Reuses the generic username-ownership resolver introduced for DriverStatsController.
    @PreAuthorize("hasRole('ADMIN') or (hasRole('DRIVER') and @driverStatsAccess.isOwner(authentication, #username))")
    @GetMapping("/activeOrders/{username}")
    public ResponseEntity<?> getActiveOrders(@P("username") @PathVariable String username){
        logger.info("Fetching all picked up orders for the driver: {}", username);
        List<OrderDto> activeOrders = orderService.getActiveOrders(username);
        return ResponseEntity.ok(activeOrders);
    }
}
