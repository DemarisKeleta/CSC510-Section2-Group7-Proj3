package FoodSeer.service.impl;

// File is used to locate and check the per-user JSON file on disk
import java.io.File;
// Optional lets us return "not tracked" instead of null or a fake zero
import java.util.Optional;

// @Value injects the configurable fitness-data directory from application.properties
import org.springframework.beans.factory.annotation.Value;
// marks this class as a Spring-managed service bean
import org.springframework.stereotype.Service;

// Jackson's ObjectMapper parses the JSON file into a FitnessData record
import com.fasterxml.jackson.databind.ObjectMapper;

// the record this method returns
import FoodSeer.dto.FitnessData;
// the interface this class implements
import FoodSeer.service.FitnessDataService;

/**
 * Reads a per-user generated JSON file standing in for a real
 * fitness-app export. Degrades to "not tracked" (empty Optional) on a
 * missing or malformed file, rather than failing the request or
 * assuming zero.
 */
// registers this class with Spring so it can be @Autowired elsewhere
@Service
public class FitnessDataServiceImpl implements FitnessDataService {

    // directory fitness files live in; defaults to "fitness-data" if not overridden in application.properties
    @Value("${app.fitness-data-dir:fitness-data}")
    private String fitnessDataDir;

    // one shared Jackson mapper instance for parsing the JSON files
    private final ObjectMapper objectMapper = new ObjectMapper();

    // implements the interface's single method
    @Override
    public Optional<FitnessData> getTodaysFitnessData(final String username) {
        // guard against a null/blank username rather than letting a bad file path form
        if (username == null || username.isBlank()) {
            // no username to look up, so there is nothing to return
            return Optional.empty();
        }

        // build the expected file path: <fitnessDataDir>/<username>.json
        final File file = new File(fitnessDataDir, username + ".json");
        // if the file doesn't exist (or isn't a regular file), this user's fitness data is "not tracked"
        if (!file.exists() || !file.isFile()) {
            // nothing to read, so report not-tracked rather than guessing
            return Optional.empty();
        }

        // attempt to read and parse the file; any failure here means malformed data
        try {
            // deserialize the JSON file straight into a FitnessData record
            final FitnessData data = objectMapper.readValue(file, FitnessData.class);
            // wrap the parsed result so callers can tell "present" from "absent"
            return Optional.ofNullable(data);
        } catch (final Exception e) {
            // Malformed file: degrade to "not tracked" rather than failing the request.
            // swallow the parse error and report not-tracked, per the use case's extension for this case
            return Optional.empty();
        }
    }
}
