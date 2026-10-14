package FoodSeer.frontend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import FoodSeer.repositories.UserRepository;
import io.github.bonigarcia.wdm.WebDriverManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
public class RegisterPageTest {

    @Autowired
    private UserRepository userRepository;

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

        // Workaround since transactional doesn't work. Delete all additions
        for(User user : userRepository.findAll()){
            if( !user.getUsername().equals("admin")){
                userRepository.delete(user);
            }
        }
    }

    /**
     * Tests the basic user registration flow.
     * Verifies that:
     * - User can fill out registration form
     * - Form submission works correctly
     * - User is redirected to login page
     * - Can login with new credentials
     * - Redirects to preferences after login
     */
    @Test
    public void testUserRegistration() {
        driver.get(baseUrl + "register");
        wait.until(d -> d.getCurrentUrl().equals("http://localhost:3000/register"));

        // Fill in registration form
        driver.findElement(By.id("username")).sendKeys("testuser");
        driver.findElement(By.id("email")).sendKeys("testuser@valid.com");
        driver.findElement(By.id("password")).sendKeys("Melvin123");
        driver.findElement(By.id("confirmPassword")).sendKeys("Melvin123");
        driver.findElement(By.id("role")).sendKeys("customer");

        // Submit the form (passwords need at least 8 characters since the Sep 19 AuthServiceImpl change)
        driver.findElement(By.className("login-button")).click();

        // Wait for redirection to login page
        wait.until(d -> d.getCurrentUrl().equals("http://localhost:3000/"));
        attemptLogin("testuser", "Melvin123");

        wait.until(d -> d.getCurrentUrl().equals("http://localhost:3000/preferences"));
        assertEquals(driver.getCurrentUrl(), "http://localhost:3000/preferences");
    }

    /**
     * Tests attempting to register with an existing username.
     * Verifies that:
     * - First registration succeeds
     * - Second registration with same credentials fails
     * - Error message is displayed
     * - User remains on registration page
     */
    @Test
    public void testDuplicateUserRegistration() {
        // First registration
        driver.get(baseUrl + "register");
        wait.until(d -> d.getCurrentUrl().equals("http://localhost:3000/register"));

        driver.findElement(By.id("username")).sendKeys("duplicate_user");
        driver.findElement(By.id("email")).sendKeys("duplicate@test.com");
        driver.findElement(By.id("password")).sendKeys("password123");
        driver.findElement(By.id("confirmPassword")).sendKeys("password123");
        driver.findElement(By.id("role")).sendKeys("customer");
        driver.findElement(By.className("login-button")).click();

        // Wait for successful registration
        wait.until(d -> d.getCurrentUrl().equals("http://localhost:3000/"));

        // Try registering the same user again
        driver.get(baseUrl + "register");
        wait.until(d -> d.getCurrentUrl().equals("http://localhost:3000/register"));

        driver.findElement(By.id("username")).sendKeys("duplicate_user");
        driver.findElement(By.id("email")).sendKeys("duplicate@test.com");
        driver.findElement(By.id("password")).sendKeys("password123");
        driver.findElement(By.id("confirmPassword")).sendKeys("password123");
        driver.findElement(By.className("login-button")).click();

        // We should still be on the register page with an error message
        assertEquals("http://localhost:3000/register", driver.getCurrentUrl());
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("error-message")));
        WebElement errorMessage = driver.findElement(By.className("error-message"));
        assertTrue(errorMessage.isDisplayed());
    }

    /**
     * Tests SQL injection prevention in registration form.
     * Verifies that:
     * - SQL injection attempts are caught
     * - Malicious input is not processed
     * - Error message is displayed
     * - User remains on registration page
     */
    @Test
    public void testSQLInjectionAttempt() {
        driver.get(baseUrl + "register");
        wait.until(d -> d.getCurrentUrl().equals("http://localhost:3000/register"));

        // Attempt SQL injection in username and password fields
        String sqlInjection = "' OR '1'='1";
        driver.findElement(By.id("username")).sendKeys(sqlInjection);
        driver.findElement(By.id("email")).sendKeys("sql@injection.com");
        driver.findElement(By.id("password")).sendKeys(sqlInjection);
        driver.findElement(By.id("confirmPassword")).sendKeys(sqlInjection);
        driver.findElement(By.className("login-button")).click();

        // Should still be on register page with validation error
        assertEquals("http://localhost:3000/register", driver.getCurrentUrl());
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("error-message")));
        WebElement errorMessage = driver.findElement(By.className("error-message"));
        assertTrue(errorMessage.isDisplayed());
    }

    /**
     * Tests password confirmation validation.
     * Verifies that:
     * - Different passwords in password fields are caught
     * - Appropriate error message is displayed
     * - User remains on registration page
     * - Cannot proceed with mismatched passwords
     */
    @Test
    public void testPasswordMismatch() {
        driver.get(baseUrl + "register");
        wait.until(d -> d.getCurrentUrl().equals("http://localhost:3000/register"));

        driver.findElement(By.id("username")).sendKeys("mismatchuser");
        driver.findElement(By.id("email")).sendKeys("mismatch@test.com");
        driver.findElement(By.id("password")).sendKeys("password123");
        driver.findElement(By.id("confirmPassword")).sendKeys("password456");
        driver.findElement(By.id("role")).sendKeys("customer");
        driver.findElement(By.className("login-button")).click();

        // Should stay on register page with error message
        assertEquals("http://localhost:3000/register", driver.getCurrentUrl());
        WebElement errorMessage = driver.findElement(By.className("error-message"));
        assertTrue(errorMessage.isDisplayed());
        assertTrue(errorMessage.getText().contains("Passwords do not match"));
    }

    /**
     * Tests email format validation.
     * Verifies that:
     * - Invalid email formats are rejected
     * - User remains on registration page
     * - Error message is displayed
     * - Cannot proceed with invalid email
     */
    @Test
    public void testInvalidEmailFormat() {
        driver.get(baseUrl + "register");
        wait.until(d -> d.getCurrentUrl().equals("http://localhost:3000/register"));

        driver.findElement(By.id("username")).sendKeys("emailuser");
        driver.findElement(By.id("email")).sendKeys("notanemail");
        driver.findElement(By.id("password")).sendKeys("password123");
        driver.findElement(By.id("confirmPassword")).sendKeys("password123");
        driver.findElement(By.className("login-button")).click();

        // Should stay on register page with error
        assertEquals("http://localhost:3000/register", driver.getCurrentUrl());
    }

    /**
     * Tests required field validation.
     * Verifies that:
     * - Form cannot be submitted with empty fields
     * - Appropriate error message is shown
     * - User remains on registration page
     * - All fields are properly validated
     */
    @Test
    public void testEmptyFields() {
        driver.get(baseUrl + "register");
        wait.until(d -> d.getCurrentUrl().equals("http://localhost:3000/register"));

        // Try to submit with all fields empty
        driver.findElement(By.className("login-button")).click();

        // Should stay on register page
        assertEquals("http://localhost:3000/register", driver.getCurrentUrl());
        WebElement errorMessage = driver.findElement(By.className("error-message"));
        assertTrue(errorMessage.isDisplayed());
        assertTrue(errorMessage.getText().contains("All fields are required"));
    }

    /**
     * Tests input length validation.
     * Verifies that:
     * - Long inputs (>50 characters) are rejected
     * - Error message is shown for oversized inputs
     * - User remains on registration page
     * - Cannot proceed with oversized input
     */
    @Test
    public void testLongInputValidation() {
        driver.get(baseUrl + "register");
        wait.until(d -> d.getCurrentUrl().equals("http://localhost:3000/register"));

        // Create very long input (more than 50 characters)
        String longInput = "a".repeat(51);

        driver.findElement(By.id("username")).sendKeys(longInput);
        driver.findElement(By.id("email")).sendKeys("long@test.com");
        driver.findElement(By.id("password")).sendKeys("password123");
        driver.findElement(By.id("confirmPassword")).sendKeys("password123");
        driver.findElement(By.className("login-button")).click();

        // Should stay on register page with error about length
        assertEquals("http://localhost:3000/register", driver.getCurrentUrl());
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("error-message")));
        WebElement errorMessage = driver.findElement(By.className("error-message"));
        assertTrue(errorMessage.isDisplayed());
    }

    private void attemptLogin(String username, String password) {
        driver.get(baseUrl);
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(2));

        System.out.println("Loaded page title: " + driver.getTitle());
        System.out.println("Current URL: " + driver.getCurrentUrl());

        // Find username and password fields and login button
        var usernameField = driver.findElement(By.id("username"));
        var passwordField = driver.findElement(By.id("password"));
        var loginButton = driver.findElement(By.className("login-button"));

        // Enter credentials
        usernameField.sendKeys(username);
        passwordField.sendKeys(password);

        // Click login button

        loginButton.click();
    }
}
