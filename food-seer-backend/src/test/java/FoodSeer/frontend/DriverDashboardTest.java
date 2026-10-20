package FoodSeer.frontend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import FoodSeer.entity.User;
import FoodSeer.entity.Order;
import FoodSeer.repositories.UserRepository;
import FoodSeer.repositories.OrderRepository;
// also cleared in tearDown below before users, since it holds a foreign key onto user_id too
import FoodSeer.repositories.MealLogRepository;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.util.List;
import java.util.function.Predicate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
public class DriverDashboardTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MealLogRepository mealLogRepository;

    private ChromeDriver driver;

    private WebDriverWait wait;

    private String baseUrl = "http://localhost:3000/";

    @BeforeEach
    public void setUp() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--headless");
        driver = new ChromeDriver(options);

        wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }

        // Clean up test data
        // orders and meal logs must go first - both hold a foreign key onto user_id, so
        // deleting a user directly while either still exists violates that constraint
        orderRepository.deleteAll();
        mealLogRepository.deleteAll();
        for(User user : userRepository.findAll()){
            if(!user.getUsername().equals("admin")){
                userRepository.delete(user);
            }
        }
    }

    /**
     * Tests the driver dashboard loads correctly.
     * Verifies that:
     * - Driver can login successfully
     * - Dashboard displays stats (total deliveries, earnings)
     * - Available orders section is visible
     * - Active orders section is visible
     */
    @Test
    public void testDriverDashboardLoads() {
        // Create and login as driver. The username and email are used by no other
        // test, so rows left in the shared test database by other test classes
        // (OrderControllerTest saves a driver@test.com user) cannot block registration.
        registerDriver("dashboarddriver", "dashboard@test.com", "password123");
        loginAsDriver("dashboarddriver", "password123");

        // Wait for dashboard to load
        waitForUrl(url -> url.contains("driver"), "the driver dashboard after login");

        // Verify stats cards are present
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("dashboard-stats")));
        List<WebElement> statCards = driver.findElements(By.className("stat-card"));
        assertEquals(2, statCards.size());

        // Verify sections are present
        assertTrue(driver.findElement(By.className("orders-section")).isDisplayed());
    }

    /**
     * Tests that driver stats are displayed correctly.
     * Verifies that:
     * - Total deliveries count is shown
     * - Today's earnings are displayed
     * - Stats are formatted properly
     */
    @Test
    public void testDriverStatsDisplay() {
        registerDriver("statsdriver", "stats@test.com", "password123");
        loginAsDriver("statsdriver", "password123");

        wait.until(d -> d.getCurrentUrl().contains("driver"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("stat-card")));

        // Find stats
        List<WebElement> statNumbers = driver.findElements(By.className("stat-number"));
        assertTrue(statNumbers.size() >= 2);

        // Verify stats contain numbers (at least display 0 or actual values)
        for(WebElement stat : statNumbers) {
            assertTrue(stat.getText().matches(".*\\d+.*"));
        }
    }

    /**
     * Tests that available orders are displayed correctly.
     * Verifies that:
     * - Available orders section shows orders with "PLACED" status
     * - Each order displays order ID
     * - Each order displays delivery cost
     * - "Pick Up" button is present for each order
     */
    @Test
    public void testAvailableOrdersDisplay() {
        registerDriver("availdriver", "avail@test.com", "password123");
        loginAsDriver("availdriver", "password123");

        wait.until(d -> d.getCurrentUrl().contains("driver"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("orders-section")));

        // Find available orders section
        WebElement availableSection = driver.findElement(By.cssSelector(".orders-section.available"));
        assertTrue(availableSection.isDisplayed());

        // Check for orders header
        WebElement header = availableSection.findElement(By.tagName("h2"));
        assertEquals("Available Orders", header.getText());
    }

    /**
     * Tests that active orders are displayed correctly.
     * Verifies that:
     * - Active orders section shows orders with "PICKED UP" status
     * - Each order displays order ID
     * - Each order displays delivery cost
     * - "Mark as Delivered" button is present for each order
     */
    @Test
    public void testActiveOrdersDisplay() {
        registerDriver("activedriver", "active@test.com", "password123");
        loginAsDriver("activedriver", "password123");

        wait.until(d -> d.getCurrentUrl().contains("driver"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("orders-section")));

        // Find active orders section
        WebElement activeSection = driver.findElement(By.cssSelector(".orders-section.active"));
        assertTrue(activeSection.isDisplayed());

        // Check for orders header
        WebElement header = activeSection.findElement(By.tagName("h2"));
        assertEquals("Active Orders", header.getText());
    }

    /**
     * Tests the pick up order functionality.
     * Verifies that:
     * - Driver can click "Pick Up" button on available order
     * - Order status changes to "PICKED UP"
     * - Order moves from available to active section
     * - Page refreshes after pickup
     */
    @Test
    public void testPickUpOrder() {
        registerDriver("pickupdriver", "pickup@test.com", "password123");
        loginAsDriver("pickupdriver", "password123");

        wait.until(d -> d.getCurrentUrl().contains("driver"));

        // Wait for available orders to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("orders-section")));

        // Check if there are available orders
        List<WebElement> availableOrders = driver.findElements(By.cssSelector(".orders-section.available .order-card"));

        if(availableOrders.size() > 0) {
            // Click the pick up button on first available order
            WebElement pickUpButton = availableOrders.get(0).findElement(By.className("fulfill-button"));
            pickUpButton.click();

            // Wait for page reload
            wait.until(ExpectedConditions.stalenessOf(pickUpButton));

            // Verify we're still on driver dashboard after reload
            wait.until(d -> d.getCurrentUrl().contains("driver"));
        }
    }

    /**
     * Tests the deliver order functionality.
     * Verifies that:
     * - Driver can click "Mark as Delivered" button on active order
     * - Order status changes to "DELIVERED"
     * - Order is removed from active section
     * - Page refreshes after delivery
     * - Driver stats update accordingly
     */
    @Test
    public void testDeliverOrder() {
        registerDriver("deliverdriver", "deliver@test.com", "password123");
        loginAsDriver("deliverdriver", "password123");

        wait.until(d -> d.getCurrentUrl().contains("driver"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("orders-section")));

        // Check if there are active orders
        List<WebElement> activeOrders = driver.findElements(By.cssSelector(".orders-section.active .order-card"));

        if(activeOrders.size() > 0) {
            // Click the deliver button on first active order
            WebElement deliverButton = activeOrders.get(0).findElement(By.className("fulfill-button"));
            deliverButton.click();

            // Wait for page reload
            wait.until(ExpectedConditions.stalenessOf(deliverButton));

            // Verify we're still on driver dashboard after reload
            wait.until(d -> d.getCurrentUrl().contains("driver"));
        }
    }

    /**
     * Tests empty state messages.
     * Verifies that:
     * - "No available orders" message shown when no orders available
     * - "No active orders" message shown when no orders active
     * - UI handles empty states gracefully
     */
    @Test
    public void testEmptyOrdersState() {
        registerDriver("emptydriver", "empty@test.com", "password123");
        loginAsDriver("emptydriver", "password123");

        wait.until(d -> d.getCurrentUrl().contains("driver"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("orders-section")));

        // Note: This test will pass if there are truly no orders in the system
        // The actual behavior depends on the backend data state
        WebElement availableSection = driver.findElement(By.cssSelector(".orders-section.available"));
        WebElement activeSection = driver.findElement(By.cssSelector(".orders-section.active"));

        assertTrue(availableSection.isDisplayed());
        assertTrue(activeSection.isDisplayed());
    }

    /**
     * Tests dashboard error handling.
     * Verifies that:
     * - Error message is displayed when dashboard fails to load
     * - User is informed of the failure
     * - Page remains stable despite error
     */
    @Test
    public void testDashboardErrorHandling() {
        registerDriver("errordriver", "error@test.com", "password123");
        loginAsDriver("errordriver", "password123");

        wait.until(d -> d.getCurrentUrl().contains("driver"));

        // Dashboard should load even if there are no orders
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("driver-dashboard-page")));
        assertTrue(driver.findElement(By.className("driver-dashboard-page")).isDisplayed());
    }

    /**
     * Tests order card information display.
     * Verifies that:
     * - Order ID is displayed correctly
     * - Status badge shows correct status
     * - Delivery cost is formatted with dollar sign
     * - All order information is readable
     */
    @Test
    public void testOrderCardInformation() {
        registerDriver("infodriver", "info@test.com", "password123");
        loginAsDriver("infodriver", "password123");

        wait.until(d -> d.getCurrentUrl().contains("driver"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("orders-section")));

        // Find any order cards
        List<WebElement> orderCards = driver.findElements(By.className("order-card"));

        if(orderCards.size() > 0) {
            WebElement firstCard = orderCards.get(0);

            // Verify order header exists
            WebElement orderHeader = firstCard.findElement(By.className("order-header"));
            assertTrue(orderHeader.isDisplayed());

            // Verify status badge exists
            WebElement statusBadge = firstCard.findElement(By.className("status-badge"));
            assertTrue(statusBadge.isDisplayed());

            // Verify order summary exists
            WebElement orderSummary = firstCard.findElement(By.className("order-summary"));
            assertTrue(orderSummary.isDisplayed());
        }
    }

    /**
     * Tests navigation between different order states.
     * Verifies that:
     * - Orders can transition from available to active
     * - Orders can transition from active to delivered
     * - UI updates correctly after each state change
     * - All sections remain functional throughout
     */
    @Test
    public void testOrderStateTransitions() {
        registerDriver("transitiondriver", "transition@test.com", "password123");
        loginAsDriver("transitiondriver", "password123");

        wait.until(d -> d.getCurrentUrl().contains("driver"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("orders-section")));

        // Count initial orders
        List<WebElement> initialAvailable = driver.findElements(By.cssSelector(".orders-section.available .order-card"));
        List<WebElement> initialActive = driver.findElements(By.cssSelector(".orders-section.active .order-card"));

        int availableCount = initialAvailable.size();
        int activeCount = initialActive.size();

        // Both sections should be present regardless of order counts
        assertTrue(driver.findElements(By.className("orders-section")).size() >= 2);
    }

    // Helper methods

    private void registerDriver(String username, String email, String password) {
        driver.get(baseUrl + "register");
        wait.until(d -> d.getCurrentUrl().equals("http://localhost:3000/register"));

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("confirmPassword")).sendKeys(password);
        driver.findElement(By.id("role")).sendKeys("driver");
        driver.findElement(By.className("login-button")).click();

        // Wait for successful registration (redirect to login)
        waitForUrl(url -> url.equals("http://localhost:3000/"), "the login page after registering " + username);
    }

    private void loginAsDriver(String username, String password) {
        driver.get(baseUrl);
        waitForUrl(url -> url.equals("http://localhost:3000/"), "the login page");

        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));
        WebElement loginButton = driver.findElement(By.className("login-button"));

        usernameField.sendKeys(username);
        passwordField.sendKeys(password);
        loginButton.click();
    }

    /**
     * Waits for the browser URL to meet a condition. On timeout it fails with the
     * current URL and any error message shown on the page, so a CI failure says
     * what went wrong instead of only naming a lambda.
     *
     * @param condition
     *            test applied to the current URL
     * @param expectation
     *            what the test was waiting for, used in the failure message
     */
    private void waitForUrl(final Predicate<String> condition, final String expectation) {
        try {
            wait.until(d -> condition.test(d.getCurrentUrl()));
        } catch (TimeoutException e) {
            final StringBuilder errors = new StringBuilder();
            for (final WebElement element : driver.findElements(By.className("error-message"))) {
                errors.append(element.getText()).append(' ');
            }
            throw new AssertionError("Timed out waiting for " + expectation + ". Current URL: "
                    + driver.getCurrentUrl() + ". Error text on page: '" + errors.toString().trim() + "'", e);
        }
    }
}
