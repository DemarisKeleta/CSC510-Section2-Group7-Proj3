package FoodSeer.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Body of a restaurant contamination report.
 *
 * @param allergen
 *            the allergen, such as "peanut"
 * @param foodIds
 *            ids of the dishes that may carry it
 * @param windowStart
 *            start of the exposure window, for example 2026-10-12T11:00
 * @param windowEnd
 *            end of the exposure window
 */
public record RestaurantIncidentRequestDto(String allergen, List<Long> foodIds, LocalDateTime windowStart,
        LocalDateTime windowEnd) {
}
