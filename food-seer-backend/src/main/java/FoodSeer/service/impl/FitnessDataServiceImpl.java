package FoodSeer.service.impl;

// File is used to locate and check the per-user JSON file on disk
import java.io.File;
// the generated file's "date" field uses today's date, same as the Python generator
import java.time.LocalDate;
// Optional lets us return "not tracked" instead of null or a fake zero
import java.util.Optional;
// source of the plausible-but-random steps/activeMinutes/caloriesBurned values below
import java.util.Random;

// logs a write failure in generateFitnessDataIfMissing without failing the caller's flow
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    // logs a write failure in generateFitnessDataIfMissing without failing the caller's flow
    private static final Logger logger = LoggerFactory.getLogger(FitnessDataServiceImpl.class);

    // directory fitness files live in; defaults to "fitness-data" if not overridden in application.properties
    @Value("${app.fitness-data-dir:fitness-data}")
    private String fitnessDataDir;

    // one shared Jackson mapper instance for parsing the JSON files
    private final ObjectMapper objectMapper = new ObjectMapper();

    // seeds the random steps/activeMinutes/caloriesBurned generated below
    private final Random random = new Random();

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

    // implements the interface's file-generation method
    @Override
    public void generateFitnessDataIfMissing(final String username) {
        // guard against a null/blank username rather than letting a bad file path form
        if (username == null || username.isBlank()) {
            // nothing to generate for
            return;
        }

        // build the expected file path: <fitnessDataDir>/<username>.json
        final File file = new File(fitnessDataDir, username + ".json");
        // an existing file (hand-generated or from a prior registration) is left untouched
        if (file.exists()) {
            // don't overwrite real or previously-generated data
            return;
        }

        // plausible daily step count, same range as the Python generator
        final int steps = 2000 + random.nextInt(10001);
        // plausible daily active-minutes count, same range as the Python generator
        final int activeMinutes = 10 + random.nextInt(81);
        // same rough steps/activeMinutes -> calories formula as the Python generator, so both
        // paths produce data with the same shape and plausibility
        final int caloriesBurned = Math.max(0, (int) (steps * 0.04 + activeMinutes * 3) + random.nextInt(61) - 30);

        // assemble today's generated fitness data for this user
        final FitnessData data = new FitnessData(username, LocalDate.now().toString(), caloriesBurned, steps, activeMinutes);

        try {
            // ensure the fitness-data directory exists before writing into it
            file.getParentFile().mkdirs();
            // write the generated data as the same JSON shape getTodaysFitnessData reads back
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
        } catch (final Exception e) {
            // a failed write here must not break registration - the user just stays "not tracked"
            // until fitness data is generated for them some other way
            logger.error("Failed to generate fitness data for " + username, e);
        }
    }
}
