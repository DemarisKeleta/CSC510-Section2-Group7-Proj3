package FoodSeer.entity;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import FoodSeer.constant.IncidentSource;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * An allergy incident: either the restaurant reported a contaminated dish, or a
 * customer reported a reaction. The trace-back feature uses it to find other
 * customers who may have been exposed.
 *
 * The reporter, source order and suspect foods are stored as plain values (a
 * username and ids) instead of foreign keys, so an incident record survives
 * later deletion of a user, order or food.
 */
@Entity
@Table(name = "incidents")
public class Incident {

    /** Incident id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Who reported it. */
    @Enumerated(EnumType.STRING)
    @Column(name = "report_source", nullable = false, length = 20)
    private IncidentSource source;

    /** The allergen, in normalized form such as "PEANUT". */
    @Column(nullable = false, length = 100)
    private String allergen;

    /** Username of the person who filed the report. */
    @Column(nullable = false)
    private String reporterUsername;

    /** For customer reports, the order the reaction followed. Null for restaurant reports. */
    private Long sourceOrderId;

    /** Ids of the foods suspected of carrying the allergen. */
    @ElementCollection
    @CollectionTable(name = "incident_suspect_foods", joinColumns = @JoinColumn(name = "incident_id"))
    @Column(name = "food_id")
    private Set<Long> suspectFoodIds = new LinkedHashSet<>();

    /** Start of the time window, inclusive. */
    @Column(nullable = false)
    private LocalDateTime windowStart;

    /** End of the time window, inclusive. */
    @Column(nullable = false)
    private LocalDateTime windowEnd;

    /** When the report was filed. */
    @Column(nullable = false)
    private LocalDateTime reportedAt;

    /** How many orders inside the window held a suspect food. */
    private Integer exposedOrderCount;

    /** How many customers were sent a private notice. */
    private Integer notifiedCount;

    /** When restaurant staff acknowledged the incident, or null if not yet. */
    private LocalDateTime restaurantAcknowledgedAt;

    /** Username of the staff member who acknowledged it. */
    private String acknowledgedBy;

    /**
     * What the restaurant offers affected customers, such as a refund or a
     * remake. Shown on every customer notice for this incident, so the notice
     * reads as the restaurant's own recall.
     */
    @Column(length = 500)
    private String remedy;

    /**
     * Constructor for Hibernate.
     */
    public Incident() {
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public IncidentSource getSource() {
        return source;
    }

    public void setSource(final IncidentSource source) {
        this.source = source;
    }

    public String getAllergen() {
        return allergen;
    }

    public void setAllergen(final String allergen) {
        this.allergen = allergen;
    }

    public String getReporterUsername() {
        return reporterUsername;
    }

    public void setReporterUsername(final String reporterUsername) {
        this.reporterUsername = reporterUsername;
    }

    public Long getSourceOrderId() {
        return sourceOrderId;
    }

    public void setSourceOrderId(final Long sourceOrderId) {
        this.sourceOrderId = sourceOrderId;
    }

    public Set<Long> getSuspectFoodIds() {
        return suspectFoodIds;
    }

    public void setSuspectFoodIds(final Set<Long> suspectFoodIds) {
        this.suspectFoodIds = suspectFoodIds;
    }

    public LocalDateTime getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(final LocalDateTime windowStart) {
        this.windowStart = windowStart;
    }

    public LocalDateTime getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(final LocalDateTime windowEnd) {
        this.windowEnd = windowEnd;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(final LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public Integer getExposedOrderCount() {
        return exposedOrderCount;
    }

    public void setExposedOrderCount(final Integer exposedOrderCount) {
        this.exposedOrderCount = exposedOrderCount;
    }

    public Integer getNotifiedCount() {
        return notifiedCount;
    }

    public void setNotifiedCount(final Integer notifiedCount) {
        this.notifiedCount = notifiedCount;
    }

    public LocalDateTime getRestaurantAcknowledgedAt() {
        return restaurantAcknowledgedAt;
    }

    public void setRestaurantAcknowledgedAt(final LocalDateTime restaurantAcknowledgedAt) {
        this.restaurantAcknowledgedAt = restaurantAcknowledgedAt;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(final String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public String getRemedy() {
        return remedy;
    }

    public void setRemedy(final String remedy) {
        this.remedy = remedy;
    }
}
