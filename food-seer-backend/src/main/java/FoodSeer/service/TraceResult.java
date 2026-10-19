package FoodSeer.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import FoodSeer.entity.Incident;
import FoodSeer.entity.Notification;

/**
 * What a trace produced: the saved incident, the orders found to be exposed,
 * and the notifications created for affected customers.
 */
public final class TraceResult {

    private final Incident incident;
    private final List<Long> exposedOrderIds;
    private final List<Notification> notifications;

    /**
     * Creates a result.
     *
     * @param incident
     *            the saved incident
     * @param exposedOrderIds
     *            ids of every order inside the window that held a suspect food,
     *            whether or not its customer was notified
     * @param notifications
     *            notifications created, one per affected customer
     */
    public TraceResult(final Incident incident, final List<Long> exposedOrderIds,
            final List<Notification> notifications) {
        this.incident = incident;
        this.exposedOrderIds = Collections.unmodifiableList(new ArrayList<>(exposedOrderIds));
        this.notifications = Collections.unmodifiableList(new ArrayList<>(notifications));
    }

    public Incident getIncident() {
        return incident;
    }

    public List<Long> getExposedOrderIds() {
        return exposedOrderIds;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    /**
     * Gets the usernames of the customers who were notified.
     *
     * @return usernames in notification order
     */
    public List<String> getNotifiedUsernames() {
        final List<String> usernames = new ArrayList<>();
        for (final Notification notification : notifications) {
            usernames.add(notification.getRecipientUsername());
        }
        return usernames;
    }
}
