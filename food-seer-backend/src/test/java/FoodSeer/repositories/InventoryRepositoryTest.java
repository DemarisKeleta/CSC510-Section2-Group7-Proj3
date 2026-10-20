package FoodSeer.repositories;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import FoodSeer.entity.Food;
import FoodSeer.entity.Inventory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * Tests InventoryRepository for FoodSeer.
 * Uses the real database - not an embedded one.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class InventoryRepositoryTest {

    /** Reference to inventory repository */
    @Autowired
    private InventoryRepository inventoryRepository;

    /** Reference to EntityManager */
    @Autowired
    private TestEntityManager testEntityManager;

    /** Reference to inventory */
    private Inventory inventory;

    /**
     * Sets up the test case. We assume only one inventory row.
     *
     * @throws Exception if error
     */
    @BeforeEach
    public void setUp() throws Exception {
        final EntityManager entityManager = testEntityManager.getEntityManager();
        // inventory_foods rows must go first - they hold a foreign key onto inventory.id,
        // so deleting inventory directly while they still exist violates that constraint
        entityManager.createNativeQuery("DELETE FROM inventory_foods").executeUpdate();
        final Query query = entityManager.createNativeQuery("DELETE FROM inventory");
        query.executeUpdate();

        // Create sample food items
        final List<Food> foods = new ArrayList<>();

        final Food pizza = new Food("pizza", 5, 10, new ArrayList<>());
        final Food pasta = new Food("pasta", 8, 12, new ArrayList<>());
        final Food salad = new Food("salad", 15, 6, new ArrayList<>());

        foods.add(pizza);
        foods.add(pasta);
        foods.add(salad);

        inventory = new Inventory(1L, foods);
        inventoryRepository.save(inventory);
    }

    /**
     * Test saving the inventory and retrieving from the repository.
     */
    @Test
    public void testSaveAndGetInventory() {
        final Inventory fetchedInventory = inventoryRepository.findById(1L).get();
        assertAll("Inventory contents",
                () -> assertEquals(1L, fetchedInventory.getId()),
                () -> assertEquals(5, fetchedInventory.getFoods().get(0).getAmount()),
                () -> assertEquals(8, fetchedInventory.getFoods().get(1).getAmount()),
                () -> assertEquals(15, fetchedInventory.getFoods().get(2).getAmount()));
    }

    /**
     * Tests updating the inventory.
     */
    @Test
    public void testUpdateInventory() {
        final Inventory fetchedInventory = inventoryRepository.findById(1L).get();

        // Replace foods with updated quantities
        final List<Food> newFoods = new ArrayList<>();

        final Food pizza = new Food("pizza", 20, 10, new ArrayList<>());
        final Food pasta = new Food("pasta", 30, 12, new ArrayList<>());
        final Food salad = new Food("salad", 40, 6, new ArrayList<>());

        newFoods.add(pizza);
        newFoods.add(pasta);
        newFoods.add(salad);

        fetchedInventory.setFoods(newFoods);

        final Inventory updatedInventory = inventoryRepository.save(fetchedInventory);
        assertAll("Updated inventory contents",
                () -> assertEquals(1L, updatedInventory.getId()),
                () -> assertEquals(20, updatedInventory.getFoods().get(0).getAmount()),
                () -> assertEquals(30, updatedInventory.getFoods().get(1).getAmount()),
                () -> assertEquals(40, updatedInventory.getFoods().get(2).getAmount()));
    }
}
