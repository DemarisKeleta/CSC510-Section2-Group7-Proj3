package FoodSeer.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import FoodSeer.dto.NotificationDto;
import FoodSeer.exception.ErrorDetails;
import FoodSeer.service.NotificationService;

/**
 * A customer's own allergy safety notices.
 *
 * <ul>
 * <li>GET /api/notifications/my: the signed-in customer's notices, newest first.</li>
 * <li>GET /api/notifications/my/unread-count: how many are not acknowledged.</li>
 * <li>POST /api/notifications/{id}/acknowledge: acknowledge one of them.</li>
 * </ul>
 */
@CrossOrigin("*")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Creates the controller.
     *
     * @param notificationService
     *            the notification service
     */
    public NotificationController(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Lists the signed-in customer's notices.
     *
     * @param authentication
     *            the signed-in customer
     * @return their notices
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my")
    public List<NotificationDto> getMyNotifications(final Authentication authentication) {
        return notificationService.getNotifications(authentication.getName());
    }

    /**
     * Counts the signed-in customer's unacknowledged notices.
     *
     * @param authentication
     *            the signed-in customer
     * @return {"count": n}
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my/unread-count")
    public Map<String, Long> getMyUnreadCount(final Authentication authentication) {
        final Map<String, Long> body = new HashMap<>();
        body.put("count", notificationService.countUnacknowledged(authentication.getName()));
        return body;
    }

    /**
     * Acknowledges one of the signed-in customer's notices.
     *
     * @param id
     *            the notice
     * @param authentication
     *            the signed-in customer
     * @return the updated notice
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/{id}/acknowledge")
    public NotificationDto acknowledge(@PathVariable("id") final Long id, final Authentication authentication) {
        return notificationService.acknowledge(id, authentication.getName());
    }

    /**
     * Turns bad input from the service into 400 Bad Request with its message.
     *
     * @param ex
     *            the validation error
     * @return 400 with the message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDetails> handleBadInput(final IllegalArgumentException ex) {
        final ErrorDetails details = new ErrorDetails(new Date(), ex.getMessage(), "Invalid notification request");
        return new ResponseEntity<>(details, HttpStatus.BAD_REQUEST);
    }
}
