package FoodSeer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

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
import FoodSeer.dto.InventoryDto;
import FoodSeer.entity.Food;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * Inventory Controller Test for FoodSeer.
 * Ensures the Inventory endpoints work correctly with Food entities.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class InventoryControllerTest {

    /** Mock MVC for testing controller */
    @Autowired
    private MockMvc mvc;

    /** Reference to EntityManager */
    @Autowired
    private EntityManager entityManager;

    /**
     * Sets up the test case. We assume only one inventory row.
     * Because inventory is treated as a singleton (only one row),
     * we must truncate for auto-increment on the ID to work correctly.
     *
     * @throws Exception if error
     */
    @BeforeEach
    public void setUp() throws Exception {
        // inventory_foods rows must go first - they hold a foreign key onto inventory.id,
        // so deleting inventory directly while they still exist violates that constraint
        entityManager.createNativeQuery("DELETE FROM inventory_foods").executeUpdate();
        final Query query = entityManager.createNativeQuery("DELETE FROM inventory");
        query.executeUpdate();
    }

    /**
     * Tests the GET /api/inventory endpoint.
     *
     * @throws Exception if issue when running the test
     */
    @Test
    @Transactional
    @WithMockUser(username = "staff", roles = "STAFF")
    public void testGetInventory() throws Exception {
        final List<Food> foods = new ArrayList<>();
        final InventoryDto expectedInventory = new InventoryDto(1L, foods);

        mvc.perform(get("/api/inventory"))
                .andExpect(content().string(TestUtils.asJsonString(expectedInventory)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/inventory")).andExpect(status().isOk());
    }

    /**
     * Tests the POST /api/inventory endpoint for updating inventory.
     *
     * @throws Exception if issue when running the test
     */
    @Test
    @Transactional
    @WithMockUser(username = "staff", roles = "STAFF")
    public void testUpdateInventory() throws Exception {
        final List<Food> foods = new ArrayList<>();
        final InventoryDto expectedInventory = new InventoryDto(1L, foods);

        mvc.perform(get("/api/inventory"))
                .andExpect(content().string(TestUtils.asJsonString(expectedInventory)))
                .andExpect(status().isOk());

        final List<Food> newFoods = new ArrayList<>();

        final Food pizza = new Food("pizza", 5, 10, new ArrayList<>());
        final Food pasta = new Food("pasta", 8, 12, new ArrayList<>());
        final Food salad = new Food("salad", 15, 6, new ArrayList<>());

        newFoods.add(pizza);
        newFoods.add(pasta);
        newFoods.add(salad);

        final InventoryDto updatedInventory = new InventoryDto(1L, newFoods);

        mvc.perform(post("/api/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(updatedInventory))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
