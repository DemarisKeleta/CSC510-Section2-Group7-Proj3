package FoodSeer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
// needed for the new POST /me/mealLog tests below
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import FoodSeer.config.Roles;
import FoodSeer.dto.RegisterRequestDto;
import FoodSeer.dto.UpdateRoleDto;
import FoodSeer.dto.UserPreferencesDto;
import FoodSeer.entity.User;
import FoodSeer.repositories.UserRepository;
import FoodSeer.service.AuthService;
import FoodSeer.service.UserService;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        authService.register(new RegisterRequestDto("testuser", "test@example.com", "password123", "customer"));
        authService.register(new RegisterRequestDto("admin", "admin@example.com", "adminpass", "customer"));
        userService.updateUserRole(userService.getByUsername("admin").getId(), "ROLE_ADMIN");

        // Reload persisted users with their database-generated IDs
        testUser = userService.getByUsername("testuser");
        adminUser = userService.getByUsername("admin");
    }

    @Test
    @WithMockUser(roles = "STANDARD")
    void shouldNotAllowNonAdminToListUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetUserById() throws Exception {
        mockMvc.perform(get("/api/users/" + testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUser.getUsername()));

        mockMvc.perform(get("/api/users/" + adminUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(adminUser.getUsername()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404WhenUserNotFound() throws Exception {
        mockMvc.perform(get("/api/users/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateUserRole() throws Exception {
        mockMvc.perform(put("/api/users/" + testUser.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRoleDto(Roles.ROLE_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(Roles.ROLE_ADMIN));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_CUSTOMER")
    void shouldNotAllowNonAdminToUpdateRole() throws Exception {
        mockMvc.perform(put("/api/users/" + testUser.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRoleDto(Roles.ROLE_ADMIN))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteUserWhenAdmin() throws Exception {
        mockMvc.perform(delete("/api/users/" + testUser.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/" + testUser.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "STANDARD")
    void shouldNotAllowNonAdminToDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/" + testUser.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldUpdateUserPreferences() throws Exception {
        UserPreferencesDto prefs = new UserPreferencesDto("LOW", "VEGAN", null);

        mockMvc.perform(put("/api/users/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prefs)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.costPreference").value("LOW"))
                .andExpect(jsonPath("$.dietaryRestrictions").value("VEGAN"));
    }

    @Test
    void shouldRejectUnauthenticatedUpdatePreferences() throws Exception {
        UserPreferencesDto prefs = new UserPreferencesDto("LOW", "VEGAN", null);

        mockMvc.perform(put("/api/users/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prefs)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnCurrentUser() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void shouldRejectUnauthenticatedGetCurrentUser() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "ghostuser")
    void shouldReturn404WhenUpdatingPreferencesForMissingUser() throws Exception {
        UserPreferencesDto prefs = new UserPreferencesDto("LOW", "VEGAN", null);

        mockMvc.perform(put("/api/users/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prefs)))
                .andExpect(status().isNotFound());
    }

    /**
     * A freshly registered user has no fitness file and no saved goal, so
     * GET /me/calorieBudget should report both as absent rather than
     * fabricating a number.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnCalorieBudget_defaultsToNotTrackedAndNoGoal() throws Exception {
        // testuser has no fitness-data file and no saved goal, so the budget should report both as absent
        mockMvc.perform(get("/api/users/me/calorieBudget")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fitnessDataTracked").value(false))
                .andExpect(jsonPath("$.goalSet").value(false))
                .andExpect(jsonPath("$.remainingCalories").doesNotExist())
                .andExpect(jsonPath("$.caloriesConsumedToday").value(0));
    }

    /** An unauthenticated request to the calorie-budget endpoint must be rejected with 401. */
    @Test
    void shouldRejectUnauthenticatedGetCalorieBudget() throws Exception {
        mockMvc.perform(get("/api/users/me/calorieBudget"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Logging a valid meal should succeed and the returned, recomputed
     * budget should immediately reflect it in both caloriesConsumedToday
     * and the per-meal breakdown.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldLogMeal_andReflectItInTheBudget() throws Exception {
        mockMvc.perform(post("/api/users/me/mealLog")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealType\":\"BREAKFAST\",\"calories\":350}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caloriesConsumedToday").value(350))
                .andExpect(jsonPath("$.loggedMealsToday[0].mealType").value("BREAKFAST"))
                .andExpect(jsonPath("$.loggedMealsToday[0].calories").value(350));
    }

    /** An unrecognized meal type (e.g. "BRUNCH") must be rejected with 400, not silently accepted. */
    @Test
    @WithMockUser(username = "testuser")
    void shouldRejectLogMealWithInvalidMealType() throws Exception {
        mockMvc.perform(post("/api/users/me/mealLog")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealType\":\"BRUNCH\",\"calories\":300}"))
                .andExpect(status().isBadRequest());
    }

    /** A request missing the required calories field must be rejected with 400. */
    @Test
    @WithMockUser(username = "testuser")
    void shouldRejectLogMealWithMissingCalories() throws Exception {
        mockMvc.perform(post("/api/users/me/mealLog")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealType\":\"LUNCH\"}"))
                .andExpect(status().isBadRequest());
    }

    /** A negative calorie count must be rejected with 400. */
    @Test
    @WithMockUser(username = "testuser")
    void shouldRejectLogMealWithNegativeCalories() throws Exception {
        mockMvc.perform(post("/api/users/me/mealLog")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealType\":\"DINNER\",\"calories\":-50}"))
                .andExpect(status().isBadRequest());
    }

    /** An unauthenticated request to log a meal must be rejected with 401. */
    @Test
    void shouldRejectUnauthenticatedLogMeal() throws Exception {
        mockMvc.perform(post("/api/users/me/mealLog")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealType\":\"LUNCH\",\"calories\":400}"))
                .andExpect(status().isUnauthorized());
    }

}
