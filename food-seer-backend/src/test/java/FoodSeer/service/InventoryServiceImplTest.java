package FoodSeer.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import FoodSeer.dto.InventoryDto;
import FoodSeer.entity.Food;
import FoodSeer.exception.ResourceNotFoundException;
import FoodSeer.mapper.InventoryMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * Tests InventoryServiceImpl for FoodSeer.
 */
@SpringBootTest
@Transactional
public class InventoryServiceImplTest {

    /** Reference to InventoryService (and InventoryServiceImpl). */
    @Autowired
    private InventoryService inventoryService;

    /** Reference to EntityManager */
    @Autowired
    private EntityManager entityManager;
    
    /** Reference to FoodService */
    @Autowired
    private FoodService foodService;

    /**
     * Sets up the test case.
     * We assume only one inventory row.
     */
    @BeforeEach
    public void setUp() throws Exception {
        // inventory_foods rows must go first - they hold a foreign key onto inventory.id,
        // so deleting inventory directly while they still exist violates that constraint
        entityManager.createNativeQuery("DELETE FROM inventory_foods").executeUpdate();
        // Clear inventory table before each test
        final Query query = entityManager.createNativeQuery("DELETE FROM inventory");
        query.executeUpdate();
    }

    /**
     * Tests InventoryService.createInventory().
     */
    @Test
    @Transactional
    public void testCreateInventory() {

        final List<Food> foods = new ArrayList<>();

        final Food pizza = new Food("pizza", 20, 10, new ArrayList<>());
        final Food pasta = new Food("pasta", 30, 12, new ArrayList<>());
        final Food salad = new Food("salad", 40, 8, new ArrayList<>());

        foods.add(pizza);
        foods.add(pasta);
        foods.add(salad);

        final InventoryDto inventoryDto = new InventoryDto(1L, foods);

        final InventoryDto createdInventoryDto = inventoryService.createInventory(inventoryDto);

        assertAll("InventoryDto contents",
                () -> assertEquals(inventoryDto.getId(), createdInventoryDto.getId()),
                () -> assertEquals(20, createdInventoryDto.getFoods().get(0).getAmount()),
                () -> assertEquals(30, createdInventoryDto.getFoods().get(1).getAmount()),
                () -> assertEquals(40, createdInventoryDto.getFoods().get(2).getAmount()));
    }

    /**
     * Tests InventoryService.updateInventory().
     */
    @Test
    @Transactional
    public void testUpdateInventory() {

        final List<Food> foods = new ArrayList<>();

        final Food pizza = new Food("pizza", 20, 10, new ArrayList<>());
        final Food pasta = new Food("pasta", 30, 12, new ArrayList<>());

        foods.add(pizza);
        foods.add(pasta);

        final InventoryDto inventoryDto = new InventoryDto(1L, foods);
        final InventoryDto createdInventoryDto = inventoryService.createInventory(inventoryDto);

        // Modify existing food amounts
        pizza.setAmount(100);
        pasta.setAmount(100);

        final List<Food> updatedFoods = new ArrayList<>();
        updatedFoods.add(pizza);
        updatedFoods.add(pasta);

        createdInventoryDto.setFoods(updatedFoods);

        final InventoryDto updatedInventoryDto = inventoryService.updateInventory(createdInventoryDto);

        assertAll("Updated InventoryDto contents",
                () -> assertEquals(createdInventoryDto.getId(), updatedInventoryDto.getId()),
                () -> assertEquals(100, updatedInventoryDto.getFoods().get(0).getAmount()),
                () -> assertEquals(100, updatedInventoryDto.getFoods().get(1).getAmount()));
    }

    /**
     * ✅ Tests InventoryMapper null-handling (mapToInventoryDto & mapToInventory)
     */
    @Test
    public void testInventoryMapperNullCases() {
        // When Inventory is null → InventoryDto should be null
        assertNull(InventoryMapper.mapToInventoryDto(null));

        // When InventoryDto is null → Inventory should be null
        assertNull(InventoryMapper.mapToInventory(null));
    }
    
    /**
     * Tests updateInventory throws ResourceNotFoundException when no inventory exists.
     */
    @Test
    @Transactional
    public void testUpdateInventoryNotFound() {
        // No inventory exists due to @BeforeEach wiping table

        // Prepare dummy InventoryDto (id doesn't matter — repo will throw)
        InventoryDto fakeInventory = new InventoryDto(1L, new ArrayList<>());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
            inventoryService.updateInventory(fakeInventory);
        });

        assertEquals("Inventory does not exist with id of " + fakeInventory.getId(), ex.getMessage());
    }
    
    @Test
    @Transactional
    public void testGetInventoryCreatesWhenEmpty() {
        // ensure DB is empty (BeforeEach already does this, but explicit safety)
        assertEquals(0, entityManager.createNativeQuery("SELECT * FROM inventory").getResultList().size());

        InventoryDto result = inventoryService.getInventory();

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertNotNull(result.getFoods());
        assertTrue(result.getFoods().isEmpty());

        // Verify that a row was created in DB
        int count = entityManager.createNativeQuery("SELECT * FROM inventory").getResultList().size();
        assertEquals(1, count);
    }
    
    @Test
    @Transactional
    public void testGetInventoryReturnsExisting() {
        // First call creates the (empty) inventory
        InventoryDto first = inventoryService.getInventory();
        assertNotNull(first);
        assertEquals(1L, first.getId());
        assertTrue(first.getFoods().isEmpty());

        // Add a food the correct way (createFood updates/creates inventory entries)
        foodService.createFood(new FoodSeer.dto.FoodDto("burger", 50, 5, new ArrayList<>()));

        // Second call should return the same existing inventory, now containing "burger"
        InventoryDto second = inventoryService.getInventory();
        assertNotNull(second);
        assertEquals(1L, second.getId());
        assertEquals(1, second.getFoods().size());
        assertEquals("BURGER", second.getFoods().get(0).getFoodName());
    }



}
