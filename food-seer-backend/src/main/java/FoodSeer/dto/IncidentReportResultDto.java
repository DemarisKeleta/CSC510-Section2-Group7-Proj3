package FoodSeer.dto;

/**
 * Reply to an incident report. Staff get the counts; customers get only the
 * incident id, so a customer never learns how many others were affected.
 *
 * @param incidentId
 *            the new incident's id
 * @param exposedOrderCount
 *            exposed orders, or null in replies to customers
 * @param notifiedCount
 *            customers notified, or null in replies to customers
 */
public record IncidentReportResultDto(Long incidentId, Integer exposedOrderCount, Integer notifiedCount) {
}
