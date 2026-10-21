package FoodSeer.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import FoodSeer.entity.Food;
import FoodSeer.entity.Order;
import FoodSeer.entity.User;

/**
 * Repository interface for managing Order entities in the database.
 * Provides CRUD operations through Spring Data JPA.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * Find all orders for a specific user.
     *
     * @param user the user
     * @return list of orders belonging to the user
     */
    List<Order> findByUser(User user);
    
    /**
     * Find all fulfilled orders for a specific user.
     *
     * @param user the user
     * @param isFulfilled true for fulfilled orders
     * @return list of fulfilled orders belonging to the user
     */
    List<Order> findByUserAndIsFulfilled(User user, boolean isFulfilled);
    
    /**
     * Find all orders that contain a specific food.
     *
     * @param food the food to search for
     * @return list of orders containing the food
     */
    @Query("SELECT o FROM Order o JOIN o.foods f WHERE f = :food")
    List<Order> findOrdersContainingFood(@Param("food") Food food);

    List<Order> findByStatus(String status);

    List<Order> findByDriverUsernameAndStatus(@Param("username") String username, @Param("status") String status);

    List<Order> findByDriverUsernameAndStatusIn(String username, List<String> statuses);
}
