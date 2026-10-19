package FoodSeer.repositories;

// Optional wraps the possibly-missing row returned by findByIdForUpdate below
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
// takes the row-level lock for findByIdForUpdate below
import org.springframework.data.jpa.repository.Lock;
// explicit JPQL, since method-name derivation can't be used alongside @Lock here
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
// PESSIMISTIC_WRITE: the lock mode findByIdForUpdate below takes
import jakarta.persistence.LockModeType;

import FoodSeer.entity.Food;

/**
 * Food Repository
 */
public interface FoodRepository extends JpaRepository<Food, Long> {

    /**
     * Same lookup as {@link #findById}, but takes a row-level write lock
     * (SELECT ... FOR UPDATE) for the duration of the caller's transaction.
     * Used by rateFoodInOrder's read-modify-write of the rolling average,
     * so two concurrent ratings on the same food can't both read the same
     * starting average and have one silently overwrite the other.
     */
    // serializes concurrent rating updates on the same food row
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Food f where f.id = :id")
    Optional<Food> findByIdForUpdate(@Param("id") Long id);
}
