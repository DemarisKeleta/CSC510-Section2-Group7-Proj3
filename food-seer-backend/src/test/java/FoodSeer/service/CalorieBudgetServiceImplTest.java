package FoodSeer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import FoodSeer.dto.CalorieBudgetDto;
import FoodSeer.dto.FitnessData;
import FoodSeer.dto.FoodRecommendationDto;
import FoodSeer.entity.Food;
import FoodSeer.entity.MealLog;
import FoodSeer.entity.Order;
import FoodSeer.entity.User;
import FoodSeer.repositories.FoodRepository;
import FoodSeer.repositories.MealLogRepository;
import FoodSeer.repositories.OrderRepository;
import FoodSeer.service.impl.CalorieBudgetServiceImpl;

/**
 * Unit tests for {@link CalorieBudgetServiceImpl}, the core service behind
 * the Fitness-Based Calorie Tracking use case.
 *
 * <p>All four of the service's collaborators ({@link FitnessDataService},
 * {@link FoodRepository}, {@link OrderRepository}, {@link MealLogRepository})
 * are mocked, so these tests exercise the budget-computation, meal-logging,
 * and recommendation-filtering logic in isolation, with no real database or
 * filesystem involved.
 *
 * <p>Covers three behaviors of the service:
 * <ul>
 *   <li>{@code getBudgetFor} - computing goal + burned - consumed, and all
 *       the "not tracked" / "not set" edge cases around it</li>
 *   <li>{@code logMeal} - validating and upserting a meal-log entry</li>
 *   <li>{@code getRecommendedFoodsFor} - filtering the menu by remaining
 *       budget, and falling back to the full menu when the budget can't be
 *       computed</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class CalorieBudgetServiceImplTest {

    @Mock
    private FitnessDataService fitnessDataService;

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MealLogRepository mealLogRepository;

    @InjectMocks
    private CalorieBudgetServiceImpl calorieBudgetService;

    /**
     * Builds a bare {@link User} with just a username set - enough for the
     * service under test, which only reads username, dailyCalorieGoal, and
     * identity (for equals/hashCode-based mock matching).
     *
     * @param username the username to assign
     * @return a new, unsaved User instance
     */
    private User user(final String username) {
        final User u = new User();
        u.setUsername(username);
        return u;
    }

    // --- getBudgetFor ---

    /**
     * A null user (e.g. an unauthenticated caller slipping through) must not
     * throw - every field in the returned budget should report the "nothing
     * known" state rather than guessing at defaults.
     */
    @Test
    void getBudgetFor_nullUser_returnsAllDefaults() {
        final CalorieBudgetDto budget = calorieBudgetService.getBudgetFor(null);

        assertNull(budget.caloriesBurnedToday());
        assertFalse(budget.fitnessDataTracked());
        assertNull(budget.dailyCalorieGoal());
        assertFalse(budget.goalSet());
        assertEquals(0, budget.caloriesConsumedToday());
        assertTrue(budget.loggedMealsToday().isEmpty());
        assertNull(budget.remainingCalories());
    }

    /**
     * A user with no fitness file and no saved goal should get
     * fitnessDataTracked=false, goalSet=false, and a null remainingCalories -
     * never a guessed or zero-defaulted number.
     */
    @Test
    void getBudgetFor_noFitnessFile_noGoal_remainingIsNull() {
        final User u = user("nodata_user");
        when(fitnessDataService.getTodaysFitnessData("nodata_user")).thenReturn(Optional.empty());
        when(orderRepository.findByUser(u)).thenReturn(List.of());
        when(mealLogRepository.findByUserAndLogDate(eq(u), any(LocalDate.class))).thenReturn(List.of());

        final CalorieBudgetDto budget = calorieBudgetService.getBudgetFor(u);

        assertFalse(budget.fitnessDataTracked());
        assertNull(budget.caloriesBurnedToday());
        assertFalse(budget.goalSet());
        assertNull(budget.remainingCalories());
    }

    /**
     * The core happy-path formula: with fitness data tracked and a goal set,
     * remaining calories = goal + burned - consumed (zero consumed here).
     */
    @Test
    void getBudgetFor_fitnessTracked_andGoalSet_computesRemaining() {
        final User u = user("tracked_user");
        u.setDailyCalorieGoal(1800);
        when(fitnessDataService.getTodaysFitnessData("tracked_user"))
                .thenReturn(Optional.of(new FitnessData("tracked_user", "2026-10-14", 300, 5000, 40)));
        when(orderRepository.findByUser(u)).thenReturn(List.of());
        when(mealLogRepository.findByUserAndLogDate(eq(u), any(LocalDate.class))).thenReturn(List.of());

        final CalorieBudgetDto budget = calorieBudgetService.getBudgetFor(u);

        assertTrue(budget.fitnessDataTracked());
        assertEquals(300, budget.caloriesBurnedToday());
        assertTrue(budget.goalSet());
        assertEquals(0, budget.caloriesConsumedToday());
        // 1800 goal + 300 burned - 0 consumed
        assertEquals(2100, budget.remainingCalories());
    }

    /**
     * Consumed calories must combine both sources: calories from today's
     * FoodSeer orders, and calories from manually logged meals, summed
     * together into one caloriesConsumedToday total.
     */
    @Test
    void getBudgetFor_subtractsTodaysOrders_andLoggedMeals() {
        final User u = user("full_user");
        u.setDailyCalorieGoal(2000);
        when(fitnessDataService.getTodaysFitnessData("full_user"))
                .thenReturn(Optional.of(new FitnessData("full_user", "2026-10-14", 200, 4000, 30)));

        final Food food = new Food("BURGER", 5, 10, List.of());
        food.setCalories(600);
        final Order todaysOrder = new Order(1L, "lunch order");
        todaysOrder.setFoods(List.of(food));
        todaysOrder.setCreatedAt(Instant.now());
        when(orderRepository.findByUser(u)).thenReturn(List.of(todaysOrder));

        final MealLog loggedBreakfast = new MealLog();
        loggedBreakfast.setMealType("BREAKFAST");
        loggedBreakfast.setCalories(400);
        when(mealLogRepository.findByUserAndLogDate(eq(u), any(LocalDate.class))).thenReturn(List.of(loggedBreakfast));

        final CalorieBudgetDto budget = calorieBudgetService.getBudgetFor(u);

        // 600 from the order + 400 from the logged meal
        assertEquals(1000, budget.caloriesConsumedToday());
        assertEquals(1, budget.loggedMealsToday().size());
        assertEquals("BREAKFAST", budget.loggedMealsToday().get(0).mealType());
        // 2000 goal + 200 burned - 1000 consumed
        assertEquals(1200, budget.remainingCalories());
    }

    /**
     * Only orders placed today should count toward today's consumption - an
     * order from yesterday, even with real calorie data, must be excluded.
     */
    @Test
    void getBudgetFor_ignoresOrdersFromOtherDays() {
        final User u = user("yesterday_user");
        u.setDailyCalorieGoal(2000);
        when(fitnessDataService.getTodaysFitnessData("yesterday_user"))
                .thenReturn(Optional.of(new FitnessData("yesterday_user", "2026-10-14", 0, 0, 0)));

        final Food food = new Food("PIZZA", 5, 10, List.of());
        food.setCalories(800);
        final Order yesterdaysOrder = new Order(2L, "yesterday order");
        yesterdaysOrder.setFoods(List.of(food));
        yesterdaysOrder.setCreatedAt(Instant.now().minus(java.time.Duration.ofDays(1)));
        when(orderRepository.findByUser(u)).thenReturn(List.of(yesterdaysOrder));
        when(mealLogRepository.findByUserAndLogDate(eq(u), any(LocalDate.class))).thenReturn(List.of());

        final CalorieBudgetDto budget = calorieBudgetService.getBudgetFor(u);

        // yesterday's order must not count toward today's consumption
        assertEquals(0, budget.caloriesConsumedToday());
    }

    /**
     * A food with no recorded calorie value must contribute nothing to the
     * consumed total - it is excluded from the sum, not silently treated as
     * zero calories (which would be indistinguishable from "verified free").
     */
    @Test
    void getBudgetFor_foodsWithNoCalories_areNotCountedAsZero() {
        final User u = user("missing_calorie_user");
        u.setDailyCalorieGoal(2000);
        when(fitnessDataService.getTodaysFitnessData("missing_calorie_user"))
                .thenReturn(Optional.of(new FitnessData("missing_calorie_user", "2026-10-14", 100, 0, 0)));

        // a food with no calories recorded at all
        final Food untaggedFood = new Food("MYSTERY", 1, 5, List.of());
        final Order order = new Order(3L, "order");
        order.setFoods(List.of(untaggedFood));
        order.setCreatedAt(Instant.now());
        when(orderRepository.findByUser(u)).thenReturn(List.of(order));
        when(mealLogRepository.findByUserAndLogDate(eq(u), any(LocalDate.class))).thenReturn(List.of());

        final CalorieBudgetDto budget = calorieBudgetService.getBudgetFor(u);

        // the untagged food contributes nothing, not zero-by-coincidence but by design
        assertEquals(0, budget.caloriesConsumedToday());
    }

    // --- logMeal ---

    /** Logging a meal with no authenticated user must fail loudly, not silently no-op. */
    @Test
    void logMeal_nullUser_throws() {
        assertThrows(IllegalArgumentException.class, () -> calorieBudgetService.logMeal(null, "BREAKFAST", 300));
    }

    /**
     * Only BREAKFAST/LUNCH/DINNER are recognized meal types; anything else
     * must be rejected, and the error message should name the bad value to
     * help the caller debug it.
     */
    @Test
    void logMeal_invalidMealType_throws() {
        final User u = user("someone");
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> calorieBudgetService.logMeal(u, "BRUNCH", 300));
        assertTrue(ex.getMessage().contains("BRUNCH"));
    }

    /** A negative calorie count makes no physical sense and must be rejected. */
    @Test
    void logMeal_negativeCalories_throws() {
        final User u = user("someone");
        assertThrows(IllegalArgumentException.class, () -> calorieBudgetService.logMeal(u, "LUNCH", -1));
    }

    /**
     * Logging a meal for the first time today should create and save a new
     * {@link MealLog} row, with the meal type normalized to uppercase
     * regardless of how the caller cased it ("dinner" -> "DINNER").
     */
    @Test
    void logMeal_newEntry_isSavedWithNormalizedMealType() {
        final User u = user("logger");
        when(mealLogRepository.findByUserAndLogDateAndMealType(eq(u), any(LocalDate.class), eq("DINNER")))
                .thenReturn(Optional.empty());
        when(fitnessDataService.getTodaysFitnessData("logger")).thenReturn(Optional.empty());
        when(orderRepository.findByUser(u)).thenReturn(List.of());
        when(mealLogRepository.findByUserAndLogDate(eq(u), any(LocalDate.class))).thenReturn(List.of());

        calorieBudgetService.logMeal(u, "dinner", 700);

        final ArgumentCaptor<MealLog> captor = ArgumentCaptor.forClass(MealLog.class);
        verify(mealLogRepository).save(captor.capture());
        final MealLog saved = captor.getValue();
        assertEquals("DINNER", saved.getMealType());
        assertEquals(700, saved.getCalories());
        assertEquals(u, saved.getUser());
        assertEquals(LocalDate.now(ZoneId.systemDefault()), saved.getLogDate());
    }

    /**
     * Re-logging the same meal slot on the same day (e.g. correcting an
     * earlier entry) must update the existing row in place - verified here
     * by checking the saved entity keeps the original row's id.
     */
    @Test
    void logMeal_existingEntry_isUpdatedNotDuplicated() {
        final User u = user("relogger");
        final MealLog existing = new MealLog();
        existing.setId(99L);
        existing.setUser(u);
        existing.setMealType("LUNCH");
        existing.setCalories(500);
        when(mealLogRepository.findByUserAndLogDateAndMealType(eq(u), any(LocalDate.class), eq("LUNCH")))
                .thenReturn(Optional.of(existing));
        when(fitnessDataService.getTodaysFitnessData("relogger")).thenReturn(Optional.empty());
        when(orderRepository.findByUser(u)).thenReturn(List.of());
        when(mealLogRepository.findByUserAndLogDate(eq(u), any(LocalDate.class))).thenReturn(List.of());

        calorieBudgetService.logMeal(u, "LUNCH", 650);

        final ArgumentCaptor<MealLog> captor = ArgumentCaptor.forClass(MealLog.class);
        verify(mealLogRepository).save(captor.capture());
        // the same row (same id) was updated, not a new one created
        assertEquals(99L, captor.getValue().getId());
        assertEquals(650, captor.getValue().getCalories());
    }

    // --- getRecommendedFoodsFor ---

    /**
     * When the budget can't be computed (no goal, or fitness not tracked),
     * the recommendation must fall back to the full, unfiltered menu rather
     * than guessing or returning an empty list.
     */
    @Test
    void getRecommendedFoodsFor_budgetNotComputable_returnsFullMenu() {
        final User u = user("no_budget_user");
        when(fitnessDataService.getTodaysFitnessData("no_budget_user")).thenReturn(Optional.empty());
        when(orderRepository.findByUser(u)).thenReturn(List.of());
        when(mealLogRepository.findByUserAndLogDate(eq(u), any(LocalDate.class))).thenReturn(List.of());

        final Food a = new Food("A", 1, 1, List.of());
        a.setCalories(100);
        final Food b = new Food("B", 1, 1, List.of());
        when(foodRepository.findAll()).thenReturn(List.of(a, b));

        final FoodRecommendationDto result = calorieBudgetService.getRecommendedFoodsFor(u);

        assertFalse(result.budgetApplied());
        assertEquals(2, result.foods().size());
        assertEquals(0, result.excludedForMissingCalories());
    }

    /**
     * With a real, computable budget, the recommendation must include only
     * foods at or under the remaining calories, exclude foods over budget,
     * and separately count (not silently drop) foods with no calorie value.
     */
    @Test
    void getRecommendedFoodsFor_filtersAndCountsExclusions() {
        final User u = user("budget_user");
        u.setDailyCalorieGoal(500);
        when(fitnessDataService.getTodaysFitnessData("budget_user"))
                .thenReturn(Optional.of(new FitnessData("budget_user", "2026-10-14", 0, 0, 0)));
        when(orderRepository.findByUser(u)).thenReturn(List.of());
        when(mealLogRepository.findByUserAndLogDate(eq(u), any(LocalDate.class))).thenReturn(List.of());

        // remaining budget = 500 + 0 - 0 = 500
        final Food fits = new Food("SALAD", 1, 1, List.of());
        fits.setCalories(300);
        final Food tooMany = new Food("FEAST", 1, 1, List.of());
        tooMany.setCalories(900);
        final Food untagged = new Food("MYSTERY", 1, 1, List.of());
        when(foodRepository.findAll()).thenReturn(List.of(fits, tooMany, untagged));

        final FoodRecommendationDto result = calorieBudgetService.getRecommendedFoodsFor(u);

        assertTrue(result.budgetApplied());
        assertEquals(1, result.foods().size());
        assertEquals("SALAD", result.foods().get(0).getFoodName());
        assertEquals(1, result.excludedForMissingCalories());
    }

    /**
     * When logMeal's validation fails, nothing should be persisted - this
     * guards against a save call sneaking in before the validation checks.
     */
    @Test
    void logMeal_doesNotSaveWhenValidationFails() {
        final User u = user("invalid_logger");
        assertThrows(IllegalArgumentException.class, () -> calorieBudgetService.logMeal(u, "SECOND_BREAKFAST", 100));
        verify(mealLogRepository, never()).save(any());
    }
}
