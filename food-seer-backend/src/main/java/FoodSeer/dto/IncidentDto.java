package FoodSeer.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * What restaurant staff see about an allergy incident. It holds counts only,
 * never the names of the customers who were notified.
 *
 * @param id
 *            incident id
 * @param source
 *            RESTAURANT or CUSTOMER
 * @param allergen
 *            normalized allergen, such as PEANUT
 * @param sourceOrderId
 *            for customer reports, the order the reaction followed
 * @param suspectFoodIds
 *            ids of the suspect foods
 * @param suspectFoodNames
 *            names of the suspect foods, in the same order as the ids
 * @param windowStart
 *            start of the exposure window
 * @param windowEnd
 *            end of the exposure window
 * @param reportedAt
 *            when the report was filed
 * @param exposedOrderCount
 *            orders inside the window that held a suspect food
 * @param notifiedCount
 *            customers who were sent a private notice
 * @param restaurantAcknowledgedAt
 *            when staff acknowledged it, or null
 * @param acknowledgedBy
 *            staff member who acknowledged it, or null
 * @param remedy
 *            what the restaurant offers affected customers, or null
 */
public record IncidentDto(Long id, String source, String allergen, Long sourceOrderId, List<Long> suspectFoodIds,
        List<String> suspectFoodNames, LocalDateTime windowStart, LocalDateTime windowEnd, LocalDateTime reportedAt,
        Integer exposedOrderCount, Integer notifiedCount, LocalDateTime restaurantAcknowledgedAt,
        String acknowledgedBy, String remedy) {
}
