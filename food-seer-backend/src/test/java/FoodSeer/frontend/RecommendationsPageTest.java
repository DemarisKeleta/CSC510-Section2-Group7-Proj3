package FoodSeer.frontend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import FoodSeer.entity.User;
import FoodSeer.repositories.FoodRepository;
// cleared below before foods/users, since Order rows hold foreign keys onto both
import FoodSeer.repositories.OrderRepository;
// also cleared before users below, since it holds a foreign key onto user_id too
import FoodSeer.repositories.MealLogRepository;
// the seeded Inventory's foods list is cleared below before foodRepository.deleteAll()
import FoodSeer.entity.Inventory;
import FoodSeer.repositories.InventoryRepository;
import FoodSeer.repositories.UserRepository;
import FoodSeer.service.impl.DataInitializer;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
public class RecommendationsPageTest {

    private ChromeDriver driver;
    private WebDriverWait wait;
    private String baseUrl = "http://localhost:3000/";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MealLogRepository mealLogRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private DataInitializer dataInitializer;

    @BeforeEach
    public void setUp() {
        // Set up ChromeDriver
        // clear anything still referencing foods before deleteAll() below - orders.foods
        // (via orders_foods) and the seeded Inventory's foods (via inventory_foods) would
        // otherwise block it with a foreign-key violation. Each repository call below
        // commits its own transaction (standard Spring Data JPA behavior), which is
        // required here since this test drives the real app over HTTP from a separate
        // thread/transaction, unlike the EntityManager native-query approach used in
        // other @Transactional test classes.
        orderRepository.deleteAll();
        for (final Inventory inventory : inventoryRepository.findAll()) {
            inventory.setFoods(new java.util.ArrayList<>());
            inventoryRepository.save(inventory);
        }
        foodRepository.deleteAll();
        dataInitializer.onApplicationReady();
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--headless");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }

        // Clean up test users
        // orders and meal logs must go first - both hold a foreign key onto user_id, so
        // deleting a user directly while either still exists violates that constraint
        orderRepository.deleteAll();
        mealLogRepository.deleteAll();
        for(User user : userRepository.findAll()) {
            if (!user.getUsername().equals("admin")) {
                userRepository.delete(user);
            }
        }
    }

    private void registerUserWithPreferences(String username, String password, String email, String budget, String... allergens) {
        // Register and navigate through preferences
        driver.get(baseUrl + "register");
        wait.until(d -> d.getCurrentUrl().equals(baseUrl + "register"));

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("confirmPassword")).sendKeys(password);
        driver.findElement(By.id("role")).sendKeys("customer");
        driver.findElement(By.className("login-button")).click();

        wait.until(d -> d.getCurrentUrl().equals(baseUrl));
        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.className("login-button")).click();

        // Set preferences
        wait.until(d -> d.getCurrentUrl().equals(baseUrl + "preferences"));
        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//div[@class='option-card '][.//div[text()='" + budget + "']]")));
        driver.findElement(By.xpath("//div[@class='option-card '][.//div[text()='" + budget + "']]")).click();
        driver.findElement(By.className("next-button")).click();

        // Select allergens
        for (String allergen : allergens) {
            wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[contains(@class, 'allergen-option')][.//span[text()='" + allergen + "']]")));
            driver.findElement(By.xpath("//label[contains(@class, 'allergen-option')][.//span[text()='" + allergen + "']]")).click();
        }
        driver.findElement(By.className("next-button")).click();

        // Wait for recommendations page
        wait.until(d -> d.getCurrentUrl().equals(baseUrl + "recommendations"));
    }

    /**
     * Tests the budget-based filtering of food recommendations.
     * Verifies that:
     * - Only foods under $10 are shown when budget preference is set
     * - Budget items like coffee, tea, and bagels are included
     * - Premium items like steak and lobster are excluded
     * - At least 8 budget items from the initializer are shown
     */
    @Test
    public void testBudgetFiltering() {
        registerUserWithPreferences("budgetuser", "testpass123", "budget@test.com", "Budget ($0-$10)");

        // Verify only budget foods are shown
        wait.until(driver -> {
            List<WebElement> cards = driver.findElements(By.className("recommendation-card"));
            return cards.size() > 0; // wait until cards actually rendered
        });
        List<WebElement> recommendedFoods = driver.findElements(By.className("recommendation-card"));

        // Should show budget items
        assertTrue(recommendedFoods.size() >= 8); // At least 8 budget items from initializer

        // Verify some specific budget items
        String recommendedText = driver.findElement(By.className("recommendations-grid")).getText();
        assertTrue(recommendedText.contains("COFFEE"));
        assertTrue(recommendedText.contains("BAGEL"));
        assertTrue(recommendedText.contains("BANANA"));

        // Verify premium items are not shown
        assertFalse(recommendedText.contains("STEAK"));
        assertFalse(recommendedText.contains("LOBSTER"));
    }

    /**
     * Tests the allergen-based filtering of food recommendations.
     * Verifies that:
     * - Foods containing selected allergens are excluded
     * - Moderate price range ($10-$20) items are filtered correctly
     * - Multiple allergen restrictions work together
     * - Safe foods without allergens are included
     */
    @Test
    public void testDietaryRestrictionFiltering() {
        // allergen checkboxes now render the shared, backend-seeded allergen names
        // (uppercase, "DAIRY"/"MILK" as separate entries) rather than the old
        // hardcoded "Milk/Dairy" combined label
        registerUserWithPreferences("allergyuser", "testpass123", "allergy@test.com",
            "Moderate ($0-$20)", "DAIRY", "FISH");

        // Verify foods with allergens are excluded
        wait.until(driver -> {
            List<WebElement> cards = driver.findElements(By.className("recommendation-card"));
            return cards.size() > 0; // wait until cards actually rendered
        });
        String recommendedText = driver.findElement(By.className("recommendations-grid")).getText();

        // Should show allergen-free items in moderate price range
        assertTrue(recommendedText.contains("GARDEN SALAD")); // No dairy or fish
        assertTrue(recommendedText.contains("FRUIT SALAD")); // Naturally allergen-free

        // Should not show items with allergens
        assertFalse(recommendedText.contains("GRILLED CHEESE")); // Contains dairy
        assertFalse(recommendedText.contains("SALMON")); // Contains fish
        assertFalse(recommendedText.contains("YOGURT PARFAIT")); // Contains dairy
    }


    /**
     * Tests the update preferences functionality from the recommendations page.
     * Verifies that:
     * - The update preferences button is clickable
     * - Clicking redirects to the preferences page
     * - The URL changes correctly
     */
    @Test
    public void testUpdatePreferences() {
        registerUserWithPreferences("updateuser", "testpass123", "update@test.com",
            "Budget ($0-$10)");

        // Click update preferences
        wait.until(ExpectedConditions.elementToBeClickable(By.className("update-preferences-button")));
        driver.findElement(By.className("update-preferences-button")).click();

        // Verify navigation to preferences page
        wait.until(d -> d.getCurrentUrl().equals(baseUrl + "preferences"));
        assertEquals(baseUrl + "preferences", driver.getCurrentUrl());
    }

    /**
     * Tests all navigation buttons on the recommendations page.
     * Verifies that:
     * - Browse All Foods button redirects to inventory page
     * - My Orders button redirects to orders page
     * - Logout button redirects to login page
     * - Back navigation works correctly
     */
    @Test
    public void testNavigationButtons() {
        registerUserWithPreferences("navuser", "testpass123", "nav@test.com",
            "Moderate ($0-$20)");

        // Test browse all foods
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Browse All Foods']")));
        driver.findElement(By.xpath("//button[text()='Browse All Foods']")).click();
        wait.until(d -> d.getCurrentUrl().equals(baseUrl + "inventory"));
        assertEquals(baseUrl + "inventory", driver.getCurrentUrl());

        // Go back and test orders
        driver.navigate().back();
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='My Orders']")));
        driver.findElement(By.xpath("//button[text()='My Orders']")).click();
        wait.until(d -> d.getCurrentUrl().equals(baseUrl + "orders"));
        assertEquals(baseUrl + "orders", driver.getCurrentUrl());

        // Test logout
        driver.navigate().back();
        wait.until(ExpectedConditions.elementToBeClickable(By.className("logout-button")));
        driver.findElement(By.className("logout-button")).click();
        wait.until(d -> d.getCurrentUrl().equals(baseUrl));
        assertEquals(baseUrl, driver.getCurrentUrl());
    }

    /**
     * Tests the user information display panel on recommendations page.
     * Verifies that:
     * - Username is displayed correctly
     * - Budget preference is shown
     * - Allergen restrictions are listed
     * - All user preferences are accurately reflected
     */
    @Test
    public void testUserInfoDisplay() {
        // same rename as above: "PEANUTS"/"TREE-NUTS" match the real seeded allergen names
        registerUserWithPreferences("infouser", "testpass123", "info@test.com",
            "Premium ($0-$35)", "PEANUTS", "TREE-NUTS");

        // Verify user info display
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("user-info-card")));
        WebElement userInfo = driver.findElement(By.className("user-info-card"));

        assertTrue(userInfo.getText().contains("infouser"));
        assertTrue(userInfo.getText().contains("premium"));
        assertTrue(userInfo.getText().contains("PEANUTS"));
        assertTrue(userInfo.getText().contains("TREE-NUTS"));
    }

    /**
     * Tests filtering with multiple allergen restrictions combined.
     * Verifies that:
     * - Multiple allergen restrictions work together
     * - Only foods meeting all criteria are shown
     * - Foods with any selected allergen are excluded
     * - Foods without any selected allergens are included
     */
    @Test
    public void testMultipleDietaryRestrictions() {
        // same rename as testDietaryRestrictionFiltering above
        registerUserWithPreferences("multiuser", "testpass123", "multi@test.com",
            "No Limit", "DAIRY", "EGGS", "SHELLFISH");

        // Verify recommendations respect multiple allergen restrictions
        wait.until(driver -> {
            List<WebElement> cards = driver.findElements(By.className("recommendation-card"));
            return cards.size() > 0; // wait until cards actually rendered
        });
        String recommendedText = driver.findElement(By.className("recommendations-grid")).getText();

        // Verify foods with any of the allergens are excluded
        assertFalse(recommendedText.contains("QUICHE")); // Contains eggs and dairy
        assertFalse(recommendedText.contains("SHRIMP SCAMPI")); // Contains shellfish
        assertFalse(recommendedText.contains("CHEESE PLATE")); // Contains dairy

        // Verify allergen-free foods are included
        assertTrue(recommendedText.contains("GARDEN SALAD")); // No common allergens
        assertTrue(recommendedText.contains("FRUIT SALAD")); // Naturally allergen-free
    }
}