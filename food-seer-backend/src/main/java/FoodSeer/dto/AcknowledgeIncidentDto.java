package FoodSeer.dto;

/**
 * Body of a staff acknowledgement.
 *
 * @param remedy
 *            what the restaurant offers affected customers, such as "Full
 *            refund or a free remake"
 */
public record AcknowledgeIncidentDto(String remedy) {
}
