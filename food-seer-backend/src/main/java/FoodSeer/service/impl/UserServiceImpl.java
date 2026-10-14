package FoodSeer.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import FoodSeer.entity.Order;
import FoodSeer.entity.User;
// used to clean up a user's meal logs before deleting them, in deleteUser below
import FoodSeer.repositories.MealLogRepository;
import FoodSeer.repositories.OrderRepository;
import FoodSeer.repositories.UserRepository;
import FoodSeer.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OrderRepository orderRepository;

    // used by deleteUser below to avoid an FK-constraint failure on delete
    @Autowired
    private MealLogRepository mealLogRepository;

    @Override
    public User getCurrentUser () {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if ( auth == null || !auth.isAuthenticated() ) {
            return null;
        }

        final String username = auth.getName();
        final Optional<User> u = userRepository.findByUsername( username );
        return u.orElse( null );
    }

    @Override
    public java.util.List<User> listUsers() {
        return userRepository.findAll();
    }

    @Override
    public User findById(final Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public User updateUserRole(final Long id, final String role) {
        final Optional<User> u = userRepository.findById(id);
        if (u.isEmpty()) return null;
        final User user = u.get();
        user.setRole(role);
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(final Long id) {
        // First, find the user
        final Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return; // User doesn't exist, nothing to delete
        }
        
        final User user = userOpt.get();

        // Delete all orders associated with this user
        final List<Order> userOrders = orderRepository.findByUser(user);
        orderRepository.deleteAll(userOrders);

        // Delete all meal logs associated with this user (same FK-constraint
        // reasoning as orders above - deleting the user first would fail).
        // look up then delete, same two-step pattern used for orders above
        mealLogRepository.deleteAll(mealLogRepository.findByUser(user));

        // Now delete the user
        userRepository.deleteById(id);
    }

    @Override
    public User getByUsername(String username) {
        final Optional<User> u = userRepository.findByUsername(username);
        return u.orElse(null);
    }

    @Override
    // signature extended with a 4th parameter, dailyCalorieGoal
    public User updateUserPreferences(String username, String costPreference, String dietaryRestrictions, Integer dailyCalorieGoal) {
        final Optional<User> u = userRepository.findByUsername(username);
        if (u.isEmpty()) return null;
        final User user = u.get();
        user.setCostPreference(costPreference);
        user.setDietaryRestrictions(dietaryRestrictions);
        // Partial update: a request that omits dailyCalorieGoal (null) must not
        // wipe out a previously-set goal, since the existing frontend preferences
        // form does not send this field at all.
        // only overwrite the goal when a real value was sent, unlike cost/dietary above
        if (dailyCalorieGoal != null) {
            user.setDailyCalorieGoal(dailyCalorieGoal);
        }
        return userRepository.save(user);
    }
}
