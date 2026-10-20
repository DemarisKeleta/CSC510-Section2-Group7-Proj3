package FoodSeer.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import FoodSeer.dto.FoodDto;
import FoodSeer.dto.InventoryDto;
import FoodSeer.exception.ResourceNotFoundException;
import FoodSeer.repositories.FoodRepository;

@SpringBootTest
@Transactional
public class FoodServiceImplTest {

    @Autowired
    private FoodService foodService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private FoodRepository foodRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager.createNativeQuery("DELETE FROM orders_foods").executeUpdate();
        // also clear Inventory's join table - DataInitializer seeds a real Inventory row
        // containing real Food rows at app startup, so without this, deleteAll() below
        // fails with a foreign-key violation on inventory_foods for any seeded food
        entityManager.createNativeQuery("DELETE FROM inventory_foods").executeUpdate();
        foodRepository.deleteAll();
    }

    // --- Helpers -----------------------------------------------------

    private void assertFood(FoodDto food, String name, int amount, int price,
                            java.util.List<String> allergies) {
        assertAll(
                () -> assertEquals(name, food.getFoodName()),
                () -> assertEquals(amount, food.getAmount()),
                () -> assertEquals(price, food.getPrice()),
                () -> assertEquals(allergies, food.getAllergies())
        );
    }

    // --- Create Tests ------------------------------------------------

    @Test
    public void testCreateValidFood() {
        FoodDto food = new FoodDto("COFFEE", 5, 3, Arrays.asList("MILK", "SUGAR"));
        FoodDto created = foodService.createFood(food);

        assertFood(created, "COFFEE", 5, 3, Arrays.asList("MILK", "SUGAR"));
    }

    @Test
    public void testCreateInvalidFoodAmount() {
        FoodDto invalidFood = new FoodDto("MATCHA", -1, 2, Arrays.asList("MILK"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                foodService.createFood(invalidFood)
        );
        assertEquals("The provided food information is invalid.", ex.getMessage());
    }

    @Test
    public void testCreateDuplicateFoodName() {
        FoodDto food1 = new FoodDto("COFFEE", 5, 3, Arrays.asList("MILK"));
        foodService.createFood(food1);

        FoodDto duplicate = new FoodDto("COFFEE", 8, 4, Arrays.asList("MILK", "SUGAR"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                foodService.createFood(duplicate)
        );
        assertEquals("The name of the new food already exists in the system.", ex.getMessage());
    }

    // --- Get Tests ---------------------------------------------------

    @Test
    public void testGetFoodById() {
        FoodDto food = new FoodDto("COFFEE", 5, 3, Arrays.asList("MILK", "SUGAR"));
        FoodDto created = foodService.createFood(food);

        FoodDto fetched = foodService.getFoodById(created.getId());
        assertFood(fetched, "COFFEE", 5, 3, Arrays.asList("MILK", "SUGAR"));
    }

    @Test
    public void testGetFoodByInvalidId() {
        Long invalidId = 9999L;

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                foodService.getFoodById(invalidId)
        );
        assertEquals("Food does not exist with id " + invalidId, ex.getMessage());
    }

    // --- Update Tests ------------------------------------------------

    @Test
    public void testUpdateFood() {
        FoodDto food = new FoodDto("COFFEE", 5, 3, Arrays.asList("MILK"));
        foodService.createFood(food);

        // added a 5th argument (calories) to match updateFood's new signature
        FoodDto updated = foodService.updateFood("COFFEE", 12, 5, Arrays.asList("Water"), 450);
        assertFood(updated, "COFFEE", 12, 5, Arrays.asList("Water"));
    }

    @Test
    public void testDeleteFoodSuccess() {
        FoodDto food = new FoodDto("LATTE", 5, 4, Arrays.asList("MILK"));
        FoodDto created = foodService.createFood(food);

        // Ensure food exists first
        assertNotNull(foodService.getFoodById(created.getId()));

        // Perform delete
        foodService.deleteFood(created.getId());

        // Now food should be gone
        assertThrows(ResourceNotFoundException.class, () ->
                foodService.getFoodById(created.getId())
        );
    }

    @Test
    public void testDeleteFoodNotFound() {
        Long badId = 9999L;
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                foodService.deleteFood(badId)
        );
        assertEquals("Food does not exist with id " + badId, ex.getMessage());
    }

    @Test
    public void testGetDuplicateNameExists() {
        FoodDto food = new FoodDto("COFFEE", 5, 3, Arrays.asList("MILK"));
        foodService.createFood(food);

        FoodDto duplicate = foodService.getDuplicateName("COFFEE");

        assertNotNull(duplicate);
        assertEquals("COFFEE", duplicate.getFoodName());
    }

    @Test
    public void testGetDuplicateNameNotFound() {
        FoodDto result = foodService.getDuplicateName("NON_EXISTENT");
        assertNull(result);
    }


    @Test
    public void testIsValidFoodTrue() {
        FoodDto food = new FoodDto("TEA", 3, 2, Arrays.asList("NONE"));
        assertTrue(foodService.isValidFood(food));
    }

    @Test
    public void testIsValidFoodFalseCases() {
        assertFalse(foodService.isValidFood(null));
        assertFalse(foodService.isValidFood(new FoodDto("", 1, 1, Arrays.asList("NONE"))));
        assertFalse(foodService.isValidFood(new FoodDto(" ", 1, 1, Arrays.asList("NONE"))));
        assertFalse(foodService.isValidFood(new FoodDto("TEA", -1, 1, Arrays.asList("NONE"))));
        assertFalse(foodService.isValidFood(new FoodDto("TEA", 1, -1, Arrays.asList("NONE"))));

        // allergies invalid
        assertFalse(foodService.isValidFood(new FoodDto("TEA", 1, 1, Arrays.asList("", "MILK"))));
    }


    @Test
    public void testUpdateFoodThrowsForNegativeAmount() {
        FoodDto food = new FoodDto("COFFEE", 5, 3, Arrays.asList("MILK"));
        foodService.createFood(food);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                // added a 5th argument (calories) to match updateFood's new signature
                foodService.updateFood("COFFEE", -5, 3, Arrays.asList("MILK"), 100)
        );
        assertEquals("The units of the food must be a positive integer.", ex.getMessage());
    }

    @Test
    public void testUpdateFoodThrowsForNegativePrice() {
        FoodDto food = new FoodDto("COFFEE", 5, 3, Arrays.asList("MILK"));
        foodService.createFood(food);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                // added a 5th argument (calories) to match updateFood's new signature
                foodService.updateFood("COFFEE", 5, -1, Arrays.asList("MILK"), 100)
        );
        assertEquals("The price of the food must be a non-negative integer.", ex.getMessage());
    }

    @Test
    @Transactional
    public void testUpdateFoodNotFound() {
        String missingName = "NON_EXISTENT_FOOD";

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                // added a 5th argument (calories) to match updateFood's new signature
                foodService.updateFood(missingName, 5, 3, Arrays.asList("NONE"), 100)
        );

        assertEquals("Food does not exist with name " + missingName, ex.getMessage());
    }

    @Test
    @Transactional
    public void testCreateFoodAddsToExistingInventory() {
        // First create a food to initialize inventory
        FoodDto food1 = new FoodDto("COFFEE", 5, 3, Arrays.asList("MILK"));
        FoodDto created1 = foodService.createFood(food1);
        assertNotNull(created1);

        // Now create a second food to hit the ELSE branch in createFood()
        FoodDto food2 = new FoodDto("TEA", 10, 4, Arrays.asList("NONE"));
        FoodDto created2 = foodService.createFood(food2);
        assertNotNull(created2);

        // Validate both foods exist
        FoodDto fetched1 = foodService.getFoodById(created1.getId());
        FoodDto fetched2 = foodService.getFoodById(created2.getId());

        assertEquals("COFFEE", fetched1.getFoodName());
        assertEquals("TEA", fetched2.getFoodName());

        // Validate inventory has BOTH foods
        InventoryDto inventory = inventoryService.getInventory();
        assertNotNull(inventory);
        assertEquals(2, inventory.getFoods().size());

        boolean containsCoffee = inventory.getFoods().stream()
                .anyMatch(f -> f.getFoodName().equals("COFFEE"));
        boolean containsTea = inventory.getFoods().stream()
                .anyMatch(f -> f.getFoodName().equals("TEA"));

        assertTrue(containsCoffee);
        assertTrue(containsTea);
    }


}
