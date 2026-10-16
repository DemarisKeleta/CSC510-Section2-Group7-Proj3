package FoodSeer.service.impl;

// used to find which calendar day an order's timestamp falls on
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
// list types used throughout this class
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// wires in the repository/service beans this class depends on
import org.springframework.beans.factory.annotation.Autowired;
// marks this class as a Spring-managed service bean
import org.springframework.stereotype.Service;

// MealType.isValid validates the mealType argument to logMeal
import FoodSeer.constant.MealType;
import FoodSeer.dto.CalorieBudgetDto;
import FoodSeer.dto.FitnessData;
import FoodSeer.dto.FoodDto;
import FoodSeer.dto.FoodRecommendationDto;
import FoodSeer.dto.MealLogEntryDto;
import FoodSeer.entity.Food;
import FoodSeer.entity.MealLog;
import FoodSeer.entity.Order;
import FoodSeer.entity.User;
// converts Food entities to FoodDto for the response
import FoodSeer.mapper.FoodMapper;
import FoodSeer.repositories.FoodRepository;
import FoodSeer.repositories.MealLogRepository;
import FoodSeer.repositories.OrderRepository;
import FoodSeer.service.CalorieBudgetService;
import FoodSeer.service.FitnessDataService;

// registers this class with Spring so it can be @Autowired into controllers
@Service
public class CalorieBudgetServiceImpl implements CalorieBudgetService {

    // supplies today's burned-calories figure from the generated fitness file
    @Autowired
    private FitnessDataService fitnessDataService;

    // used to list every food, for the recommendation pass
    @Autowired
    private FoodRepository foodRepository;

    // used to find a user's orders, to sum calories consumed via FoodSeer orders
    @Autowired
    private OrderRepository orderRepository;

    // used to read/write a user's manually logged meals
    @Autowired
    private MealLogRepository mealLogRepository;

    // implements the "compute today's budget" half of the interface
    @Override
    public CalorieBudgetDto getBudgetFor(final User user) {
        // defensive null check: no user means nothing is tracked or set
        if (user == null) {
            // every field defaults to "not tracked / not set" rather than guessing
            return new CalorieBudgetDto(null, false, null, false, 0, List.of(), null);
        }

        // ask FitnessDataService for today's parsed fitness file, if any
        final Optional<FitnessData> fitness = fitnessDataService.getTodaysFitnessData(user.getUsername());
        // "tracked" only when a file was found AND it actually had a caloriesBurned value
        final boolean fitnessDataTracked = fitness.isPresent() && fitness.get().caloriesBurned() != null;
        // pull the burned value out only when tracked, otherwise leave it null
        final Integer caloriesBurnedToday = fitnessDataTracked ? fitness.get().caloriesBurned() : null;

        // the user's saved goal, straight off the entity
        final Integer dailyCalorieGoal = user.getDailyCalorieGoal();
        // "set" simply means the goal isn't null
        final boolean goalSet = dailyCalorieGoal != null;

        // sum of calories from every food in every order placed today
        final int caloriesFromOrders = sumCaloriesConsumedToday(user);
        // every meal-log row for this user, for today's date
        final List<MealLog> loggedMeals = mealLogRepository.findByUserAndLogDate(user, LocalDate.now(ZoneId.systemDefault()));
        // add up the calories across all of today's logged meals
        final int caloriesFromLoggedMeals = loggedMeals.stream().mapToInt(MealLog::getCalories).sum();
        // total consumed = orders + manually logged meals
        final int caloriesConsumedToday = caloriesFromOrders + caloriesFromLoggedMeals;

        // convert the entity list into the small DTO shape exposed to the frontend
        final List<MealLogEntryDto> loggedMealsToday = loggedMeals.stream()
                .map(m -> new MealLogEntryDto(m.getMealType(), m.getCalories()))
                .toList();

        // Activity increases the budget, orders and logged meals placed
        // today reduce it - same model real fitness-tracking apps use.
        // Whichever meal is logged first naturally gets the full budget;
        // each one logged after that is reduced by everything before it.
        // only compute a number when both inputs (goal and burned) are actually known
        final Integer remainingCalories = (goalSet && fitnessDataTracked)
                ? dailyCalorieGoal + caloriesBurnedToday - caloriesConsumedToday
                : null;

        // assemble and return the full budget response
        return new CalorieBudgetDto(caloriesBurnedToday, fitnessDataTracked, dailyCalorieGoal, goalSet, caloriesConsumedToday, loggedMealsToday, remainingCalories);
    }

    // implements the "log one meal" half of the interface
    @Override
    public CalorieBudgetDto logMeal(final User user, final String mealType, final int calories) {
        // can't log a meal with no authenticated user
        if (user == null) {
            throw new IllegalArgumentException("No authenticated user");
        }
        // reject anything other than BREAKFAST/LUNCH/DINNER (case-insensitive)
        if (!MealType.isValid(mealType)) {
            throw new IllegalArgumentException("Unrecognized meal type: " + mealType);
        }
        // calories eaten can't be negative
        if (calories < 0) {
            throw new IllegalArgumentException("Calories cannot be negative");
        }

        // store the meal type in a consistent case regardless of how the client sent it
        final String normalizedMealType = mealType.toUpperCase();
        // always log against today's date in the server's local time zone
        final LocalDate today = LocalDate.now(ZoneId.systemDefault());

        // look for an existing row for this user/date/mealType; if found, update it instead of creating a duplicate
        final MealLog entry = mealLogRepository.findByUserAndLogDateAndMealType(user, today, normalizedMealType)
                .orElseGet(MealLog::new);
        // set/overwrite every field on the (possibly new) entry
        entry.setUser(user);
        entry.setLogDate(today);
        entry.setMealType(normalizedMealType);
        entry.setCalories(calories);
        // persist the upserted row
        mealLogRepository.save(entry);

        // return the freshly recomputed budget, reflecting this new entry
        return getBudgetFor(user);
    }

    // implements the "recommend foods within budget" half of the interface
    @Override
    public FoodRecommendationDto getRecommendedFoodsFor(final User user) {
        // reuse the same budget computation used for the standalone budget endpoint
        final CalorieBudgetDto budget = getBudgetFor(user);
        // the full catalog, to filter down from
        final List<Food> allFoods = foodRepository.findAll();

        // if we don't have a real number to filter by, don't guess - show everything instead
        if (budget.remainingCalories() == null) {
            // Budget could not be computed - show the full, unfiltered menu
            // rather than guessing at a goal or assuming zero burned.
            // map every food straight to its DTO, unfiltered
            final List<FoodDto> foods = allFoods.stream().map(FoodMapper::mapToFoodDto).toList();
            // budgetApplied=false signals to the frontend that this is the full menu, not a filtered one
            return new FoodRecommendationDto(foods, budget, false, 0);
        }

        // accumulator for foods that fit within the remaining budget
        final List<FoodDto> withinBudget = new ArrayList<>();
        // running count of foods skipped because they have no calorie value at all
        int excludedForMissingCalories = 0;
        // walk every food in the catalog once
        for (final Food food : allFoods) {
            // a food with no recorded calories can't be judged, so it's excluded (not assumed to be 0)
            if (food.getCalories() == null) {
                // tally the exclusion so the frontend can explain why the list looks smaller
                excludedForMissingCalories++;
                // skip to the next food
                continue;
            }
            // include this food only if it fits within what's left of the budget
            if (food.getCalories() <= budget.remainingCalories()) {
                // map to DTO and add to the result list
                withinBudget.add(FoodMapper.mapToFoodDto(food));
            }
        }

        // budgetApplied=true here since filtering actually happened
        return new FoodRecommendationDto(withinBudget, budget, true, excludedForMissingCalories);
    }

    /**
     * Sums calorie values across every food in every order the user placed
     * today (local date). Foods with no recorded calorie value do not
     * contribute - they are not assumed to be zero.
     */
    // helper used by getBudgetFor to compute the "from orders" half of caloriesConsumedToday
    private int sumCaloriesConsumedToday(final User user) {
        // today's date in the server's local time zone, used to filter orders
        final LocalDate today = LocalDate.now(ZoneId.systemDefault());
        // every order this user has ever placed
        final List<Order> orders = orderRepository.findByUser(user);

        // running total of calories across qualifying orders
        int total = 0;
        // check each order to see if it was placed today
        for (final Order order : orders) {
            // when this order was created
            final LocalDateTime createdAt = order.getCreatedAt();
            // orders created before this field existed have no timestamp - skip them rather than guessing
            if (createdAt == null) {
                continue;
            }
            // createdAt is already in the server's local time, so take its calendar date
            final LocalDate orderDate = createdAt.toLocalDate();
            // only orders placed today count toward today's consumption
            if (!orderDate.equals(today)) {
                continue;
            }
            // add up calories for every food in this qualifying order
            for (final Food food : order.getFoods()) {
                // only add a food's calories if it actually has a recorded value
                if (food.getCalories() != null) {
                    total += food.getCalories();
                }
            }
        }
        // final summed total across all of today's orders
        return total;
    }
}
