package FoodSeer.service;

import FoodSeer.entity.AllergyAcknowledgment;
import FoodSeer.entity.Order;
import FoodSeer.entity.User;
import FoodSeer.repositories.AllergyAcknowledgmentRepository;
import FoodSeer.repositories.OrderRepository;
import FoodSeer.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AllergyHandoffService {

    @Autowired
    private AllergyAcknowledgmentRepository acknowledgmentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserService userService;

    @Transactional
    public AllergyAcknowledgment acknowledge(Long orderId, String role) {
        User currentUser = userService.getCurrentUser();

        if (currentUser == null) {
            throw new IllegalStateException("Authentication required");
        }

        if (!role.equals("STAFF") && !role.equals("DRIVER")) {
            throw new IllegalArgumentException("Invalid acknowledgment role");
        }

        if (!currentUser.getRole().equals("ROLE_" + role)) {
            throw new SecurityException("Not authorized for this role");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found"));

        if (order.getIsFulfilled()
                || "Delivered".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException(
                "Cannot acknowledge an order that has already been completed");
        }

        if (order.getAllergyRestrictions() == null
                || order.getAllergyRestrictions().isBlank()) {
            throw new IllegalStateException(
                    "Order has no dietary restrictions to acknowledge");
        }

        if (role.equals("DRIVER")) {
            if (order.getDriver() == null
                    || !order.getDriver().getUsername()
                            .equals(currentUser.getUsername())) {
                throw new SecurityException(
                        "Driver is not assigned to this order");
            }

            if (!acknowledgmentRepository.existsByOrderIdAndActorRole(
                    orderId, "STAFF")) {
                throw new IllegalStateException(
                        "Restaurant must acknowledge first");
            }
        }

        if (acknowledgmentRepository.existsByOrderIdAndActorRole(
                orderId, role)) {
            throw new IllegalStateException(
                    "This role already acknowledged the order");
        }

        AllergyAcknowledgment acknowledgment =
                new AllergyAcknowledgment(
                        order,
                        currentUser.getUsername(),
                        role
                );

        return acknowledgmentRepository.save(acknowledgment);
    }

    public boolean isAcknowledged(Long orderId, String role) {
        return acknowledgmentRepository.existsByOrderIdAndActorRole(
                orderId, role);
    }

    public void validateStatusAccess(Long orderId) {
        User currentUser = userService.getCurrentUser();

         if (currentUser == null) {
            throw new SecurityException("Authentication required");
        }

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if ("ROLE_STAFF".equals(currentUser.getRole())) {
            return;
        }

        if ("ROLE_DRIVER".equals(currentUser.getRole())
                && order.getDriver() != null
                && currentUser.getUsername().equals(order.getDriver().getUsername())) {
            return;
    }

        throw new SecurityException("Not authorized to view this order's allergy handoff");
}
}
