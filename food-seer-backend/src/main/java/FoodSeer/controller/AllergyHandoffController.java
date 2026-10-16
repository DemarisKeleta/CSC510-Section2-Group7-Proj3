package FoodSeer.controller;

import FoodSeer.entity.AllergyAcknowledgment;
import FoodSeer.service.AllergyHandoffService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders/{orderId}/allergy-handoff")
@CrossOrigin("*")
public class AllergyHandoffController {

    @Autowired
    private AllergyHandoffService allergyHandoffService;

    @PreAuthorize("hasAnyRole('STAFF', 'DRIVER')")
    @PostMapping("/acknowledge")
    public ResponseEntity<?> acknowledge(
            @PathVariable Long orderId) {

        try {
            // Role is derived from authentication, not user input.
            AllergyAcknowledgment acknowledgment;

            try {
                acknowledgment =
                        allergyHandoffService.acknowledge(orderId, "STAFF");
            } catch (SecurityException e) {
                acknowledgment =
                        allergyHandoffService.acknowledge(orderId, "DRIVER");
            }

            return ResponseEntity.ok(Map.of(
                    "orderId", orderId,
                    "role", acknowledgment.getActorRole(),
                    "actor", acknowledgment.getActorUsername(),
                    "acknowledgedAt",
                    acknowledgment.getAcknowledgedAt().toString()
            ));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }
}