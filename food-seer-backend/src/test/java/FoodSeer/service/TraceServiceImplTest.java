package FoodSeer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import FoodSeer.constant.IncidentSource;
import FoodSeer.dto.IncidentDto;
import FoodSeer.entity.Food;
import FoodSeer.entity.Incident;
import FoodSeer.entity.Notification;
import FoodSeer.entity.Order;
import FoodSeer.entity.User;
import FoodSeer.exception.ResourceNotFoundException;
import FoodSeer.repositories.FoodRepository;
import FoodSeer.repositories.IncidentRepository;
import FoodSeer.repositories.NotificationRepository;
import FoodSeer.repositories.OrderRepository;
import FoodSeer.service.impl.TraceServiceImpl;

/**
 * Unit tests for {@link TraceServiceImpl}. Repositories are mocks, so there is
 * no Spring context and no database.
 */
public class TraceServiceImplTest {

    private static final LocalDateTime NOON = LocalDateTime.of(2026, 10, 6, 12, 0);

    private OrderRepository orderRepository;
    private FoodRepository foodRepository;
    private IncidentRepository incidentRepository;
    private NotificationRepository notificationRepository;
    private TraceServiceImpl service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        foodRepository = mock(FoodRepository.class);
        incidentRepository = mock(IncidentRepository.class);
        notificationRepository = mock(NotificationRepository.class);

        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> {
            final Incident incident = invocation.getArgument(0);
            incident.setId(100L);
            return incident;
        });
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service = new TraceServiceImpl(orderRepository, foodRepository, incidentRepository,
                notificationRepository, 2);
    }

    // Helpers
    private Food food(final long id, final String name, final String... allergyTags) {
        final Food food = new Food(name, 10, 5, new ArrayList<>(Arrays.asList(allergyTags)));
        food.setId(id);
        return food;
    }

    private User user(final String username, final String restrictions) {
        return User.builder().username(username).dietaryRestrictions(restrictions).build();
    }

    private Order order(final long id, final User owner, final LocalDateTime placedAt, final Food... foods) {
        final Order order = new Order();
        order.setId(id);
        order.setUser(owner);
        order.setCreatedAt(placedAt);
        order.setFoods(new ArrayList<>(Arrays.asList(foods)));
        return order;
    }

    /** Makes the food findable by id and tells the repository which orders contain it. */
    private void stubFood(final Food food, final Order... ordersContainingIt) {
        when(foodRepository.findById(food.getId())).thenReturn(Optional.of(food));
        when(orderRepository.findOrdersContainingFood(food)).thenReturn(Arrays.asList(ordersContainingIt));
    }

    private TraceResult restaurantReport(final String allergen, final Long... foodIds) {
        return service.reportRestaurantIncident("staff1", allergen, Arrays.asList(foodIds), NOON.minusHours(1),
                NOON.plusHours(1));
    }

    // Restaurant report: who gets notified
    @Test
    public void testRestaurantReportNotifiesAllergicCustomerInWindow() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Order aliceOrder = order(1, user("alice", "PEANUTS"), NOON, cookie);
        final Order bobOrder = order(2, user("bob", "MILK"), NOON.plusMinutes(10), cookie);
        stubFood(cookie, aliceOrder, bobOrder);

        final TraceResult result = restaurantReport("peanut", 10L);

        assertEquals(Arrays.asList(1L, 2L), result.getExposedOrderIds());
        assertEquals(Arrays.asList("alice"), result.getNotifiedUsernames());
        assertEquals(1, result.getNotifications().size());
        verify(notificationRepository).saveAll(result.getNotifications());
    }

    @Test
    public void testRestaurantReportSavesIncidentWithAllFields() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        stubFood(cookie);

        final TraceResult result = restaurantReport("Peanuts", 10L);
        final Incident incident = result.getIncident();

        assertEquals(100L, incident.getId());
        assertEquals(IncidentSource.RESTAURANT, incident.getSource());
        assertEquals("PEANUT", incident.getAllergen());
        assertEquals("staff1", incident.getReporterUsername());
        assertNull(incident.getSourceOrderId());
        assertEquals(new LinkedHashSet<>(Arrays.asList(10L)), incident.getSuspectFoodIds());
        assertEquals(NOON.minusHours(1), incident.getWindowStart());
        assertEquals(NOON.plusHours(1), incident.getWindowEnd());
        assertNotNull(incident.getReportedAt());
        verify(incidentRepository).save(incident);
    }

    @Test
    public void testNotificationFieldsAndPrivateMessage() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Order aliceOrder = order(1, user("alice", "PEANUTS"), NOON, cookie);
        final Order bobOrder = order(2, user("bob", "PEANUTS"), NOON.plusMinutes(5), cookie);
        stubFood(cookie, aliceOrder, bobOrder);

        final TraceResult result = restaurantReport("peanut", 10L);
        final Notification forAlice = result.getNotifications().get(0);

        assertEquals(100L, forAlice.getIncidentId());
        assertEquals(1L, forAlice.getOrderId());
        assertEquals("alice", forAlice.getRecipientUsername());
        assertFalse(forAlice.isAcknowledged());
        assertNull(forAlice.getAcknowledgedAt());
        assertNotNull(forAlice.getCreatedAt());
        assertTrue(forAlice.getMessage().contains("#1"));
        assertTrue(forAlice.getMessage().contains("COOKIES"));
        assertTrue(forAlice.getMessage().contains("PEANUT"));
        assertFalse(forAlice.getMessage().contains("bob"));
        assertFalse(forAlice.getMessage().contains("staff1"));
    }

    @Test
    public void testPluralAndFormatDifferencesStillNotify() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Order order = order(1, user("alice", "peanuts, Tree-Nuts"), NOON, cookie);
        stubFood(cookie, order);

        final TraceResult result = restaurantReport("peanut", 10L);

        assertEquals(Arrays.asList("alice"), result.getNotifiedUsernames());
    }

    @Test
    public void testGenericNutsIncidentNotifiesPeanutAndTreeNutCustomers() {
        final Food bar = food(10, "GRANOLA BAR", "TREE-NUTS", "PEANUTS");
        final Order peanutOrder = order(1, user("alice", "PEANUTS"), NOON, bar);
        final Order treeNutOrder = order(2, user("bob", "TREE-NUTS"), NOON, bar);
        final Order milkOrder = order(3, user("carol", "MILK"), NOON, bar);
        stubFood(bar, peanutOrder, treeNutOrder, milkOrder);

        final TraceResult result = restaurantReport("nuts", 10L);

        assertEquals(Arrays.asList("alice", "bob"), result.getNotifiedUsernames());
    }

    @Test
    public void testFishIncidentDoesNotNotifyShellfishCustomer() {
        final Food roll = food(10, "SUSHI ROLL", "FISH");
        final Order fishOrder = order(1, user("alice", "FISH"), NOON, roll);
        final Order shellfishOrder = order(2, user("bob", "SHELLFISH"), NOON, roll);
        stubFood(roll, fishOrder, shellfishOrder);

        final TraceResult result = restaurantReport("fish", 10L);

        assertEquals(Arrays.asList("alice"), result.getNotifiedUsernames());
    }

    @Test
    public void testCustomerWithNoSavedRestrictionsIsNotNotified() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Order nullRestrictions = order(1, user("alice", null), NOON, cookie);
        final Order blankRestrictions = order(2, user("bob", "   "), NOON, cookie);
        final Order unrelated = order(3, user("carol", "SOY"), NOON, cookie);
        stubFood(cookie, nullRestrictions, blankRestrictions, unrelated);

        final TraceResult result = restaurantReport("peanut", 10L);

        assertEquals(Arrays.asList(1L, 2L, 3L), result.getExposedOrderIds());
        assertTrue(result.getNotifications().isEmpty());
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    public void testOneNotificationPerCustomerUsingTheirEarliestExposedOrder() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final User alice = user("alice", "PEANUTS");
        final Order later = order(5, alice, NOON.plusMinutes(30), cookie);
        final Order earlier = order(4, alice, NOON.minusMinutes(30), cookie);
        stubFood(cookie, later, earlier);

        final TraceResult result = restaurantReport("peanut", 10L);

        assertEquals(1, result.getNotifications().size());
        assertEquals(4L, result.getNotifications().get(0).getOrderId());
        assertEquals(Arrays.asList(4L, 5L), result.getExposedOrderIds());
    }

    @Test
    public void testOrderHoldingTwoSuspectFoodsIsCountedOnce() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Food bar = food(11, "GRANOLA BAR", "PEANUTS");
        final Order order = order(1, user("alice", "PEANUTS"), NOON, cookie, bar);
        stubFood(cookie, order);
        stubFood(bar, order);

        final TraceResult result = restaurantReport("peanut", 10L, 11L);

        assertEquals(Arrays.asList(1L), result.getExposedOrderIds());
        assertEquals(1, result.getNotifications().size());
        assertTrue(result.getNotifications().get(0).getMessage().contains("COOKIES"));
        assertTrue(result.getNotifications().get(0).getMessage().contains("GRANOLA BAR"));
    }

    @Test
    public void testOrdersFromDifferentSuspectFoodsAreCombined() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Food bar = food(11, "GRANOLA BAR", "PEANUTS");
        final Order cookieOrder = order(1, user("alice", "PEANUTS"), NOON, cookie);
        final Order barOrder = order(2, user("bob", "PEANUTS"), NOON.plusMinutes(1), bar);
        stubFood(cookie, cookieOrder);
        stubFood(bar, barOrder);

        final TraceResult result = restaurantReport("peanut", 10L, 11L);

        assertEquals(Arrays.asList("alice", "bob"), result.getNotifiedUsernames());
    }

    @Test
    public void testNoExposedOrdersStillSavesIncident() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        stubFood(cookie);

        final TraceResult result = restaurantReport("peanut", 10L);

        assertNotNull(result.getIncident().getId());
        assertTrue(result.getExposedOrderIds().isEmpty());
        assertTrue(result.getNotifications().isEmpty());
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    public void testOrderWithNoUserIsSkipped() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Order orphan = order(1, null, NOON, cookie);
        stubFood(cookie, orphan);

        final TraceResult result = restaurantReport("peanut", 10L);

        assertEquals(Arrays.asList(1L), result.getExposedOrderIds());
        assertTrue(result.getNotifications().isEmpty());
    }

    @Test
    public void testRepositoryReturningNullOrderListIsHandled() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        when(foodRepository.findById(10L)).thenReturn(Optional.of(cookie));
        when(orderRepository.findOrdersContainingFood(cookie)).thenReturn(null);

        final TraceResult result = restaurantReport("peanut", 10L);

        assertTrue(result.getExposedOrderIds().isEmpty());
    }

    // Restaurant report: the time window
    @Test
    public void testWindowBoundariesAreInclusive() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Order atStart = order(1, user("alice", "PEANUTS"), NOON.minusHours(1), cookie);
        final Order atEnd = order(2, user("bob", "PEANUTS"), NOON.plusHours(1), cookie);
        stubFood(cookie, atStart, atEnd);

        final TraceResult result = restaurantReport("peanut", 10L);

        assertEquals(Arrays.asList("alice", "bob"), result.getNotifiedUsernames());
    }

    @Test
    public void testOrdersJustOutsideTheWindowAreNotExposed() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Order tooEarly = order(1, user("alice", "PEANUTS"), NOON.minusHours(1).minusMinutes(1), cookie);
        final Order tooLate = order(2, user("bob", "PEANUTS"), NOON.plusHours(1).plusMinutes(1), cookie);
        stubFood(cookie, tooEarly, tooLate);

        final TraceResult result = restaurantReport("peanut", 10L);

        assertTrue(result.getExposedOrderIds().isEmpty());
        assertTrue(result.getNotifications().isEmpty());
    }

    @Test
    public void testOrderWithNoRecordedTimeIsSkipped() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Order legacy = order(1, user("alice", "PEANUTS"), null, cookie);
        stubFood(cookie, legacy);

        final TraceResult result = restaurantReport("peanut", 10L);

        assertTrue(result.getExposedOrderIds().isEmpty());
        assertTrue(result.getNotifications().isEmpty());
    }

    @Test
    public void testWindowOfZeroLengthMatchesOnlyThatInstant() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Order exact = order(1, user("alice", "PEANUTS"), NOON, cookie);
        final Order off = order(2, user("bob", "PEANUTS"), NOON.plusSeconds(1), cookie);
        stubFood(cookie, exact, off);

        final TraceResult result = service.reportRestaurantIncident("staff1", "peanut", Arrays.asList(10L), NOON,
                NOON);

        assertEquals(Arrays.asList(1L), result.getExposedOrderIds());
    }

    // Restaurant report: bad input
    @Test
    public void testRestaurantReportRejectsBlankReporter() {
        assertThrows(IllegalArgumentException.class, () -> service.reportRestaurantIncident(null, "peanut",
                Arrays.asList(10L), NOON.minusHours(1), NOON.plusHours(1)));
        assertThrows(IllegalArgumentException.class, () -> service.reportRestaurantIncident("  ", "peanut",
                Arrays.asList(10L), NOON.minusHours(1), NOON.plusHours(1)));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    public void testRestaurantReportRejectsBlankAllergen() {
        assertThrows(IllegalArgumentException.class, () -> restaurantReport(null, 10L));
        assertThrows(IllegalArgumentException.class, () -> restaurantReport("", 10L));
        assertThrows(IllegalArgumentException.class, () -> restaurantReport("   ", 10L));
        assertThrows(IllegalArgumentException.class, () -> restaurantReport("free", 10L));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    public void testRestaurantReportRejectsMissingFoods() {
        assertThrows(IllegalArgumentException.class, () -> service.reportRestaurantIncident("staff1", "peanut",
                null, NOON.minusHours(1), NOON.plusHours(1)));
        assertThrows(IllegalArgumentException.class, () -> service.reportRestaurantIncident("staff1", "peanut",
                Collections.<Long>emptyList(), NOON.minusHours(1), NOON.plusHours(1)));
        assertThrows(IllegalArgumentException.class, () -> service.reportRestaurantIncident("staff1", "peanut",
                Arrays.asList((Long) null), NOON.minusHours(1), NOON.plusHours(1)));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    public void testRestaurantReportRejectsMissingOrReversedWindow() {
        assertThrows(IllegalArgumentException.class, () -> service.reportRestaurantIncident("staff1", "peanut",
                Arrays.asList(10L), null, NOON));
        assertThrows(IllegalArgumentException.class, () -> service.reportRestaurantIncident("staff1", "peanut",
                Arrays.asList(10L), NOON, null));
        assertThrows(IllegalArgumentException.class, () -> service.reportRestaurantIncident("staff1", "peanut",
                Arrays.asList(10L), NOON.plusHours(1), NOON.minusHours(1)));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    public void testRestaurantReportRejectsUnknownFood() {
        when(foodRepository.findById(99L)).thenReturn(Optional.empty());

        final ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> restaurantReport("peanut", 99L));

        assertEquals("Food does not exist with id 99", ex.getMessage());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    public void testDuplicateFoodIdsAreLoadedOnce() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        stubFood(cookie);

        final TraceResult result = restaurantReport("peanut", 10L, 10L);

        verify(foodRepository, times(1)).findById(10L);
        assertEquals(new LinkedHashSet<>(Arrays.asList(10L)), result.getIncident().getSuspectFoodIds());
    }

    // Customer report
    private Order reporterOrder(final User reporter, final Food... foods) {
        final Order order = order(1, reporter, NOON, foods);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        return order;
    }

    @Test
    public void testCustomerReportNarrowsToFoodsThatDeclareTheAllergen() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Food salad = food(11, "QUINOA SALAD");
        final User reporter = user("alice", "PEANUTS");
        final Order mine = reporterOrder(reporter, cookie, salad);
        final Order other = order(2, user("bob", "PEANUTS"), NOON.plusMinutes(20), cookie);
        stubFood(cookie, mine, other);

        final TraceResult result = service.reportCustomerReaction("alice", 1L, "peanut");

        assertEquals(new LinkedHashSet<>(Arrays.asList(10L)), result.getIncident().getSuspectFoodIds());
        verify(orderRepository, never()).findOrdersContainingFood(salad);
        assertEquals(Arrays.asList("bob"), result.getNotifiedUsernames());
    }

    @Test
    public void testCustomerReportWithUndeclaredAllergenTreatsEveryFoodAsSuspect() {
        final Food soup = food(10, "SOUP", "MILK");
        final Food salad = food(11, "QUINOA SALAD");
        final User reporter = user("alice", "PEANUTS");
        final Order mine = reporterOrder(reporter, soup, salad);
        final Order soupOrder = order(2, user("bob", "PEANUTS"), NOON.plusMinutes(5), soup);
        final Order saladOrder = order(3, user("carol", "PEANUTS"), NOON.plusMinutes(6), salad);
        stubFood(soup, mine, soupOrder);
        stubFood(salad, mine, saladOrder);

        final TraceResult result = service.reportCustomerReaction("alice", 1L, "peanut");

        assertEquals(new LinkedHashSet<>(Arrays.asList(10L, 11L)), result.getIncident().getSuspectFoodIds());
        assertEquals(Arrays.asList("bob", "carol"), result.getNotifiedUsernames());
    }

    @Test
    public void testCustomerReportSavesIncidentAsCustomerSource() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Order mine = reporterOrder(user("alice", "PEANUTS"), cookie);
        stubFood(cookie, mine);

        final TraceResult result = service.reportCustomerReaction("alice", 1L, "Peanuts");
        final Incident incident = result.getIncident();

        assertEquals(IncidentSource.CUSTOMER, incident.getSource());
        assertEquals("PEANUT", incident.getAllergen());
        assertEquals("alice", incident.getReporterUsername());
        assertEquals(1L, incident.getSourceOrderId());
        assertEquals(NOON.minusHours(2), incident.getWindowStart());
        assertEquals(NOON.plusHours(2), incident.getWindowEnd());
    }

    @Test
    public void testReporterIsNotNotifiedButTheirOrderIsStillCounted() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Order mine = reporterOrder(user("alice", "PEANUTS"), cookie);
        stubFood(cookie, mine);

        final TraceResult result = service.reportCustomerReaction("alice", 1L, "peanut");

        assertEquals(Arrays.asList(1L), result.getExposedOrderIds());
        assertTrue(result.getNotifications().isEmpty());
    }

    @Test
    public void testCustomerWindowIsTwoHoursEachSideByDefault() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Order mine = reporterOrder(user("alice", "PEANUTS"), cookie);
        final Order earlyEdge = order(2, user("bob", "PEANUTS"), NOON.minusHours(2), cookie);
        final Order lateEdge = order(3, user("carol", "PEANUTS"), NOON.plusHours(2), cookie);
        final Order tooEarly = order(4, user("dave", "PEANUTS"), NOON.minusHours(2).minusMinutes(1), cookie);
        final Order tooLate = order(5, user("erin", "PEANUTS"), NOON.plusHours(2).plusMinutes(1), cookie);
        stubFood(cookie, mine, earlyEdge, lateEdge, tooEarly, tooLate);

        final TraceResult result = service.reportCustomerReaction("alice", 1L, "peanut");

        assertEquals(Arrays.asList("bob", "carol"), result.getNotifiedUsernames());
    }

    @Test
    public void testCustomWindowHoursAreUsed() {
        final TraceServiceImpl oneHour = new TraceServiceImpl(orderRepository, foodRepository, incidentRepository,
                notificationRepository, 1);
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Order mine = reporterOrder(user("alice", "PEANUTS"), cookie);
        final Order inside = order(2, user("bob", "PEANUTS"), NOON.plusMinutes(59), cookie);
        final Order outside = order(3, user("carol", "PEANUTS"), NOON.plusMinutes(61), cookie);
        stubFood(cookie, mine, inside, outside);

        final TraceResult result = oneHour.reportCustomerReaction("alice", 1L, "peanut");

        assertEquals(NOON.minusHours(1), result.getIncident().getWindowStart());
        assertEquals(NOON.plusHours(1), result.getIncident().getWindowEnd());
        assertEquals(Arrays.asList("bob"), result.getNotifiedUsernames());
    }

    @Test
    public void testZeroWindowHoursOnlyMatchesTheSameInstant() {
        final TraceServiceImpl zero = new TraceServiceImpl(orderRepository, foodRepository, incidentRepository,
                notificationRepository, 0);
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Order mine = reporterOrder(user("alice", "PEANUTS"), cookie);
        final Order same = order(2, user("bob", "PEANUTS"), NOON, cookie);
        final Order later = order(3, user("carol", "PEANUTS"), NOON.plusMinutes(1), cookie);
        stubFood(cookie, mine, same, later);

        final TraceResult result = zero.reportCustomerReaction("alice", 1L, "peanut");

        assertEquals(Arrays.asList("bob"), result.getNotifiedUsernames());
    }

    @Test
    public void testNegativeWindowHoursAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TraceServiceImpl(orderRepository, foodRepository,
                incidentRepository, notificationRepository, -1));
    }

    @Test
    public void testCustomerReportRejectsUnknownOrder() {
        when(orderRepository.findById(77L)).thenReturn(Optional.empty());

        final ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.reportCustomerReaction("alice", 77L, "peanut"));

        assertEquals("Order does not exist with id 77", ex.getMessage());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    public void testCustomerCannotReportOnSomeoneElsesOrder() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        reporterOrder(user("bob", "PEANUTS"), cookie);

        assertThrows(AccessDeniedException.class, () -> service.reportCustomerReaction("alice", 1L, "peanut"));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    public void testOrderWithNoOwnerCannotBeReported() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        reporterOrder(null, cookie);

        assertThrows(AccessDeniedException.class, () -> service.reportCustomerReaction("alice", 1L, "peanut"));
    }

    @Test
    public void testOrderWithNoRecordedTimeCannotBeTraced() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Order legacy = order(1, user("alice", "PEANUTS"), null, cookie);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(legacy));

        assertThrows(IllegalArgumentException.class, () -> service.reportCustomerReaction("alice", 1L, "peanut"));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    public void testOrderWithNoFoodsCannotBeTraced() {
        reporterOrder(user("alice", "PEANUTS"));

        assertThrows(IllegalArgumentException.class, () -> service.reportCustomerReaction("alice", 1L, "peanut"));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    public void testCustomerReportRejectsBadArguments() {
        assertThrows(IllegalArgumentException.class, () -> service.reportCustomerReaction(null, 1L, "peanut"));
        assertThrows(IllegalArgumentException.class, () -> service.reportCustomerReaction("alice", null, "peanut"));
        assertThrows(IllegalArgumentException.class, () -> service.reportCustomerReaction("alice", 1L, null));
        assertThrows(IllegalArgumentException.class, () -> service.reportCustomerReaction("alice", 1L, "  "));
        verify(incidentRepository, never()).save(any());
    }

    // TraceResult
    @Test
    public void testResultListsAreCopiesThatCannotBeChanged() {
        final List<Long> ids = new ArrayList<>(Arrays.asList(1L));
        final TraceResult result = new TraceResult(new Incident(), ids, new ArrayList<Notification>());

        ids.add(2L);

        assertEquals(Arrays.asList(1L), result.getExposedOrderIds());
        assertThrows(UnsupportedOperationException.class, () -> result.getExposedOrderIds().add(3L));
        assertThrows(UnsupportedOperationException.class, () -> result.getNotifications().add(new Notification()));
    }

    // Counts stored on the incident
    @Test
    public void testIncidentRecordsExposedAndNotifiedCounts() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        final Order aliceOrder = order(1, user("alice", "PEANUTS"), NOON, cookie);
        final Order bobOrder = order(2, user("bob", "MILK"), NOON, cookie);
        stubFood(cookie, aliceOrder, bobOrder);

        final TraceResult result = restaurantReport("peanut", 10L);

        assertEquals(Integer.valueOf(2), result.getIncident().getExposedOrderCount());
        assertEquals(Integer.valueOf(1), result.getIncident().getNotifiedCount());
    }

    @Test
    public void testIncidentWithNoExposureRecordsZeroCounts() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        stubFood(cookie);

        final TraceResult result = restaurantReport("peanut", 10L);

        assertEquals(Integer.valueOf(0), result.getIncident().getExposedOrderCount());
        assertEquals(Integer.valueOf(0), result.getIncident().getNotifiedCount());
    }

    // listIncidents and describe
    private Incident storedIncident(final long id) {
        final Incident incident = new Incident();
        incident.setId(id);
        incident.setSource(IncidentSource.CUSTOMER);
        incident.setAllergen("PEANUT");
        incident.setReporterUsername("alice");
        incident.setSourceOrderId(5L);
        incident.setSuspectFoodIds(new LinkedHashSet<>(Arrays.asList(10L, 11L)));
        incident.setWindowStart(NOON.minusHours(2));
        incident.setWindowEnd(NOON.plusHours(2));
        incident.setReportedAt(NOON);
        incident.setExposedOrderCount(4);
        incident.setNotifiedCount(2);
        return incident;
    }

    @Test
    public void testDescribeCopiesFieldsAndNamesFoods() {
        final Food cookie = food(10, "COOKIES", "PEANUTS");
        when(foodRepository.findById(10L)).thenReturn(Optional.of(cookie));
        when(foodRepository.findById(11L)).thenReturn(Optional.empty());

        final IncidentDto dto = service.describe(storedIncident(3L));

        assertEquals(3L, dto.id());
        assertEquals("CUSTOMER", dto.source());
        assertEquals("PEANUT", dto.allergen());
        assertEquals(5L, dto.sourceOrderId());
        assertEquals(Arrays.asList(10L, 11L), dto.suspectFoodIds());
        assertEquals(Arrays.asList("COOKIES", "Food #11 (removed)"), dto.suspectFoodNames());
        assertEquals(NOON.minusHours(2), dto.windowStart());
        assertEquals(NOON.plusHours(2), dto.windowEnd());
        assertEquals(NOON, dto.reportedAt());
        assertEquals(Integer.valueOf(4), dto.exposedOrderCount());
        assertEquals(Integer.valueOf(2), dto.notifiedCount());
        assertNull(dto.restaurantAcknowledgedAt());
        assertNull(dto.remedy());
    }

    @Test
    public void testDescribeHandlesMissingSourceAndFoods() {
        final Incident bare = new Incident();
        bare.setId(9L);
        bare.setSource(null);
        bare.setSuspectFoodIds(null);

        final IncidentDto dto = service.describe(bare);

        assertNull(dto.source());
        assertTrue(dto.suspectFoodIds().isEmpty());
        assertTrue(dto.suspectFoodNames().isEmpty());
    }

    @Test
    public void testListIncidentsKeepsRepositoryOrder() {
        when(incidentRepository.findAllByOrderByReportedAtDesc())
                .thenReturn(Arrays.asList(storedIncident(2L), storedIncident(1L)));

        final List<IncidentDto> list = service.listIncidents();

        assertEquals(2, list.size());
        assertEquals(2L, list.get(0).id());
        assertEquals(1L, list.get(1).id());
    }

    @Test
    public void testListIncidentsWhenThereAreNone() {
        when(incidentRepository.findAllByOrderByReportedAtDesc()).thenReturn(new ArrayList<Incident>());

        assertTrue(service.listIncidents().isEmpty());
    }

    // acknowledgeIncident
    @Test
    public void testFirstAcknowledgementSetsTimeStaffAndRemedy() {
        final Incident incident = storedIncident(3L);
        when(incidentRepository.findById(3L)).thenReturn(Optional.of(incident));

        final IncidentDto dto = service.acknowledgeIncident(3L, "staff1", "  Full refund or a free remake  ");

        assertNotNull(dto.restaurantAcknowledgedAt());
        assertEquals("staff1", dto.acknowledgedBy());
        assertEquals("Full refund or a free remake", dto.remedy());
        verify(incidentRepository).save(incident);
    }

    @Test
    public void testSecondAcknowledgementKeepsFirstTimeButUpdatesRemedy() {
        final Incident incident = storedIncident(3L);
        final LocalDateTime firstTime = NOON.plusMinutes(10);
        incident.setRestaurantAcknowledgedAt(firstTime);
        incident.setAcknowledgedBy("staff1");
        incident.setRemedy("Refund");
        when(incidentRepository.findById(3L)).thenReturn(Optional.of(incident));

        final IncidentDto dto = service.acknowledgeIncident(3L, "staff2", "Refund and a free dessert");

        assertEquals(firstTime, dto.restaurantAcknowledgedAt());
        assertEquals("staff1", dto.acknowledgedBy());
        assertEquals("Refund and a free dessert", dto.remedy());
    }

    @Test
    public void testAcknowledgeRejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> service.acknowledgeIncident(3L, null, "Refund"));
        assertThrows(IllegalArgumentException.class, () -> service.acknowledgeIncident(3L, " ", "Refund"));
        assertThrows(IllegalArgumentException.class, () -> service.acknowledgeIncident(null, "staff1", "Refund"));
        assertThrows(IllegalArgumentException.class, () -> service.acknowledgeIncident(3L, "staff1", null));
        assertThrows(IllegalArgumentException.class, () -> service.acknowledgeIncident(3L, "staff1", "   "));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    public void testAcknowledgeRemedyLengthLimit() {
        final Incident incident = storedIncident(3L);
        when(incidentRepository.findById(3L)).thenReturn(Optional.of(incident));
        final StringBuilder exactly500 = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            exactly500.append('r');
        }

        final IncidentDto dto = service.acknowledgeIncident(3L, "staff1", exactly500.toString());
        assertEquals(500, dto.remedy().length());

        final String tooLong = exactly500.toString() + "r";
        assertThrows(IllegalArgumentException.class, () -> service.acknowledgeIncident(3L, "staff1", tooLong));
    }

    @Test
    public void testAcknowledgeUnknownIncident() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        final ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.acknowledgeIncident(99L, "staff1", "Refund"));

        assertEquals("Incident does not exist with id 99", ex.getMessage());
    }
}
