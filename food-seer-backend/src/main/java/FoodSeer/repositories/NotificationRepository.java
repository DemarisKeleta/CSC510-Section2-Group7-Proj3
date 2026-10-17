package FoodSeer.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import FoodSeer.entity.Notification;

/**
 * Repository for customer safety notifications.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Finds every notification for one customer, newest first.
     *
     * @param recipientUsername
     *            the customer's username
     * @return the customer's notifications
     */
    List<Notification> findByRecipientUsernameOrderByCreatedAtDesc(String recipientUsername);

    /**
     * Counts a customer's notifications that have not been acknowledged.
     *
     * @param recipientUsername
     *            the customer's username
     * @return the number of unacknowledged notifications
     */
    long countByRecipientUsernameAndAcknowledgedFalse(String recipientUsername);

    /**
     * Finds every notification created for one incident.
     *
     * @param incidentId
     *            the incident id
     * @return the incident's notifications
     */
    List<Notification> findByIncidentId(Long incidentId);
}
