package FoodSeer.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import FoodSeer.dto.NotificationDto;
import FoodSeer.entity.Incident;
import FoodSeer.entity.Notification;
import FoodSeer.exception.ResourceNotFoundException;
import FoodSeer.repositories.IncidentRepository;
import FoodSeer.repositories.NotificationRepository;
import FoodSeer.service.NotificationService;

/**
 * Default {@link NotificationService}.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final IncidentRepository incidentRepository;

    /**
     * Creates the service.
     *
     * @param notificationRepository
     *            notifications
     * @param incidentRepository
     *            incidents, read to show the restaurant's remedy
     */
    public NotificationServiceImpl(final NotificationRepository notificationRepository,
            final IncidentRepository incidentRepository) {
        this.notificationRepository = notificationRepository;
        this.incidentRepository = incidentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(final String username) {
        requireUsername(username);
        final List<NotificationDto> result = new ArrayList<>();
        for (final Notification notification : notificationRepository
                .findByRecipientUsernameOrderByCreatedAtDesc(username)) {
            result.add(toDto(notification));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnacknowledged(final String username) {
        requireUsername(username);
        return notificationRepository.countByRecipientUsernameAndAcknowledgedFalse(username);
    }

    @Override
    @Transactional
    public NotificationDto acknowledge(final Long notificationId, final String username) {
        requireUsername(username);
        if (notificationId == null) {
            throw new IllegalArgumentException("A notification id is required.");
        }
        final Notification notification = notificationRepository.findById(notificationId).orElseThrow(
                () -> new ResourceNotFoundException("Notification does not exist with id " + notificationId));
        if (!username.equals(notification.getRecipientUsername())) {
            throw new AccessDeniedException("You can only acknowledge your own notifications.");
        }
        if (!notification.isAcknowledged()) {
            notification.setAcknowledged(true);
            notification.setAcknowledgedAt(LocalDateTime.now());
        }
        final Notification saved = notificationRepository.save(notification);
        return toDto(saved);
    }

    /**
     * Builds the customer's view of a notice, adding the restaurant's remedy
     * from the incident when there is one.
     */
    private NotificationDto toDto(final Notification notification) {
        String remedy = null;
        if (notification.getIncidentId() != null) {
            final Optional<Incident> incident = incidentRepository.findById(notification.getIncidentId());
            if (incident.isPresent()) {
                remedy = incident.get().getRemedy();
            }
        }
        return new NotificationDto(notification.getId(), notification.getIncidentId(), notification.getOrderId(),
                notification.getMessage(), notification.getCreatedAt(), notification.isAcknowledged(),
                notification.getAcknowledgedAt(), remedy);
    }

    private void requireUsername(final String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("A username is required.");
        }
    }
}
