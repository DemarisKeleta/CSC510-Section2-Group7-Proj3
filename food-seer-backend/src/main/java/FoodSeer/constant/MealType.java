package FoodSeer.constant;

// Set gives O(1) membership checking for isValid()
import java.util.Set;

// holds the fixed list of recognized meal-type strings, mirroring the FoodSeer.constant.Roles pattern
public final class MealType {

    // private constructor: this class is a namespace for constants, never instantiated
    private MealType() {
        // prevent instantiation
    }

    // the three recognized meal-slot values
    public static final String BREAKFAST = "BREAKFAST";
    public static final String LUNCH = "LUNCH";
    public static final String DINNER = "DINNER";

    // precomputed set of the valid values above, used by isValid()
    private static final Set<String> VALID = Set.of(BREAKFAST, LUNCH, DINNER);

    // returns true only for a non-null value matching one of the three constants, case-insensitively
    public static boolean isValid(final String value) {
        return value != null && VALID.contains(value.toUpperCase());
    }
}
