package FoodSeer.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import FoodSeer.entity.Food;
import FoodSeer.entity.Incident;
import FoodSeer.entity.Order;
import FoodSeer.entity.User;
import FoodSeer.repositories.FoodRepository;
import FoodSeer.repositories.IncidentRepository;
import FoodSeer.repositories.NotificationRepository;
import FoodSeer.repositories.OrderRepository;
import FoodSeer.service.AllergenMatcher;
import FoodSeer.service.TraceResult;
import FoodSeer.service.impl.TraceServiceImpl;

/**
 * M0, the risk-killer eval for the allergy trace-back feature.
 *
 * <p>
 * <b>Claim.</b> When an allergy incident is reported, FoodSeer warns the
 * customers who were really exposed (alert recall of at least 0.95) without
 * flooding everyone else (alert precision of at least 0.70). Baselines: the
 * exact string matching FoodSeer used before (found in Project 1a), alerting
 * every allergic customer who ordered that day, and alerting everyone who
 * ordered the dish without checking allergies.
 * </p>
 *
 * <p>
 * <b>Simulation.</b> There are no real users yet, so each seed builds one
 * simulated day: the real FoodSeer seed menu, 2000 customers with true
 * allergies, and 4000 orders between 08:00 and 22:00. Customers mostly avoid
 * dishes that declare their allergens, as real allergic customers do. 40% of
 * customers typed their restrictions as free text (for example "no dairy
 * products" or "soy sauce") instead of using the checkboxes. Some of those
 * phrasings are outside what the matcher knows, so recall can fall short.
 * </p>
 *
 * <p>
 * Each incident hides an undeclared allergen in one dish during a true
 * window of 30 to 180 minutes. Half are reported by the restaurant, whose
 * window guess is off by 20 minutes too narrow to 40 minutes too wide on each
 * side and which names one innocent extra dish 25% of the time. The other
 * half are reported by an allergic customer who ate the dish, which the
 * service traces with its own window of 2 hours either side of that order.
 * Ground truth is who really ate the dish in the true window and really has
 * the allergy. The service never sees the truth.
 * </p>
 *
 * <p>
 * Run it with: mvn -Dtest=M0TraceEval test. The report is written to
 * target/m0-eval/m0-report.md. Every number comes from fixed seeds, so the
 * same command gives the same numbers on any machine.
 * </p>
 *
 * <p>
 * The class name deliberately does not end in "Test", so the normal
 * mvn test run (and its build badge) does not include it. The M0 Eval
 * GitHub workflow runs it on every push and shows the report in the job
 * summary; that workflow fails whenever the claim fails.
 * </p>
 */
public class M0TraceEval {

    /** The claim: alert recall must reach this. */
    static final double RECALL_THRESHOLD = 0.95;

    /** The claim: alert precision must reach this. */
    static final double PRECISION_THRESHOLD = 0.70;

    static final long[] SEEDS = {1L, 2L, 3L, 4L, 5L};
    static final int INCIDENTS_PER_SEED = 100;
    static final int CUSTOMERS = 2000;
    static final int ORDERS_PER_DAY = 4000;
    static final double FREE_TEXT_SHARE = 0.40;
    static final double AVOID_DECLARED_ALLERGEN = 0.95;
    static final double EXTRA_SUSPECT_DISH = 0.25;
    static final LocalDateTime DAY_START = LocalDateTime.of(2026, 10, 12, 8, 0);
    static final int DAY_MINUTES = 14 * 60;

    /** The allergens in the simulation, in canonical form. */
    static final String[] ALLERGENS = {"PEANUT", "TREE NUT", "MILK", "EGG", "FISH", "SHELLFISH", "WHEAT", "SOY",
        "SESAME"};

    /**
     * Share of simulated customers with each allergen. Higher than real life,
     * so every incident has enough exposed customers to measure.
     */
    static final double[] PREVALENCE = {0.12, 0.08, 0.12, 0.06, 0.05, 0.06, 0.05, 0.05, 0.04};

    /** What the preferences page saves for each allergen. */
    static final String[] CHECKBOX_VALUE = {"PEANUTS", "TREE-NUTS", "MILK", "EGGS", "FISH", "SHELLFISH", "WHEAT",
        "SOY", "SESAME"};

    /**
     * Ways a person might type each allergen as free text. Written to sound
     * like people, not to suit the matcher; several will not match.
     */
    static final String[][] FREE_TEXT = {
        {"peanut", "Peanuts", "peanut allergy", "allergic to peanuts", "groundnuts", "peanut butter"},
        {"tree nuts", "Tree-Nut", "treenuts", "almonds and cashews", "walnuts"},
        {"dairy", "milk", "lactose", "no dairy products", "milk protein"},
        {"egg", "eggs", "egg whites"},
        {"fish", "finned fish", "salmon and tuna"},
        {"shellfish", "shrimp", "crustaceans", "shell fish"},
        {"wheat", "wheat flour", "gluten"},
        {"soy", "soya", "soybeans", "soy sauce"},
        {"sesame", "sesame seeds", "tahini"}
    };

    /** How staff or a customer might type the allergen in a report. */
    static final String[][] REPORT_TEXT = {
        {"peanut", "Peanuts", "PEANUT"},
        {"tree nuts", "Tree Nut", "TREE-NUTS"},
        {"milk", "dairy", "MILK"},
        {"egg", "Eggs", "EGG"},
        {"fish", "Fish", "FISH"},
        {"shellfish", "Shellfish", "SHELLFISH"},
        {"wheat", "Wheat", "WHEAT"},
        {"soy", "Soy", "SOY"},
        {"sesame", "Sesame", "SESAME"}
    };

    /** The FoodSeer seed menu from DataInitializer: name, then allergen tags. */
    static final String[][] MENU = {
        {"COFFEE", "CAFFEINE"},
        {"TEA", "CAFFEINE"},
        {"BAGEL", "GLUTEN", "WHEAT", "SESAME"},
        {"BANANA"},
        {"APPLE"},
        {"ORANGE JUICE"},
        {"YOGURT", "MILK", "DAIRY", "LACTOSE"},
        {"GRANOLA BAR", "TREE-NUTS", "PEANUTS", "GLUTEN", "WHEAT", "SOY"},
        {"TURKEY SANDWICH", "GLUTEN", "WHEAT", "MEAT", "POULTRY", "DAIRY", "EGGS", "MUSTARD"},
        {"GARDEN SALAD"},
        {"CAESAR SALAD", "MILK", "DAIRY", "EGGS", "FISH", "GLUTEN", "WHEAT"},
        {"PASTA", "GLUTEN", "WHEAT", "EGGS"},
        {"PIZZA SLICE", "GLUTEN", "WHEAT", "MILK", "DAIRY", "LACTOSE"},
        {"BURRITO", "GLUTEN", "WHEAT", "MILK", "DAIRY", "LACTOSE", "MEAT", "BEEF", "SOY"},
        {"VEGETABLE SOUP", "SOY"},
        {"CHICKEN NOODLE SOUP", "GLUTEN", "WHEAT", "MEAT", "POULTRY", "EGGS"},
        {"SUSHI ROLL", "FISH", "SOY", "SESAME", "EGGS"},
        {"CHICKEN WRAP", "GLUTEN", "WHEAT", "MEAT", "POULTRY", "MILK", "DAIRY"},
        {"STEAK", "MEAT", "BEEF"},
        {"SALMON", "FISH"},
        {"LOBSTER", "SHELLFISH"},
        {"SUSHI PLATTER", "FISH", "SHELLFISH", "SOY", "SESAME", "EGGS"},
        {"RIBEYE", "MEAT", "BEEF"},
        {"VEGGIE BURGER", "GLUTEN", "WHEAT", "SOY", "SESAME"},
        {"TOFU BOWL", "SOY", "SESAME"},
        {"QUINOA SALAD"},
        {"HUMMUS WRAP", "GLUTEN", "WHEAT", "SESAME"},
        {"CHOCOLATE CAKE", "GLUTEN", "WHEAT", "MILK", "DAIRY", "LACTOSE", "EGGS", "SOY"},
        {"ICE CREAM", "MILK", "DAIRY", "LACTOSE", "EGGS"},
        {"COOKIES", "GLUTEN", "WHEAT", "MILK", "DAIRY", "EGGS", "SOY", "TREE-NUTS", "PEANUTS"},
        {"FRUIT SALAD"}
    };

    /** The approaches compared. */
    enum Approach {
        TRACE_BACK("FoodSeer trace-back (this feature)"),
        EXACT_MATCH("Same trace, exact string match (old FoodSeer matching)"),
        NO_TRACE("Every allergic customer who ordered that day"),
        NO_ALLERGY_CHECK("Everyone who ordered a suspect dish in the window");

        final String label;

        Approach(final String label) {
            this.label = label;
        }
    }

    /** Running totals for one approach. */
    static final class Tally {
        long truePositives = 0;
        long alertsSent = 0;
        long trulyExposed = 0;

        void add(final Set<String> predicted, final Set<String> truth) {
            alertsSent = alertsSent + predicted.size();
            trulyExposed = trulyExposed + truth.size();
            for (final String username : predicted) {
                if (truth.contains(username)) {
                    truePositives = truePositives + 1;
                }
            }
        }

        double recall() {
            if (trulyExposed == 0) {
                return 1.0;
            }
            return (double) truePositives / (double) trulyExposed;
        }

        double precision() {
            if (alertsSent == 0) {
                return 1.0;
            }
            return (double) truePositives / (double) alertsSent;
        }
    }

    /** A simulated customer: the account the service sees, plus the truth it does not. */
    static final class Customer {
        final User user;
        final Set<String> trueAllergens;

        Customer(final User user, final Set<String> trueAllergens) {
            this.user = user;
            this.trueAllergens = trueAllergens;
        }
    }

    /** Everything one seed produces. */
    static final class EvalResult {
        final Map<Approach, Tally> overall = new LinkedHashMap<>();
        final Tally fromRestaurant = new Tally();
        final Tally fromCustomer = new Tally();
        final Map<Long, Tally> perSeed = new LinkedHashMap<>();
        int incidents = 0;
        int restaurantReports = 0;
        int customerReports = 0;

        EvalResult() {
            for (final Approach approach : Approach.values()) {
                overall.put(approach, new Tally());
            }
        }
    }

    // The eval

    @Test
    public void traceBackMeetsTheM0Claim() throws IOException {
        final EvalResult result = runAllSeeds();
        final String report = buildReport(result);
        final Path reportPath = writeReport(report);
        System.out.println(report);
        System.out.println("M0 report written to " + reportPath.toAbsolutePath());

        final Tally ours = result.overall.get(Approach.TRACE_BACK);
        assertTrue(ours.recall() >= RECALL_THRESHOLD, String.format(Locale.ROOT,
                "M0 claim failed: alert recall %.3f is below %.2f. See %s", ours.recall(), RECALL_THRESHOLD,
                reportPath));
        assertTrue(ours.precision() >= PRECISION_THRESHOLD, String.format(Locale.ROOT,
                "M0 claim failed: alert precision %.3f is below %.2f. See %s", ours.precision(),
                PRECISION_THRESHOLD, reportPath));
        assertTrue(ours.recall() > result.overall.get(Approach.EXACT_MATCH).recall(),
                "Trace-back recall should beat exact string matching.");
        assertTrue(ours.precision() > result.overall.get(Approach.NO_TRACE).precision(),
                "Trace-back precision should beat alerting every allergic customer.");
        assertTrue(ours.precision() > result.overall.get(Approach.NO_ALLERGY_CHECK).precision(),
                "Trace-back precision should beat alerting without an allergy check.");
    }

    @Test
    public void sameSeedGivesSameNumbers() {
        final EvalResult first = new EvalResult();
        runSeed(1L, first);
        final EvalResult second = new EvalResult();
        runSeed(1L, second);

        for (final Approach approach : Approach.values()) {
            assertEquals(first.overall.get(approach).truePositives, second.overall.get(approach).truePositives);
            assertEquals(first.overall.get(approach).alertsSent, second.overall.get(approach).alertsSent);
            assertEquals(first.overall.get(approach).trulyExposed, second.overall.get(approach).trulyExposed);
        }
    }

    @Test
    public void everySeedProducesIncidentsWithExposedCustomers() {
        final EvalResult result = runAllSeeds();

        assertEquals(SEEDS.length * INCIDENTS_PER_SEED, result.incidents);
        assertTrue(result.restaurantReports > 0, "Expected some restaurant reports.");
        assertTrue(result.customerReports > 0, "Expected some customer reports.");
        for (final long seed : SEEDS) {
            assertTrue(result.perSeed.get(seed).trulyExposed > 0, "Seed " + seed + " had nobody exposed.");
        }
    }

    // Simulation

    static EvalResult runAllSeeds() {
        final EvalResult result = new EvalResult();
        for (final long seed : SEEDS) {
            runSeed(seed, result);
        }
        return result;
    }

    static void runSeed(final long seed, final EvalResult result) {
        final Random rng = new Random(seed);
        final List<Food> menu = buildMenu();
        final List<Customer> customers = buildCustomers(rng);
        final List<Order> orders = buildOrders(rng, customers, menu);

        final Map<Long, Food> foodsById = new HashMap<>();
        for (final Food food : menu) {
            foodsById.put(food.getId(), food);
        }
        final Map<Long, Order> ordersById = new HashMap<>();
        final Map<Long, List<Order>> ordersByFoodId = new HashMap<>();
        final Set<String> orderedToday = new HashSet<>();
        for (final Order order : orders) {
            ordersById.put(order.getId(), order);
            orderedToday.add(order.getUser().getUsername());
            final Set<Long> seenFoods = new HashSet<>();
            for (final Food food : order.getFoods()) {
                if (seenFoods.add(food.getId())) {
                    if (!ordersByFoodId.containsKey(food.getId())) {
                        ordersByFoodId.put(food.getId(), new ArrayList<>());
                    }
                    ordersByFoodId.get(food.getId()).add(order);
                }
            }
        }
        final Map<String, Customer> customersByName = new HashMap<>();
        for (final Customer customer : customers) {
            customersByName.put(customer.user.getUsername(), customer);
        }

        final TraceServiceImpl service = buildService(foodsById, ordersById, ordersByFoodId);
        final Tally seedTally = new Tally();

        for (int i = 0; i < INCIDENTS_PER_SEED; i++) {
            runIncident(rng, menu, orders, customers, customersByName, orderedToday, service, result, seedTally);
        }
        result.perSeed.put(seed, seedTally);
    }

    static void runIncident(final Random rng, final List<Food> menu, final List<Order> orders,
            final List<Customer> customers, final Map<String, Customer> customersByName,
            final Set<String> orderedToday, final TraceServiceImpl service, final EvalResult result,
            final Tally seedTally) {

        // Pick a dish and an allergen the dish does not declare (cross-contact).
        Food dish = null;
        int allergenIndex = -1;
        while (dish == null) {
            final Food candidate = menu.get(rng.nextInt(menu.size()));
            final List<Integer> undeclared = new ArrayList<>();
            for (int a = 0; a < ALLERGENS.length; a++) {
                if (!AllergenMatcher.foodDeclaresAllergen(candidate.getAllergies(), ALLERGENS[a])) {
                    undeclared.add(a);
                }
            }
            if (!undeclared.isEmpty()) {
                dish = candidate;
                allergenIndex = undeclared.get(rng.nextInt(undeclared.size()));
            }
        }
        final String allergen = ALLERGENS[allergenIndex];

        // The true contamination window.
        final int startMinute = 60 + rng.nextInt(12 * 60 - 60);
        final int duration = 30 + rng.nextInt(151);
        final LocalDateTime trueStart = DAY_START.plusMinutes(startMinute);
        final LocalDateTime trueEnd = trueStart.plusMinutes(duration);

        final List<Order> trulyExposedOrders = new ArrayList<>();
        for (final Order order : orders) {
            if (order.getFoods().contains(dish) && !order.getCreatedAt().isBefore(trueStart)
                    && !order.getCreatedAt().isAfter(trueEnd)) {
                trulyExposedOrders.add(order);
            }
        }

        // Half the reports come from an allergic customer who ate the dish, when there is one.
        boolean customerReport = rng.nextBoolean();
        Order reporterOrder = null;
        if (customerReport) {
            final List<Order> reactors = new ArrayList<>();
            for (final Order order : trulyExposedOrders) {
                if (customersByName.get(order.getUser().getUsername()).trueAllergens.contains(allergen)) {
                    reactors.add(order);
                }
            }
            if (reactors.isEmpty()) {
                customerReport = false;
            } else {
                reporterOrder = reactors.get(rng.nextInt(reactors.size()));
            }
        }
        final String reporter;
        if (customerReport) {
            reporter = reporterOrder.getUser().getUsername();
        } else {
            reporter = "staff";
        }

        // Ground truth: really ate the dish in the true window and really has the allergy.
        final Set<String> truth = new HashSet<>();
        for (final Order order : trulyExposedOrders) {
            final String username = order.getUser().getUsername();
            if (!username.equals(reporter) && customersByName.get(username).trueAllergens.contains(allergen)) {
                truth.add(username);
            }
        }

        final String[] reportVariants = REPORT_TEXT[allergenIndex];
        final String typedAllergen = reportVariants[rng.nextInt(reportVariants.length)];

        final TraceResult traced;
        if (customerReport) {
            traced = service.reportCustomerReaction(reporter, reporterOrder.getId(), typedAllergen);
            result.customerReports = result.customerReports + 1;
        } else {
            final int padStart = -20 + rng.nextInt(61);
            final int padEnd = -20 + rng.nextInt(61);
            final LocalDateTime reportedStart = trueStart.minusMinutes(padStart);
            LocalDateTime reportedEnd = trueEnd.plusMinutes(padEnd);
            if (reportedEnd.isBefore(reportedStart)) {
                reportedEnd = reportedStart;
            }
            final List<Long> suspectIds = new ArrayList<>();
            suspectIds.add(dish.getId());
            if (rng.nextDouble() < EXTRA_SUSPECT_DISH) {
                Food extra = dish;
                while (extra == dish) {
                    extra = menu.get(rng.nextInt(menu.size()));
                }
                suspectIds.add(extra.getId());
            }
            traced = service.reportRestaurantIncident(reporter, typedAllergen, suspectIds, reportedStart,
                    reportedEnd);
            result.restaurantReports = result.restaurantReports + 1;
        }
        result.incidents = result.incidents + 1;

        // This feature.
        final Set<String> ours = new HashSet<>(traced.getNotifiedUsernames());

        // Owners of the orders the trace found, used by two baselines.
        final Set<String> exposedOwners = new LinkedHashSet<>();
        final Map<Long, Order> ordersById = new HashMap<>();
        for (final Order order : orders) {
            ordersById.put(order.getId(), order);
        }
        for (final Long orderId : traced.getExposedOrderIds()) {
            final String username = ordersById.get(orderId).getUser().getUsername();
            if (!username.equals(reporter)) {
                exposedOwners.add(username);
            }
        }

        // Baseline: same trace, but exact string equality like the old FoodSeer check.
        final Set<String> exactMatch = new HashSet<>();
        for (final String username : exposedOwners) {
            if (exactStringMatch(customersByName.get(username).user.getDietaryRestrictions(), typedAllergen)) {
                exactMatch.add(username);
            }
        }

        // Baseline: every allergic customer who ordered anything that day.
        final Set<String> noTrace = new HashSet<>();
        for (final Customer customer : customers) {
            final String username = customer.user.getUsername();
            if (!username.equals(reporter) && orderedToday.contains(username)
                    && AllergenMatcher.restrictionsAffectedBy(customer.user.getDietaryRestrictions(), typedAllergen)) {
                noTrace.add(username);
            }
        }

        // Baseline: everyone the trace found, without checking allergies.
        final Set<String> noAllergyCheck = new HashSet<>(exposedOwners);

        result.overall.get(Approach.TRACE_BACK).add(ours, truth);
        result.overall.get(Approach.EXACT_MATCH).add(exactMatch, truth);
        result.overall.get(Approach.NO_TRACE).add(noTrace, truth);
        result.overall.get(Approach.NO_ALLERGY_CHECK).add(noAllergyCheck, truth);
        seedTally.add(ours, truth);
        if (customerReport) {
            result.fromCustomer.add(ours, truth);
        } else {
            result.fromRestaurant.add(ours, truth);
        }
    }

    /**
     * The matching FoodSeer used before this feature: split the saved text on
     * commas and compare each piece to the allergen with plain string equality.
     */
    static boolean exactStringMatch(final String restrictions, final String allergen) {
        if (restrictions == null || allergen == null) {
            return false;
        }
        final String wanted = allergen.trim().toLowerCase(Locale.ROOT);
        for (final String piece : restrictions.split(",")) {
            if (piece.trim().toLowerCase(Locale.ROOT).equals(wanted)) {
                return true;
            }
        }
        return false;
    }

    static List<Food> buildMenu() {
        final List<Food> menu = new ArrayList<>();
        for (int i = 0; i < MENU.length; i++) {
            final List<String> tags = new ArrayList<>(Arrays.asList(MENU[i]).subList(1, MENU[i].length));
            final Food food = new Food(MENU[i][0], 100, 10, tags);
            food.setId((long) (i + 1));
            menu.add(food);
        }
        return menu;
    }

    static List<Customer> buildCustomers(final Random rng) {
        final List<Customer> customers = new ArrayList<>();
        for (int c = 0; c < CUSTOMERS; c++) {
            final Set<String> truth = new LinkedHashSet<>();
            for (int a = 0; a < ALLERGENS.length; a++) {
                if (rng.nextDouble() < PREVALENCE[a]) {
                    truth.add(ALLERGENS[a]);
                }
            }
            final boolean freeText = rng.nextDouble() < FREE_TEXT_SHARE;
            final List<String> pieces = new ArrayList<>();
            for (int a = 0; a < ALLERGENS.length; a++) {
                if (truth.contains(ALLERGENS[a])) {
                    if (freeText) {
                        pieces.add(FREE_TEXT[a][rng.nextInt(FREE_TEXT[a].length)]);
                    } else {
                        pieces.add(CHECKBOX_VALUE[a]);
                    }
                }
            }
            final String restrictions;
            if (pieces.isEmpty()) {
                restrictions = null;
            } else {
                restrictions = String.join(", ", pieces);
            }
            final String username = String.format(Locale.ROOT, "cust%03d", c);
            final User user = User.builder().username(username).dietaryRestrictions(restrictions).build();
            customers.add(new Customer(user, truth));
        }
        return customers;
    }

    static List<Order> buildOrders(final Random rng, final List<Customer> customers, final List<Food> menu) {
        final List<Order> orders = new ArrayList<>();
        for (int o = 0; o < ORDERS_PER_DAY; o++) {
            final Customer customer = customers.get(rng.nextInt(customers.size()));
            final int dishCount = 1 + rng.nextInt(3);
            final List<Food> dishes = new ArrayList<>();
            while (dishes.size() < dishCount) {
                final Food dish = menu.get(rng.nextInt(menu.size()));
                if (declaresAny(dish, customer.trueAllergens) && rng.nextDouble() < AVOID_DECLARED_ALLERGEN) {
                    continue;
                }
                if (!dishes.contains(dish)) {
                    dishes.add(dish);
                }
            }
            final Order order = new Order();
            order.setId((long) (o + 1));
            order.setUser(customer.user);
            order.setFoods(dishes);
            order.setCreatedAt(DAY_START.plusMinutes(rng.nextInt(DAY_MINUTES + 1)));
            orders.add(order);
        }
        return orders;
    }

    static boolean declaresAny(final Food dish, final Set<String> allergens) {
        for (final String allergen : allergens) {
            if (AllergenMatcher.foodDeclaresAllergen(dish.getAllergies(), allergen)) {
                return true;
            }
        }
        return false;
    }

    /** The real service, with repositories backed by the simulated day. */
    static TraceServiceImpl buildService(final Map<Long, Food> foodsById, final Map<Long, Order> ordersById,
            final Map<Long, List<Order>> ordersByFoodId) {
        final OrderRepository orderRepository = mock(OrderRepository.class);
        final FoodRepository foodRepository = mock(FoodRepository.class);
        final IncidentRepository incidentRepository = mock(IncidentRepository.class);
        final NotificationRepository notificationRepository = mock(NotificationRepository.class);
        final long[] nextIncidentId = {1L};

        when(orderRepository.findOrdersContainingFood(any(Food.class))).thenAnswer(invocation -> {
            final Food food = invocation.getArgument(0);
            final List<Order> found = ordersByFoodId.get(food.getId());
            if (found == null) {
                return new ArrayList<Order>();
            }
            return found;
        });
        when(orderRepository.findById(any())).thenAnswer(invocation -> {
            final Long id = invocation.getArgument(0);
            return Optional.ofNullable(ordersById.get(id));
        });
        when(foodRepository.findById(any())).thenAnswer(invocation -> {
            final Long id = invocation.getArgument(0);
            return Optional.ofNullable(foodsById.get(id));
        });
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> {
            final Incident incident = invocation.getArgument(0);
            incident.setId(nextIncidentId[0]);
            nextIncidentId[0] = nextIncidentId[0] + 1;
            return incident;
        });
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        return new TraceServiceImpl(orderRepository, foodRepository, incidentRepository, notificationRepository, 2);
    }

    // Report

    static String buildReport(final EvalResult result) {
        final Tally ours = result.overall.get(Approach.TRACE_BACK);
        final boolean recallPass = ours.recall() >= RECALL_THRESHOLD;
        final boolean precisionPass = ours.precision() >= PRECISION_THRESHOLD;
        final StringBuilder out = new StringBuilder();
        out.append("# M0 eval: allergy trace-back\n\n");
        out.append(String.format(Locale.ROOT,
                "Claim: alert recall >= %.2f and alert precision >= %.2f on simulated incidents.%n%n",
                RECALL_THRESHOLD, PRECISION_THRESHOLD));
        out.append(String.format(Locale.ROOT, "Result: recall %.3f (%s), precision %.3f (%s).%n%n", ours.recall(),
                passFail(recallPass), ours.precision(), passFail(precisionPass)));
        out.append(String.format(Locale.ROOT,
                "Incidents: %d (%d restaurant reports, %d customer reports) over seeds %s; %d customers and %d"
                        + " orders per simulated day; %.0f%% of customers typed free text.%n%n",
                result.incidents, result.restaurantReports, result.customerReports, Arrays.toString(SEEDS), CUSTOMERS,
                ORDERS_PER_DAY, FREE_TEXT_SHARE * 100));

        out.append("| Approach | Recall | Precision | Alerts sent | Truly exposed |\n");
        out.append("| :- | -: | -: | -: | -: |\n");
        for (final Approach approach : Approach.values()) {
            final Tally tally = result.overall.get(approach);
            out.append(String.format(Locale.ROOT, "| %s | %.3f | %.3f | %d | %d |%n", approach.label,
                    tally.recall(), tally.precision(), tally.alertsSent, tally.trulyExposed));
        }

        out.append("\nTrace-back by who reported:\n\n");
        out.append("| Reported by | Recall | Precision | Alerts sent | Truly exposed |\n");
        out.append("| :- | -: | -: | -: | -: |\n");
        out.append(row("Restaurant", result.fromRestaurant));
        out.append(row("Customer", result.fromCustomer));

        out.append("\nTrace-back by seed:\n\n");
        out.append("| Seed | Recall | Precision | Alerts sent | Truly exposed |\n");
        out.append("| :- | -: | -: | -: | -: |\n");
        for (final Map.Entry<Long, Tally> entry : result.perSeed.entrySet()) {
            out.append(row(String.valueOf(entry.getKey()), entry.getValue()));
        }

        out.append("\nReproduce with: mvn -Dtest=M0TraceEval test\n");
        return out.toString();
    }

    static String row(final String label, final Tally tally) {
        return String.format(Locale.ROOT, "| %s | %.3f | %.3f | %d | %d |%n", label, tally.recall(),
                tally.precision(), tally.alertsSent, tally.trulyExposed);
    }

    static String passFail(final boolean pass) {
        if (pass) {
            return "PASS";
        }
        return "FAIL";
    }

    static Path writeReport(final String report) throws IOException {
        final Path directory = Paths.get("target", "m0-eval");
        Files.createDirectories(directory);
        final Path file = directory.resolve("m0-report.md");
        Files.write(file, report.getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
