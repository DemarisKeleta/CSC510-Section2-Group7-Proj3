package FoodSeer.service;

// Optional lets the caller distinguish "no data" from "data present"
import java.util.Optional;

// the record this service returns
import FoodSeer.dto.FitnessData;

/**
 * Reads a user's fitness activity from a generated data file standing
 * in for a real fitness-app export (see Use Case: Fitness-Based Calorie
 * Tracking). Returns an empty Optional - "not tracked" - rather than
 * zero, whenever no file exists or it cannot be parsed, since "no data"
 * and "zero calories burned" are not the same thing.
 */
// contract for reading one user's fitness data; implemented by FitnessDataServiceImpl
public interface FitnessDataService {

    /**
     * Reads today's fitness data for the given username.
     *
     * @param username the user to look up
     * @return the parsed FitnessData, or empty if no file exists or it is malformed
     */
    // single method: given a username, return today's fitness data if available
    Optional<FitnessData> getTodaysFitnessData(String username);

    /**
     * Writes a plausible mock fitness-data file for the given username, if
     * one doesn't already exist. Called on customer registration so every
     * new customer has fitness data from day one, instead of needing the
     * generate_fitness_data.py script run by hand per account.
     *
     * @param username the user to generate fitness data for
     */
    // mirrors scripts/generate_fitness_data.py's formula, so both paths produce the same shape of data
    void generateFitnessDataIfMissing(String username);
}
