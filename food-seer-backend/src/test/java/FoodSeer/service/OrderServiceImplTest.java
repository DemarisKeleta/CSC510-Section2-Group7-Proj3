// java
package FoodSeer.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import FoodSeer.dto.OrderDto;
import FoodSeer.dto.DriverStatsDto;
import FoodSeer.entity.Order;
import FoodSeer.entity.Food;
import FoodSeer.entity.User;
import FoodSeer.entity.DriverStats;
import FoodSeer.exception.ResourceNotFoundException;
import FoodSeer.mapper.OrderMapper;
import FoodSeer.mapper.DriverStatsMapper;
import FoodSeer.repositories.FoodRepository;
import FoodSeer.repositories.OrderRepository;
import FoodSeer.service.impl.OrderServiceImpl;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private UserService userService;

    @Mock
    private DriverStatsService driverStatsService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrder_whenNoAuthenticatedUser_throws() {
        // Arrange
        OrderDto dto = mock(OrderDto.class);
        when(dto.getFoods()).thenReturn(new ArrayList<>());
        when(userService.getCurrentUser()).thenReturn(null);

        // Act / Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> orderService.createOrder(dto));
        assertEquals("No authenticated user found", ex.getMessage());
    }

    @Test
    void createOrder_success_returnsMappedDto() {
        // Arrange: create a food with id and mock repository to return a managed entity
        Food inputFood = new Food();
        inputFood.setId(5L);
        inputFood.setFoodName("COFFEE");
        inputFood.setAmount(10);
        inputFood.setPrice(3);

        OrderDto request = mock(OrderDto.class);
        when(request.getFoods()).thenReturn(List.of(inputFood));
        when(request.getName()).thenReturn("MyOrder");

        Food managedFood = new Food();
        managedFood.setId(5L);
        managedFood.setFoodName("COFFEE");

        when(foodRepository.findById(5L)).thenReturn(Optional.of(managedFood));

        User currentUser = new User();
        currentUser.setUsername("customer");
        when(userService.getCurrentUser()).thenReturn(currentUser);

        Order saved = new Order();
        saved.setId(100L);
        saved.setName("MyOrder");
        saved.setFoods(List.of(managedFood));
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        try (MockedStatic<OrderMapper> mocked = mockStatic(OrderMapper.class)) {
            mocked.when(() -> OrderMapper.mapToOrderDto(saved)).thenReturn(new OrderDto(100L, "MyOrder"));

            // Act
            OrderDto result = orderService.createOrder(request);

            // Assert
            assertNotNull(result);
            assertEquals(100L, result.getId());
            assertEquals("MyOrder", result.getName());
            verify(orderRepository, times(1)).save(any(Order.class));
        }
    }

    @Test
    void getOrderById_whenNotFound_throwsResourceNotFound() {
        when(orderRepository.findById(77L)).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> orderService.getOrderById(77L));
        assertTrue(ex.getMessage().contains("Order does not exist with id 77"));
    }

    @Test
    void getOrderById_mapsAndReturnsDto() {
        Order order = new Order();
        order.setId(2L);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
        try (MockedStatic<OrderMapper> mocked = mockStatic(OrderMapper.class)) {
            mocked.when(() -> OrderMapper.mapToOrderDto(order)).thenReturn(new OrderDto(2L, "X"));
            OrderDto dto = orderService.getOrderById(2L);
            assertEquals(2L, dto.getId());
        }
    }

    @Test
    void getAllOrders_and_filtersFulfilled_unfulfilled() {
        Order o1 = new Order(); o1.setId(1L);
        Order o2 = new Order(); o2.setId(2L);
        when(orderRepository.findAll()).thenReturn(List.of(o1, o2));
        try (MockedStatic<OrderMapper> mocked = mockStatic(OrderMapper.class)) {
            OrderDto d1 = new OrderDto(1L, "A"); d1.setIsFulfilled(true);
            OrderDto d2 = new OrderDto(2L, "B"); d2.setIsFulfilled(false);
            mocked.when(() -> OrderMapper.mapToOrderDto(o1)).thenReturn(d1);
            mocked.when(() -> OrderMapper.mapToOrderDto(o2)).thenReturn(d2);

            List<OrderDto> all = orderService.getAllOrders();
            assertEquals(2, all.size());

            List<OrderDto> fulfilled = orderService.getAllFulfilledOrders();
            assertEquals(1, fulfilled.size());
            assertTrue(fulfilled.get(0).getIsFulfilled());

            List<OrderDto> unfulfilled = orderService.getAllUnfulfilledOrders();
            assertEquals(1, unfulfilled.size());
            assertFalse(unfulfilled.get(0).getIsFulfilled());
        }
    }

    @Test
    void fulfillOrder_success_deductsStock_and_marksFulfilled() {
        // Arrange - order with two identical food items (counts as 2)
        Food foo = new Food();
        foo.setId(10L);
        foo.setFoodName("SANDWICH");
        foo.setAmount(5);
        foo.setPrice(2);

        Order order = new Order();
        order.setId(50L);
        // simulate order containing two of same Food object in the list -> counting logic will count 2
        order.setFoods(new ArrayList<>(List.of(foo, foo)));

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        // foodRepository should be queried for id 10
        Food managed = new Food();
        managed.setId(10L);
        managed.setFoodName("SANDWICH");
        managed.setAmount(5);
        when(foodRepository.findById(10L)).thenReturn(Optional.of(managed));
        when(foodRepository.save(any(Food.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<OrderMapper> mocked = mockStatic(OrderMapper.class)) {
            mocked.when(() -> OrderMapper.mapToOrderDto(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                OrderDto dto = new OrderDto(o.getId(), o.getName());
                dto.setIsFulfilled(o.getIsFulfilled());
                return dto;
            });

            OrderDto dto = orderService.fulfillOrder(50L);

            assertTrue(dto.getIsFulfilled());
            // amount deducted by 2 (ordered quantity)
            assertEquals(3, managed.getAmount());
            verify(foodRepository, times(1)).save(managed);
            verify(orderRepository, times(1)).save(order);
        }
    }

    @Test
    void fulfillOrder_notEnoughStock_throws() {
        Food foo = new Food();
        foo.setId(20L);
        foo.setFoodName("TEA");
        foo.setAmount(1);

        Order order = new Order();
        order.setId(51L);
        // order contains two TEAs
        order.setFoods(new ArrayList<>(List.of(foo, foo)));

        when(orderRepository.findById(51L)).thenReturn(Optional.of(order));

        Food managed = new Food();
        managed.setId(20L);
        managed.setFoodName("TEA");
        managed.setAmount(1);
        when(foodRepository.findById(20L)).thenReturn(Optional.of(managed));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> orderService.fulfillOrder(51L));
        assertTrue(ex.getMessage().contains("Not enough stock to fulfill the order for TEA"));
    }

    @Test
    void getCurrentUserOrders_and_variants_requireAuthentication() {
        // No user -> expect exceptions for current-user methods
        when(userService.getCurrentUser()).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> orderService.getCurrentUserOrders());
        assertThrows(IllegalStateException.class, () -> orderService.getCurrentUserFulfilledOrders());
        assertThrows(IllegalStateException.class, () -> orderService.getCurrentUserUnfulfilledOrders());

        // With user present, repository calls should be forwarded and mapped
        User user = new User();
        user.setUsername("u1");
        when(userService.getCurrentUser()).thenReturn(user);

        Order o = new Order(); o.setId(7L);
        when(orderRepository.findByUser(user)).thenReturn(List.of(o));
        when(orderRepository.findByUserAndIsFulfilled(user, true)).thenReturn(List.of(o));
        when(orderRepository.findByUserAndIsFulfilled(user, false)).thenReturn(List.of(o));

        try (MockedStatic<OrderMapper> mocked = mockStatic(OrderMapper.class)) {
            mocked.when(() -> OrderMapper.mapToOrderDto(o)).thenReturn(new OrderDto(7L, "UOrder"));
            assertEquals(1, orderService.getCurrentUserOrders().size());
            assertEquals(1, orderService.getCurrentUserFulfilledOrders().size());
            assertEquals(1, orderService.getCurrentUserUnfulfilledOrders().size());
        }
    }

    @Test
    void updateOrder_whenNotFound_returnsNull() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
        OrderDto res = orderService.updateOrder(999L, "any", "Picked Up");
        assertNull(res);
        verify(orderRepository, never()).save(any());
    }

//    @Test
//    void updateOrder_pickedUp_setsDriver_and_persists() {
//        Order order = new Order();
//        order.setId(200L);
//        order.setStatus("Placed");
//        order.setIsFulfilled(false);
//
//        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
//        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
//
//        DriverStatsDto statsDto = new DriverStatsDto();
//        when(driverStatsService.getDriverStats("driverA")).thenReturn(statsDto);
//
//        DriverStats mapped = new DriverStats();
//        mapped.setUsername("driverA");
//
//        try (MockedStatic<DriverStatsMapper> mocked = mockStatic(DriverStatsMapper.class)) {
//            mocked.when(() -> DriverStatsMapper.mapToDriverStats(statsDto)).thenReturn(mapped);
//
//            OrderDto dto = orderService.updateOrder(200L, "driverA", "Picked Up");
//
//            assertNotNull(dto);
//            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
//            verify(orderRepository, times(1)).save(captor.capture());
//            Order saved = captor.getValue();
//            assertNotNull(saved.getDriver());
//            assertEquals("driverA", saved.getDriver().getUsername());
//            assertFalse(saved.getIsFulfilled());
//        }
//    }

    @Test
    void updateOrder_delivered_bySameDriver_setsFulfilled_and_updatesEarnings() {
        Order order = new Order();
        order.setId(201L);
        order.setStatus("Picked Up");
        order.setIsFulfilled(false);
        order.setDeliveryCost(BigDecimal.valueOf(15));

        DriverStats driver = new DriverStats();
        driver.setUsername("driverB");
        order.setDriver(driver);

        when(orderRepository.findById(201L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDto dto = orderService.updateOrder(201L, "driverB", "Delivered");

        assertNotNull(dto);
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(captor.capture());
        Order saved = captor.getValue();
        assertTrue(saved.getIsFulfilled());
        verify(driverStatsService, times(1)).updateTotalEarnings("driverB", saved.getDeliveryCost());
    }

    /**
     * Marking "Delivered" before the order was ever "Picked Up" leaves
     * order.getDriver() null - this must be rejected, not NPE into a 500.
     */
    @Test
    void updateOrder_deliveredWithoutPickup_throwsInsteadOfNpe() {
        Order order = new Order();
        order.setId(202L);
        order.setStatus("Placed");
        order.setIsFulfilled(false);
        // driver was never set - never went through "Picked Up"

        when(orderRepository.findById(202L)).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrder(202L, "driverB", "Delivered"));
        verify(orderRepository, never()).save(any());
        verify(driverStatsService, never()).updateTotalEarnings(anyString(), any());
    }

    /**
     * The username on the "Delivered" request must match the driver who
     * actually picked the order up - otherwise the wrong driver could be
     * credited and the order left in a contradictory status/isFulfilled state.
     */
    @Test
    void updateOrder_deliveredByMismatchedDriver_throwsAndDoesNotCredit() {
        Order order = new Order();
        order.setId(203L);
        order.setStatus("Picked Up");
        order.setIsFulfilled(false);
        order.setDeliveryCost(BigDecimal.valueOf(10));

        DriverStats driver = new DriverStats();
        driver.setUsername("driverB");
        order.setDriver(driver);

        when(orderRepository.findById(203L)).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrder(203L, "driverC", "Delivered"));
        verify(orderRepository, never()).save(any());
        verify(driverStatsService, never()).updateTotalEarnings(anyString(), any());
    }

    /**
     * Calling "Delivered" twice (e.g. a double-click/retry) must not credit
     * the driver's earnings a second time.
     */
    @Test
    void updateOrder_deliveredTwice_doesNotDoubleCreditEarnings() {
        Order order = new Order();
        order.setId(204L);
        order.setStatus("Delivered");
        // already fulfilled from the first "Delivered" call
        order.setIsFulfilled(true);
        order.setDeliveryCost(BigDecimal.valueOf(12));

        DriverStats driver = new DriverStats();
        driver.setUsername("driverB");
        order.setDriver(driver);

        when(orderRepository.findById(204L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDto dto = orderService.updateOrder(204L, "driverB", "Delivered");

        assertNotNull(dto);
        assertTrue(dto.getIsFulfilled());
        verify(driverStatsService, never()).updateTotalEarnings(anyString(), any());
    }

    @Test
    void getAvailableOrders_and_getActiveOrders_mapResults() {
        Order p1 = new Order(); p1.setId(300L);
        when(orderRepository.findByStatus("Placed")).thenReturn(List.of(p1));

        Order active = new Order(); active.setId(301L);
        when(orderRepository.findByDriverUsernameAndStatus("drv", "Picked Up")).thenReturn(List.of(active));

        try (MockedStatic<OrderMapper> mocked = mockStatic(OrderMapper.class)) {
            mocked.when(() -> OrderMapper.mapToOrderDto(p1)).thenReturn(new OrderDto(300L, "Placed"));
            mocked.when(() -> OrderMapper.mapToOrderDto(active)).thenReturn(new OrderDto(301L, "Active"));

            List<OrderDto> avail = orderService.getAvailableOrders();
            assertEquals(1, avail.size());
            assertEquals(300L, avail.get(0).getId());

            List<OrderDto> act = orderService.getActiveOrders("drv");
            assertEquals(1, act.size());
            assertEquals(301L, act.get(0).getId());
        }
    }
}