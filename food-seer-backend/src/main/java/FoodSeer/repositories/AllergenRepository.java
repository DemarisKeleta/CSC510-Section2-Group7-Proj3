package FoodSeer.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import FoodSeer.entity.Allergen;

public interface AllergenRepository extends JpaRepository<Allergen, Long> {

    Optional<Allergen> findByName(String name);

    boolean existsByName(String name);
}
