package FoodSeer.repositories;

import FoodSeer.entity.AllergyAcknowledgment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AllergyAcknowledgmentRepository
        extends JpaRepository<AllergyAcknowledgment, Long> {

    Optional<AllergyAcknowledgment> findByOrderIdAndActorRole(
            Long orderId, String actorRole);

    boolean existsByOrderIdAndActorRole(
            Long orderId, String actorRole);
}