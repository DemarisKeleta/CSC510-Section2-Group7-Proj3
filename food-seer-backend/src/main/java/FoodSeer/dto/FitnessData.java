package FoodSeer.dto;

/**
 * Represents one day's fitness activity for a user, as read from a
 * generated data file standing in for a real fitness-app export
 * (see FitnessDataService). Not a JPA entity - this is never persisted
 * to the database, only read from the file on request.
 */
// record holding one day's parsed fitness-file contents: who it belongs to, the date, and the three tracked metrics
public record FitnessData(String username, String date, Integer caloriesBurned, Integer steps, Integer activeMinutes) {
}
