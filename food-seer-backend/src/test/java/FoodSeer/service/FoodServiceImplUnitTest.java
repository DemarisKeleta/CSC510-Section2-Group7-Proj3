package FoodSeer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import FoodSeer.dto.FoodDto;
import FoodSeer.entity.Food;
import FoodSeer.entity.Inventory;
import FoodSeer.entity.Order;
import FoodSeer.repositories.FoodRepository;
import FoodSeer.repositories.InventoryRepository;
import FoodSeer.repositories.OrderRepository;
import FoodSeer.service.impl.FoodServiceImpl;

/**
 * Mockito-only unit tests for two bug fixes in {@link FoodServiceImpl} -
 * the rating-lock fix and the delete/inventory-cleanup fix. These are kept
 * separate from {@code FoodServiceImplTest} (a {@code @SpringBootTest}
 * integration test against a real database) because that class's own
 * {@code @BeforeEach} has a pre-existing, unrelated fixture bug (it calls
 * {@code foodRepository.deleteAll()} directly, bypassing the service layer
 * and hitting the same FK constraint these fixes address) - using mocks
 * here avoids depending on that fixture at all.
 */
@ExtendWith(MockitoExtension.class)
class FoodServiceImplUnitTest {

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private FoodServiceImpl foodService;

    /**
     * rateFoodInOrder must take a row-level lock on the Food being rated
     * (via findByIdForUpdate), not a plain findById, so two concurrent
     * ratings on the same food can't both read the same starting average.
     */
    @Test
    void rateFoodInOrder_locksTheFoodRow_notPlainFindById() {
        final Food food = new Food("PIZZA", 5, 10, List.of());
        food.setId(1L);
        food.setRating(4.0);
        food.setNumberOfRatings(1);

        final Order order = new Order(100L, "Order100");
        order.setFoods(new ArrayList<>(List.of(food)));
        order.setIsFulfilled(true);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(foodRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(food));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(foodRepository.saveAndFlush(any(Food.class))).thenAnswer(inv -> inv.getArgument(0));

        final FoodDto result = foodService.rateFoodInOrder(100L, 1L, 5.0);

        // new average of (4.0*1 + 5.0) / 2 = 4.5
        assertEquals(4.5, result.getRating());
        assertEquals(2, result.getNumberOfRatings());
        // the row-locking lookup must have been used...
        verify(foodRepository).findByIdForUpdate(1L);
        // ...and the plain, non-locking lookup must NOT have been used for this food
        verify(foodRepository, never()).findById(anyLong());
    }

    /**
     * Deleting a food that is still listed in an Inventory's foods list
     * must remove it from that Inventory first - otherwise the delete
     * below would fail with a foreign-key constraint violation on the
     * inventory_foods join table.
     */
    @Test
    void deleteFood_removesFoodFromInventoryBeforeDeleting() {
        final Food food = new Food("SALAD", 5, 7, List.of());
        food.setId(2L);

        final Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setFoods(new ArrayList<>(List.of(food)));

        when(foodRepository.findById(2L)).thenReturn(Optional.of(food));
        when(orderRepository.findOrdersContainingFood(food)).thenReturn(List.of());
        when(inventoryRepository.findAll()).thenReturn(List.of(inventory));
        when(inventoryRepository.saveAndFlush(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        foodService.deleteFood(2L);

        // the food must have been stripped out of the inventory's list...
        assertFalse(inventory.getFoods().stream().anyMatch(f -> f.getId().equals(2L)));
        verify(inventoryRepository).saveAndFlush(inventory);
        // ...before the food row itself was deleted
        verify(foodRepository).delete(food);
    }

    /**
     * A food that was never added to any Inventory must still delete
     * cleanly - deleteFood shouldn't assume every food has an inventory
     * entry, or touch inventories that don't contain it.
     */
    @Test
    void deleteFood_foodNotInAnyInventory_stillDeletesCleanly() {
        final Food food = new Food("WATER", 5, 1, List.of());
        food.setId(3L);

        final Food otherFood = new Food("BREAD", 5, 2, List.of());
        otherFood.setId(4L);
        final Inventory unrelatedInventory = new Inventory();
        unrelatedInventory.setId(1L);
        unrelatedInventory.setFoods(new ArrayList<>(List.of(otherFood)));

        when(foodRepository.findById(3L)).thenReturn(Optional.of(food));
        when(orderRepository.findOrdersContainingFood(food)).thenReturn(List.of());
        when(inventoryRepository.findAll()).thenReturn(List.of(unrelatedInventory));

        foodService.deleteFood(3L);

        // the unrelated inventory must be left untouched
        verify(inventoryRepository, never()).saveAndFlush(any(Inventory.class));
        verify(foodRepository).delete(food);
    }
}
