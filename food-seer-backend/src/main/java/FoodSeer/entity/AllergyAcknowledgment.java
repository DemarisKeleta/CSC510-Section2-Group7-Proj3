package FoodSeer.entity;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(
    name = "allergy_acknowledgments",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"order_id", "actor_role"}
    )
)
public class AllergyAcknowledgment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "actor_username", nullable = false)
    private String actorUsername;

    @Column(name = "actor_role", nullable = false)
    private String actorRole;

    @Column(nullable = false)
    private Instant acknowledgedAt;

    public AllergyAcknowledgment() {}

    public AllergyAcknowledgment(
            Order order, String actorUsername, String actorRole) {
        this.order = order;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.acknowledgedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getActorRole() {
        return actorRole;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }
}