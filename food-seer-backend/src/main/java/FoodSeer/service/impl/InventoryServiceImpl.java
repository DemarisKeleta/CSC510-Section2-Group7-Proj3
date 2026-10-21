package FoodSeer.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import FoodSeer.dto.FoodDto;
import FoodSeer.dto.InventoryDto;
import FoodSeer.entity.Food;
import FoodSeer.entity.Inventory;
import FoodSeer.exception.ResourceNotFoundException;
import FoodSeer.mapper.FoodMapper;
import FoodSeer.mapper.InventoryMapper;
import FoodSeer.repositories.InventoryRepository;
import FoodSeer.service.FoodService;
import FoodSeer.service.InventoryService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Implementation of the InventoryService interface for FoodSeer.
 * Manages the collection of Food items available in the system.
 */
@Service
public class InventoryServiceImpl implements InventoryService {

    /** Connection to the repository to work with the DAO + database */
    @Autowired
    private InventoryRepository inventoryRepository;

    /**
     * Lazy dependency injection to prevent circular dependency
     * between InventoryService and FoodService.
     */
    @Autowired
    @Lazy
    private FoodService foodService;

    /** Reference to EntityManager */
    @Autowired
    private EntityManager entityManager;

    /**
     * Creates the inventory.
     *
     * @param inventoryDto
     *            The inventory to create
     * @return The created inventory as a DTO
     */
    @Override
    public InventoryDto createInventory(final InventoryDto inventoryDto) {
        final Inventory inventory = InventoryMapper.mapToInventory(inventoryDto);
        final Inventory savedInventory = inventoryRepository.saveAndFlush(inventory);
        return InventoryMapper.mapToInventoryDto(savedInventory);
    }

    /**
     * Returns the single inventory.
     *
     * @return The single inventory as a DTO
     */
    @Override
    public InventoryDto getInventory() {
        final List<Inventory> inventoryList = inventoryRepository.findAll();

        if (inventoryList.isEmpty()) {
            // initialize new empty food list in the new inventory
            final List<Food> foods = new ArrayList<>();
            final InventoryDto newInventoryDto = new InventoryDto(1L, foods);
            return createInventory(newInventoryDto);
        }

        return InventoryMapper.mapToInventoryDto(inventoryList.get(0));
    }

    /**
     * Updates the contents of the inventory.
     *
     * This method does NOT add new foods to the inventory.
     * It only updates the existing foods' values (amount, price, etc.)
     * if they already exist in the inventory.
     *
     * @param inventoryDto
     *            The inventory data to update
     * @return The updated inventory as a DTO
     */
    @Override
    @Transactional
    public InventoryDto updateInventory(final InventoryDto inventoryDto) {

        final Inventory inventory = inventoryRepository.findById(1L)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory does not exist with id of " + inventoryDto.getId()));

        final List<Integer> indexesToRemove = new ArrayList<>();
        final List<Food> foodsToAdd = new ArrayList<>();

        for (final Food f : inventoryDto.getFoods()) {
            final int size = inventory.getFoods().size();
            for (int index = 0; index < size; index++) {
                if (inventory.getFoods().get(index).getFoodName().equals(f.getFoodName())) {

                    // Update existing food item
                    // pass the existing food's calories through so inventory-driven updates don't wipe it out
                    final FoodDto fDto = foodService.updateFood(f.getFoodName(), f.getAmount(), f.getPrice(), f.getAllergies(), f.getCalories());
                    final Food updatedFood = FoodMapper.mapToFood(fDto);

                    // track indexes and updated foods
                    indexesToRemove.add(index);
                    foodsToAdd.add(updatedFood);

                    FoodMapper.mapToFoodDto(updatedFood);
                }
            }
        }

        // clear persistence context to prevent detached entity issues
        entityManager.clear();

        // apply updates to inventory
        int num = indexesToRemove.size();
        while (num > 0) {
            inventory.getFoods().remove((int) indexesToRemove.get(num - 1));
            inventory.getFoods().add(foodsToAdd.get(num - 1));
            num--;
        }

        final Inventory savedInventory = inventoryRepository.save(inventory);
        return InventoryMapper.mapToInventoryDto(savedInventory);
    }
}
