package FoodSeer.dto;

import java.time.LocalDateTime;

/**
 * A safety notice as the customer sees it.
 *
 * @param id
 *            notification id
 * @param incidentId
 *            the incident behind it
 * @param orderId
 *            the customer's order that may have been exposed
 * @param message
 *            the notice text
 * @param createdAt
 *            when it was created
 * @param acknowledged
 *            true once the customer has acknowledged it
 * @param acknowledgedAt
 *            when they acknowledged it, or null
 * @param remedy
 *            what the restaurant offers, or null if it has not responded yet
 */
public record NotificationDto(Long id, Long incidentId, Long orderId, String message, LocalDateTime createdAt,
        boolean acknowledged, LocalDateTime acknowledgedAt, String remedy) {
}
