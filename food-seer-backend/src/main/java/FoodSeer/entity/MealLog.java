package FoodSeer.entity;

// LocalDate: the calendar day this meal entry belongs to
import java.time.LocalDate;

// JPA annotations for mapping this class to the meal_logs table
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
// Lombok generates getters/setters/equals/hashCode/toString
import lombok.Data;
// Lombok generates a no-arg constructor, required by JPA
import lombok.NoArgsConstructor;

/**
 * A customer's self-reported calorie total for one meal slot on one day.
 * One row per (user, date, mealType) - logging the same meal again on the
 * same day updates the existing row rather than creating a duplicate.
 */
// maps this entity to the meal_logs table, with a composite uniqueness rule enforced at the DB level
@Entity
@Table(name = "meal_logs", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "logDate", "mealType" }))
@Data
@NoArgsConstructor
public class MealLog {

    // auto-generated primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // the customer this meal entry belongs to
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // the calendar day this entry is for (always "today" when written, per the service layer)
    private LocalDate logDate;

    /** One of BREAKFAST, LUNCH, DINNER - see FoodSeer.constant.MealType. */
    // stored as a plain string, validated against MealType before being saved
    private String mealType;

    // calories the customer reported eating for this meal
    private int calories;
}
