package FoodSeer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import FoodSeer.dto.FitnessData;
import FoodSeer.service.impl.FitnessDataServiceImpl;

/**
 * Unit tests for {@link FitnessDataServiceImpl}, which reads a per-user
 * generated JSON file standing in for a real fitness-app export.
 *
 * <p>Unlike the rest of the service layer, this class is deliberately tested
 * against real file I/O (via JUnit's {@link TempDir}) rather than a mocked
 * filesystem, since the whole point of this service is file-reading
 * behavior - missing files, malformed JSON, wrong file types - which a mock
 * would not meaningfully exercise.
 *
 * <p>The service's {@code fitnessDataDir} field is normally populated by
 * Spring's {@code @Value} injection; since there is no Spring context here,
 * {@link ReflectionTestUtils} is used to point it at the temp directory
 * instead.
 */
class FitnessDataServiceImplTest {

    // a real, isolated directory on disk for each test, cleaned up automatically afterward
    @TempDir
    Path tempDir;

    private FitnessDataServiceImpl fitnessDataService;

    /**
     * Creates a fresh service instance before each test and redirects it to
     * read from the test's isolated temp directory instead of the real
     * "fitness-data" folder.
     */
    @BeforeEach
    void setUp() {
        fitnessDataService = new FitnessDataServiceImpl();
        // point the service at the temp directory instead of the real "fitness-data" folder,
        // since the @Value-injected field has no Spring context to populate it in a plain unit test
        ReflectionTestUtils.setField(fitnessDataService, "fitnessDataDir", tempDir.toString());
    }

    /** No file at all for a given username must report "not tracked", not an error. */
    @Test
    void getTodaysFitnessData_noFileForUser_returnsEmpty() {
        final Optional<FitnessData> result = fitnessDataService.getTodaysFitnessData("nobody");
        assertTrue(result.isEmpty());
    }

    /** A null username can't form a valid file path and must be handled gracefully. */
    @Test
    void getTodaysFitnessData_nullUsername_returnsEmpty() {
        assertTrue(fitnessDataService.getTodaysFitnessData(null).isEmpty());
    }

    /** A blank (whitespace-only) username is treated the same as a missing one. */
    @Test
    void getTodaysFitnessData_blankUsername_returnsEmpty() {
        assertTrue(fitnessDataService.getTodaysFitnessData("   ").isEmpty());
    }

    /**
     * The happy path: a well-formed JSON file for the given username is
     * read and parsed into a {@link FitnessData} with every field intact.
     */
    @Test
    void getTodaysFitnessData_validFile_parsesCorrectly() throws IOException {
        final String json = "{\"username\":\"alice\",\"date\":\"2026-10-14\",\"caloriesBurned\":450,\"steps\":7000,\"activeMinutes\":35}";
        Files.writeString(tempDir.resolve("alice.json"), json);

        final Optional<FitnessData> result = fitnessDataService.getTodaysFitnessData("alice");

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().username());
        assertEquals(450, result.get().caloriesBurned());
        assertEquals(7000, result.get().steps());
        assertEquals(35, result.get().activeMinutes());
    }

    /**
     * A file that exists but fails to parse (e.g. truncated/invalid JSON)
     * must degrade to "not tracked" rather than throwing and failing the
     * caller's request.
     */
    @Test
    void getTodaysFitnessData_malformedJson_returnsEmptyInsteadOfThrowing() throws IOException {
        // deliberately broken JSON - missing closing brace
        Files.writeString(tempDir.resolve("broken.json"), "{\"username\":\"broken\", \"caloriesBurned\": ");

        final Optional<FitnessData> result = fitnessDataService.getTodaysFitnessData("broken");

        assertFalse(result.isPresent());
    }

    /**
     * A directory happening to share the expected filename must not be read
     * as if it were a file - File#isFile() guards against this.
     */
    @Test
    void getTodaysFitnessData_fileIsADirectory_returnsEmpty() throws IOException {
        // a directory named like a user's file, rather than an actual file
        Files.createDirectory(tempDir.resolve("dirname.json"));

        final Optional<FitnessData> result = fitnessDataService.getTodaysFitnessData("dirname");

        assertFalse(result.isPresent());
    }

    /**
     * The presence of one user's file must not accidentally satisfy a
     * lookup for a different username - each user's data is isolated by
     * filename.
     */
    @Test
    void getTodaysFitnessData_doesNotMatchDifferentUsersFile() throws IOException {
        Files.writeString(tempDir.resolve("bob.json"),
                "{\"username\":\"bob\",\"date\":\"2026-10-14\",\"caloriesBurned\":200,\"steps\":1000,\"activeMinutes\":10}");

        // "carol" has no file, even though "bob" does - must not cross-match
        final Optional<FitnessData> result = fitnessDataService.getTodaysFitnessData("carol");

        assertTrue(result.isEmpty());
    }

    // --- generateFitnessDataIfMissing ---

    /**
     * The main path: a user with no existing file gets a freshly generated
     * one, readable straight back through getTodaysFitnessData, with
     * plausible (non-negative, in-range) values.
     */
    @Test
    void generateFitnessDataIfMissing_noExistingFile_writesPlausibleData() {
        fitnessDataService.generateFitnessDataIfMissing("brandNewUser");

        final Optional<FitnessData> result = fitnessDataService.getTodaysFitnessData("brandNewUser");

        assertTrue(result.isPresent());
        assertEquals("brandNewUser", result.get().username());
        assertTrue(result.get().caloriesBurned() >= 0);
        assertTrue(result.get().steps() >= 2000 && result.get().steps() <= 12000);
        assertTrue(result.get().activeMinutes() >= 10 && result.get().activeMinutes() <= 90);
    }

    /**
     * An existing file (hand-crafted or previously generated) must never be
     * overwritten - generation is only a fallback for users with no data.
     */
    @Test
    void generateFitnessDataIfMissing_existingFile_isNotOverwritten() throws IOException {
        final String originalJson = "{\"username\":\"dave\",\"date\":\"2020-01-01\",\"caloriesBurned\":999,\"steps\":1,\"activeMinutes\":1}";
        Files.writeString(tempDir.resolve("dave.json"), originalJson);

        fitnessDataService.generateFitnessDataIfMissing("dave");

        final Optional<FitnessData> result = fitnessDataService.getTodaysFitnessData("dave");
        assertTrue(result.isPresent());
        // the original, deliberately-unusual values must survive untouched
        assertEquals(999, result.get().caloriesBurned());
        assertEquals(1, result.get().steps());
    }

    /** A null/blank username can't form a valid file path and must be a no-op, not a crash. */
    @Test
    void generateFitnessDataIfMissing_nullOrBlankUsername_doesNotThrow() {
        fitnessDataService.generateFitnessDataIfMissing(null);
        fitnessDataService.generateFitnessDataIfMissing("   ");
        // nothing to assert beyond "no exception" - there's no valid file either call could have written
    }
}
