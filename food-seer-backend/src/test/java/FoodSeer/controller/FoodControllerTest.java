package FoodSeer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import FoodSeer.TestUtils;
import FoodSeer.dto.FoodDto;
import FoodSeer.entity.User;
import FoodSeer.repositories.FoodRepository;
import FoodSeer.repositories.InventoryRepository;
import FoodSeer.repositories.OrderRepository;
import FoodSeer.repositories.UserRepository;
import FoodSeer.service.FoodService;

/**
 * Tests FoodController
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class FoodControllerTest {

    /** Mock MVC for testing controller */
    @Autowired
    private MockMvc mvc;

    /** Reference to food repository */
    @Autowired
    private FoodRepository foodRepository;

    /** Reference to inventory repository */
    @Autowired
    private InventoryRepository inventoryRepository;

    /** Reference to order repository (ensure orders removed before foods) */
    @Autowired
    private OrderRepository orderRepository;

    /** Reference to food service */
    @Autowired
    private FoodService foodService;

    /** Reference to user repository (used only by the withinCalorieBudget tests below) */
    @Autowired
    private UserRepository userRepository;

    /**
     * Sets up the test case.
     *
     * @throws java.lang.Exception if error
     */
    @BeforeEach
    public void setUp () throws Exception {
        // delete orders first so foreign key constraints do not block food deletions
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        foodRepository.deleteAll();
    }

    @Test
    @WithMockUser ( username = "staff", roles = "STAFF" )
    void testGetFoods () throws Exception {
        mvc.perform( get( "/api/foods" ) ).andExpect( status().isOk() );
    }

    @Test
    @WithMockUser ( username = "staff", roles = "STAFF" )
    void testCreateFood () throws Exception {
        final FoodDto food1 = new FoodDto( "COFFEE", 5, 3, Arrays.asList( "MILK", "SUGAR" ) );

        mvc.perform( post( "/api/foods" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( TestUtils.asJsonString( food1 ) )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.amount" ).value( "5" ) )
                .andExpect( jsonPath( "$.price" ).value( "3" ) )
                .andExpect( jsonPath( "$.foodName" ).value( "COFFEE" ) );

        final FoodDto food2 = new FoodDto( "PUMPKIN_SPICE", 10, 7, Arrays.asList( "CINNAMON" ) );
        final FoodDto savedFood2 = foodService.createFood( food2 );

        mvc.perform(
                        get( "/api/foods/" + foodService.getFoodById( savedFood2.getId() ).getId() ) )
                .andExpect( status().isOk() );
    }

    @Test
    @WithMockUser ( username = "staff", roles = "STAFF" )
    void testUpdateFood () throws Exception {
        final FoodDto food1 = new FoodDto( "COFFEE", 5, 3, Arrays.asList( "MILK" ) );

        mvc.perform( post( "/api/foods" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( TestUtils.asJsonString( food1 ) )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.amount" ).value( "5" ) )
                .andExpect( jsonPath( "$.foodName" ).value( "COFFEE" ) );

        final FoodDto updatedFood = new FoodDto( "COFFEE", 12, 4, Arrays.asList( "MILK", "SUGAR" ) );

        mvc.perform( post( "/api/foods/updateFood" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( TestUtils.asJsonString( updatedFood ) )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() );
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void testCreateFoodDuplicateName() throws Exception {
        FoodDto food = new FoodDto("COFFEE", 5, 3, Arrays.asList("MILK"));
        foodService.createFood(food);

        mvc.perform(post("/api/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtils.asJsonString(food)))
                .andExpect(status().isConflict()); // 409
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void testCreateFoodInvalid() throws Exception {
        // Negative amount = invalid
        FoodDto invalid = new FoodDto("INVALID", -5, 3, Arrays.asList("NONE"));

        mvc.perform(post("/api/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtils.asJsonString(invalid)))
                .andExpect(status().isBadRequest()); // 400
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void testDeleteFoodSuccess() throws Exception {
        FoodDto food = new FoodDto("TEA", 5, 3, Arrays.asList("NONE"));
        FoodDto saved = foodService.createFood(food);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/foods/" + saved.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void testDeleteFoodNotFound() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/foods/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void testUpdateFoodMissingName() throws Exception {
        FoodDto invalid = new FoodDto("", 5, 3, Arrays.asList("NONE"));

        mvc.perform(post("/api/foods/updateFood")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtils.asJsonString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void testUpdateFoodNotFound() throws Exception {
        FoodDto notExist = new FoodDto("NOFOOD", 5, 3, Arrays.asList("NONE"));

        mvc.perform(post("/api/foods/updateFood")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtils.asJsonString(notExist)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void testUpdateFoodInvalidValues() throws Exception {
        FoodDto food = new FoodDto("BREAD", 5, 2, Arrays.asList("GLUTEN"));
        foodService.createFood(food);

        FoodDto update = new FoodDto("BREAD", -1, 2, Arrays.asList("GLUTEN")); // invalid amount

        mvc.perform(post("/api/foods/updateFood")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtils.asJsonString(update)))
                .andExpect(status().isBadRequest());
    }

    /**
     * A customer with no saved daily calorie goal should get the full,
     * unfiltered menu back from /withinCalorieBudget (budgetApplied=false),
     * since there is no real budget number to filter by yet.
     */
    @Test
    @WithMockUser(username = "budget_customer", roles = "CUSTOMER")
    void testWithinCalorieBudget_noGoalSet_returnsFullMenuUnfiltered() throws Exception {
        // @WithMockUser only fakes Spring Security's authentication - the endpoint also
        // looks the user up in the real database, so a matching row has to exist too
        userRepository.save(User.builder()
                .username("budget_customer")
                .email("budget_customer@test.com")
                .password("password")
                .role("ROLE_CUSTOMER")
                .build());

        final FoodDto food = new FoodDto("RICE_BOWL", 5, 8, Arrays.asList("NONE"));
        foodService.createFood(food);

        mvc.perform(get("/api/foods/withinCalorieBudget"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetApplied").value(false))
                .andExpect(jsonPath("$.foods", org.hamcrest.Matchers.hasSize(1)));
    }

    /** An unauthenticated request to the budget-aware food endpoint must be rejected with 401. */
    @Test
    void testWithinCalorieBudget_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/foods/withinCalorieBudget"))
                .andExpect(status().isUnauthorized());
    }
}
