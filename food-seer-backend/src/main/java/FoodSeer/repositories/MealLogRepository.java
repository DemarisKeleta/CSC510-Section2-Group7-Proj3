package FoodSeer.repositories;

// LocalDate is used in the date-scoped finder methods
import java.time.LocalDate;
// returned by the "all entries" finders
import java.util.List;
// returned by the "one specific entry" finder
import java.util.Optional;

// base interface providing save/findAll/deleteAll/etc. for free
import org.springframework.data.jpa.repository.JpaRepository;

import FoodSeer.entity.MealLog;
import FoodSeer.entity.User;

// Spring Data derives the SQL for each method below from its name - no implementation needed
public interface MealLogRepository extends JpaRepository<MealLog, Long> {

    // all of a user's meal-log rows regardless of date, used when deleting a user (see UserServiceImpl.deleteUser)
    List<MealLog> findByUser(User user);

    // all of a user's meal-log rows for one specific day, used to compute today's budget
    List<MealLog> findByUserAndLogDate(User user, LocalDate logDate);

    // the single row for one user/date/mealType combination, used to detect "update existing" vs "insert new" when logging a meal
    Optional<MealLog> findByUserAndLogDateAndMealType(User user, LocalDate logDate, String mealType);
}
