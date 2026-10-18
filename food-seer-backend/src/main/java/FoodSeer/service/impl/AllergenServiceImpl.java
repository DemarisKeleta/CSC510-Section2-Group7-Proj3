package FoodSeer.service.impl;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import FoodSeer.entity.Allergen;
import FoodSeer.repositories.AllergenRepository;
import FoodSeer.service.AllergenService;

@Service
public class AllergenServiceImpl implements AllergenService {

    @Autowired
    private AllergenRepository allergenRepository;

    @Override
    public List<String> getAllAllergenNames() {
        return allergenRepository.findAll().stream()
                .map(Allergen::getName)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    @Override
    public List<String> addAllergenIfNotExists(final String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Allergen name cannot be blank");
        }
        final String normalized = name.trim().toUpperCase();
        if (!allergenRepository.existsByName(normalized)) {
            allergenRepository.save(new Allergen(normalized));
        }
        return getAllAllergenNames();
    }
}
