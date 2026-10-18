package FoodSeer.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// response shape for the new getCalorieBudget endpoint below
import FoodSeer.dto.CalorieBudgetDto;
// response shape for the new getCalorieHistory endpoint below
import FoodSeer.dto.DailyCalorieHistoryDto;
// request shape for the new logMeal endpoint below
import FoodSeer.dto.LogMealRequestDto;
import FoodSeer.dto.UpdateRoleDto;
import FoodSeer.dto.UserDto;
import FoodSeer.dto.UserPreferencesDto;
// supplies budget computation and meal logging for the two new endpoints below
import FoodSeer.service.CalorieBudgetService;
import FoodSeer.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@CrossOrigin("*")
@RequestMapping("/api/users")
@RestController
public class UserController {
    @Autowired
    private UserService userService;

    // powers the two new endpoints below: getCalorieBudget and logMeal
    @Autowired
    private CalorieBudgetService calorieBudgetService;

    // Admin-only: list all users
    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> listUsers() {
        return userService.listUsers().stream().map(UserDto::fromEntity).collect(Collectors.toList());
    }

    // Admin-only: get user by id
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        final var u = userService.findById(id);
        if (u == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(UserDto.fromEntity(u));
    }

    // Admin-only: update a user's role
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateRole(@PathVariable Long id, @RequestBody UpdateRoleDto req) {
        final var updated = userService.updateUserRole(id, req.role());
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(UserDto.fromEntity(updated));
    }

    // Admin-only: delete user
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping ( "/me" )
    public ResponseEntity<UserDto> getCurrentUser () {
        return ResponseEntity.ok( UserDto.fromEntity(userService.getCurrentUser()) );
    }

    @PutMapping ( "/me/preferences" )
    public ResponseEntity<UserDto> updatePreferences (@RequestBody final UserPreferencesDto preferences) {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        final String username = auth.getName();
        final var updatedUser = userService.updateUserPreferences(
            username,
            preferences.costPreference(),
            preferences.dietaryRestrictions(),
            // pass the new calorie-goal field through to the service layer
            preferences.dailyCalorieGoal()
        );
        
        if (updatedUser == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(UserDto.fromEntity(updatedUser));
    }

    // new endpoint: GET /api/users/me/calorieBudget
    @GetMapping ( "/me/calorieBudget" )
    public ResponseEntity<CalorieBudgetDto> getCalorieBudget () {
        // resolve who's calling
        final var currentUser = userService.getCurrentUser();
        // no authenticated user means this request can't be fulfilled
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        // delegate the computation to CalorieBudgetService
        return ResponseEntity.ok(calorieBudgetService.getBudgetFor(currentUser));
    }

    // new endpoint: POST /api/users/me/mealLog
    @PostMapping ( "/me/mealLog" )
    public ResponseEntity<?> logMeal (@RequestBody final LogMealRequestDto req) {
        // resolve who's calling
        final var currentUser = userService.getCurrentUser();
        // no authenticated user means this request can't be fulfilled
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        // calories is required even though mealType validation happens deeper in the service
        if (req.calories() == null) {
            return ResponseEntity.badRequest().body("calories is required");
        }
        // delegate to the service, which validates mealType/calories and upserts the entry
        try {
            return ResponseEntity.ok(calorieBudgetService.logMeal(currentUser, req.mealType(), req.calories()));
        } catch (final IllegalArgumentException e) {
            // service-layer validation failure (bad meal type or negative calories) becomes a 400
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // new endpoint: GET /api/users/me/calorieHistory
    @GetMapping ( "/me/calorieHistory" )
    public ResponseEntity<List<DailyCalorieHistoryDto>> getCalorieHistory () {
        // resolve who's calling
        final var currentUser = userService.getCurrentUser();
        // no authenticated user means this request can't be fulfilled
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        // delegate the per-day aggregation to CalorieBudgetService
        return ResponseEntity.ok(calorieBudgetService.getConsumptionHistory(currentUser));
    }

}
