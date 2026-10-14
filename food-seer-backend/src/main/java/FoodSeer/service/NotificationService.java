package FoodSeer.service;

import java.util.List;

import FoodSeer.dto.NotificationDto;

/**
 * Lets a customer read and acknowledge their own safety notices.
 */
public interface NotificationService {

    /**
     * Lists a customer's notices, newest first.
     *
     * @param username
     *            the customer
     * @return their notices, each with the restaurant's remedy if it has one
     * @throws IllegalArgumentException
     *             if the username is blank
     */
    List<NotificationDto> getNotifications(String username);

    /**
     * Counts a customer's notices that have not been acknowledged.
     *
     * @param username
     *            the customer
     * @return the number of unacknowledged notices
     * @throws IllegalArgumentException
     *             if the username is blank
     */
    long countUnacknowledged(String username);

    /**
     * Marks one of the customer's own notices as acknowledged. Doing it twice
     * keeps the first acknowledgement time.
     *
     * @param notificationId
     *            the notice
     * @param username
     *            the customer; must be the notice's recipient
     * @return the updated notice
     * @throws FoodSeer.exception.ResourceNotFoundException
     *             if the notice does not exist
     * @throws org.springframework.security.access.AccessDeniedException
     *             if the notice belongs to someone else
     */
    NotificationDto acknowledge(Long notificationId, String username);
}
