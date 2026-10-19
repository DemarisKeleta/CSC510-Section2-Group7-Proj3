package FoodSeer.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A private safety notice sent to one customer because an allergy incident may
 * have exposed them. It never names other customers or who filed the report.
 *
 * The recipient is stored as a username and the incident and order as ids, so
 * the notice survives later deletion of those records.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    /** Notification id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The incident that caused this notice. */
    @Column(nullable = false)
    private Long incidentId;

    /** The recipient's order that may have been exposed. */
    private Long orderId;

    /** Username of the customer being warned. */
    @Column(nullable = false)
    private String recipientUsername;

    /** Text shown to the customer. */
    @Column(nullable = false, length = 1000)
    private String message;

    /** When the notice was created. */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** True once the customer has acknowledged the notice. */
    @Column(nullable = false)
    private boolean acknowledged = false;

    /** When the customer acknowledged it, or null if they have not. */
    private LocalDateTime acknowledgedAt;

    /**
     * Constructor for Hibernate.
     */
    public Notification() {
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(final Long incidentId) {
        this.incidentId = incidentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(final Long orderId) {
        this.orderId = orderId;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public void setRecipientUsername(final String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(final boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(final LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }
}
