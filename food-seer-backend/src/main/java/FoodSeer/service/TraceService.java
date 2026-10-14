package FoodSeer.service;

import java.time.LocalDateTime;
import java.util.List;

import FoodSeer.dto.IncidentDto;
import FoodSeer.entity.Incident;

/**
 * Traces an allergy incident back to the customers it may have exposed and
 * warns the ones whose saved restrictions include the allergen.
 */
public interface TraceService {

    /**
     * Handles a report from the restaurant that some dishes may carry an
     * allergen during a time window.
     *
     * @param reporterUsername
     *            username of the staff member filing the report
     * @param allergen
     *            the allergen, such as "peanut"
     * @param suspectFoodIds
     *            ids of the foods that may carry the allergen
     * @param windowStart
     *            start of the exposure window, inclusive
     * @param windowEnd
     *            end of the exposure window, inclusive
     * @return the saved incident, the exposed orders and the notifications created
     * @throws IllegalArgumentException
     *             if the allergen is blank, no foods are given, or the window is missing or reversed
     * @throws FoodSeer.exception.ResourceNotFoundException
     *             if a suspect food does not exist
     */
    TraceResult reportRestaurantIncident(String reporterUsername, String allergen, List<Long> suspectFoodIds,
            LocalDateTime windowStart, LocalDateTime windowEnd);

    /**
     * Handles a report from a customer who had a reaction after an order.
     *
     * The suspect foods are the foods in that order whose allergen tags include
     * the allergen. If none declare it, the allergen was not listed on the menu,
     * so every food in the order is suspect. The window runs from the
     * configured number of hours before the order to the same number after it.
     *
     * @param reporterUsername
     *            username of the customer; must own the order
     * @param orderId
     *            the order the reaction followed
     * @param allergen
     *            the allergen the customer reacted to
     * @return the saved incident, the exposed orders and the notifications created
     * @throws IllegalArgumentException
     *             if the allergen is blank, or the order has no time or no foods
     * @throws FoodSeer.exception.ResourceNotFoundException
     *             if the order does not exist
     * @throws org.springframework.security.access.AccessDeniedException
     *             if the order belongs to someone else
     */
    TraceResult reportCustomerReaction(String reporterUsername, Long orderId, String allergen);

    /**
     * Lists every incident for restaurant staff, newest first.
     *
     * @return incident summaries with counts only, never customer names
     */
    List<IncidentDto> listIncidents();

    /**
     * Records that restaurant staff have seen an incident and what they offer
     * affected customers. The first acknowledgement time is kept; calling again
     * only updates the remedy.
     *
     * @param incidentId
     *            the incident
     * @param staffUsername
     *            the staff member acknowledging it
     * @param remedy
     *            what the restaurant offers, 1 to 500 characters
     * @return the updated incident summary
     * @throws IllegalArgumentException
     *             if the staff username or remedy is blank, or the remedy is too long
     * @throws FoodSeer.exception.ResourceNotFoundException
     *             if the incident does not exist
     */
    IncidentDto acknowledgeIncident(Long incidentId, String staffUsername, String remedy);

    /**
     * Turns an incident into the summary staff see.
     *
     * @param incident
     *            the incident
     * @return its summary
     */
    IncidentDto describe(Incident incident);
}
