package FoodSeer.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import FoodSeer.service.AllergenService;

/**
 * Exposes the shared, site-wide allergen list - seeded with the original
 * fixed set, and extended whenever a customer adds a custom allergy that
 * isn't already known.
 */
@CrossOrigin("*")
@RestController
@RequestMapping("/api/allergens")
public class AllergenController {

    @Autowired
    private AllergenService allergenService;

    @GetMapping
    public List<String> getAllergens() {
        return allergenService.getAllAllergenNames();
    }

    @PostMapping
    public ResponseEntity<?> addAllergen(@RequestBody final Map<String, String> req) {
        try {
            final List<String> updated = allergenService.addAllergenIfNotExists(req.get("name"));
            return ResponseEntity.ok(updated);
        } catch (final IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
