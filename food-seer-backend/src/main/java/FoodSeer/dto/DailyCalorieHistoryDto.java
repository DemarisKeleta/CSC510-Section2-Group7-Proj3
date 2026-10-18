package FoodSeer.dto;

// the calendar day this entry summarizes
import java.time.LocalDate;

/**
 * One day's total calories consumed, for the calorie-history page.
 * Sums both FoodSeer orders and manually logged meals for that date.
 */
// record holding one row of the per-day consumption history: the date and that day's total
public record DailyCalorieHistoryDto(LocalDate date, int caloriesConsumed) {
}
