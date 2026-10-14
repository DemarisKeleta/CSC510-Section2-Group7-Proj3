package FoodSeer.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import FoodSeer.entity.Incident;

/**
 * Repository for allergy incident reports.
 */
@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    /**
     * Finds every incident, newest report first.
     *
     * @return all incidents
     */
    List<Incident> findAllByOrderByReportedAtDesc();
}
