package FoodSeer.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import FoodSeer.entity.User;
import FoodSeer.entity.Role;
import FoodSeer.entity.Food;
import FoodSeer.entity.Allergen;
import FoodSeer.repositories.UserRepository;
import FoodSeer.repositories.RoleRepository;
import FoodSeer.repositories.FoodRepository;
import FoodSeer.repositories.AllergenRepository;

/**
 * Initializes application data such as a default admin user.
 */
@Component
public class DataInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FoodRepository foodRepository;
    private final AllergenRepository allergenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin-user-password:admin}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           FoodRepository foodRepository,
                           AllergenRepository allergenRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.foodRepository = foodRepository;
        this.allergenRepository = allergenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Ensure roles exist
        if (roleRepository.findByName("ROLE_ADMIN") == null) {
            roleRepository.save(new Role(null, "ROLE_ADMIN"));
        }
        if (roleRepository.findByName("ROLE_CUSTOMER") == null) {
            roleRepository.save(new Role(null, "ROLE_CUSTOMER"));
        }
        if (roleRepository.findByName("ROLE_STAFF") == null) {
            roleRepository.save(new Role(null, "ROLE_STAFF"));
        }

        // Ensure admin user exists or update password
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            final String hash = passwordEncoder.encode(adminPassword);
            admin = User.builder()
                    .username("admin")
                    .email("admin@localhost")
                    .password(hash)
                    .role("ROLE_ADMIN")
                    .build();
            userRepository.save(admin);
            System.out.println("Created default admin user 'admin' with password: " + adminPassword);
        } else {
            // Update admin password if it has changed
            final String hash = passwordEncoder.encode(adminPassword);
            admin.setPassword(hash);
            userRepository.save(admin);
            System.out.println("Updated admin user password to: " + adminPassword);
        }

        // Seed the shared allergen list with the original fixed vocabulary, once.
        // Customers can add to this list later via AllergenController.
        if (allergenRepository.count() == 0) {
            final List<String> initialAllergens = Arrays.asList(
                    "MILK", "LACTOSE", "EGGS", "FISH", "SHELLFISH", "TREE-NUTS", "PEANUTS",
                    "WHEAT", "GLUTEN", "SOY", "SESAME", "CORN", "SULFITES", "MUSTARD",
                    "MEAT", "BEEF", "PORK", "POULTRY", "GELATIN", "CAFFEINE", "DAIRY"
            );
            for (final String name : initialAllergens) {
                allergenRepository.save(new Allergen(name));
            }
            System.out.println("Seeded " + initialAllergens.size() + " initial allergens.");
        }

        // Initialize sample food data if database is empty
        if (foodRepository.count() == 0) {
            System.out.println("Database empty - initializing sample food data...");
            
            List<Food> sampleFoods = new ArrayList<>();
            
            /*
             * Comprehensive Allergen List Reference:
             * - MILK/DAIRY: milk, cheese, butter, cream
             * - LACTOSE: lactose intolerance specific
             * - EGGS: egg products
             * - FISH: finned fish
             * - SHELLFISH: crustaceans, mollusks
             * - TREE-NUTS: almonds, walnuts, cashews, etc.
             * - PEANUTS: peanuts specifically
             * - WHEAT: wheat flour
             * - GLUTEN: wheat, barley, rye
             * - SOY: soybean products
             * - SESAME: sesame seeds/oil
             * - CORN: corn products
             * - SULFITES: preservatives in wine, dried fruit
             * - MUSTARD: mustard seeds/products
             * - MEAT: general meat (for vegetarians)
             * - BEEF: beef specifically
             * - PORK: pork specifically
             * - POULTRY: chicken, turkey
             * - GELATIN: animal-derived gelatin
             * - CAFFEINE: caffeinated products
             */
            
            // Budget-friendly options (under $10)
            sampleFoods.add(new Food("COFFEE", 50, 3, Arrays.asList("CAFFEINE")));
            sampleFoods.add(new Food("TEA", 40, 2, Arrays.asList("CAFFEINE")));
            sampleFoods.add(new Food("BAGEL", 30, 4, Arrays.asList("GLUTEN", "WHEAT", "SESAME")));
            sampleFoods.add(new Food("BANANA", 60, 1, new ArrayList<>()));
            sampleFoods.add(new Food("APPLE", 50, 2, new ArrayList<>()));
            sampleFoods.add(new Food("ORANGE JUICE", 25, 5, new ArrayList<>()));
            sampleFoods.add(new Food("YOGURT", 35, 4, Arrays.asList("MILK", "DAIRY", "LACTOSE")));
            sampleFoods.add(new Food("GRANOLA BAR", 45, 3, Arrays.asList("TREE-NUTS", "PEANUTS", "GLUTEN", "WHEAT", "SOY")));
            
            // Mid-range options ($10-$20)
            sampleFoods.add(new Food("TURKEY SANDWICH", 20, 12, Arrays.asList("GLUTEN", "WHEAT", "MEAT", "POULTRY", "DAIRY", "EGGS", "MUSTARD")));
            sampleFoods.add(new Food("GARDEN SALAD", 15, 10, new ArrayList<>())); // Vegan-friendly
            sampleFoods.add(new Food("CAESAR SALAD", 15, 11, Arrays.asList("MILK", "DAIRY", "EGGS", "FISH", "GLUTEN", "WHEAT"))); // Has cheese, anchovies, croutons
            sampleFoods.add(new Food("PASTA", 18, 14, Arrays.asList("GLUTEN", "WHEAT", "EGGS"))); // Most pasta has eggs
            sampleFoods.add(new Food("PIZZA SLICE", 25, 8, Arrays.asList("GLUTEN", "WHEAT", "MILK", "DAIRY", "LACTOSE")));
            sampleFoods.add(new Food("BURRITO", 22, 11, Arrays.asList("GLUTEN", "WHEAT", "MILK", "DAIRY", "LACTOSE", "MEAT", "BEEF", "SOY")));
            sampleFoods.add(new Food("VEGETABLE SOUP", 20, 9, Arrays.asList("SOY"))); // May contain soy broth
            sampleFoods.add(new Food("CHICKEN NOODLE SOUP", 18, 10, Arrays.asList("GLUTEN", "WHEAT", "MEAT", "POULTRY", "EGGS")));
            sampleFoods.add(new Food("SUSHI ROLL", 15, 13, Arrays.asList("FISH", "SOY", "SESAME", "EGGS")));
            sampleFoods.add(new Food("CHICKEN WRAP", 18, 10, Arrays.asList("GLUTEN", "WHEAT", "MEAT", "POULTRY", "MILK", "DAIRY")));
            
            // Premium options (over $20)
            sampleFoods.add(new Food("STEAK", 10, 28, Arrays.asList("MEAT", "BEEF")));
            sampleFoods.add(new Food("SALMON", 12, 24, Arrays.asList("FISH")));
            sampleFoods.add(new Food("LOBSTER", 8, 35, Arrays.asList("SHELLFISH")));
            sampleFoods.add(new Food("SUSHI PLATTER", 10, 32, Arrays.asList("FISH", "SHELLFISH", "SOY", "SESAME", "EGGS")));
            sampleFoods.add(new Food("RIBEYE", 8, 30, Arrays.asList("MEAT", "BEEF")));
            
            // Vegan/Vegetarian options
            sampleFoods.add(new Food("VEGGIE BURGER", 20, 11, Arrays.asList("GLUTEN", "WHEAT", "SOY", "SESAME")));
            sampleFoods.add(new Food("TOFU BOWL", 18, 12, Arrays.asList("SOY", "SESAME")));
            sampleFoods.add(new Food("QUINOA SALAD", 15, 13, new ArrayList<>())); // Fully vegan/allergy-friendly
            sampleFoods.add(new Food("HUMMUS WRAP", 20, 9, Arrays.asList("GLUTEN", "WHEAT", "SESAME")));
            
            // Desserts
            sampleFoods.add(new Food("CHOCOLATE CAKE", 12, 7, Arrays.asList("GLUTEN", "WHEAT", "MILK", "DAIRY", "LACTOSE", "EGGS", "SOY")));
            sampleFoods.add(new Food("ICE CREAM", 25, 6, Arrays.asList("MILK", "DAIRY", "LACTOSE", "EGGS")));
            sampleFoods.add(new Food("COOKIES", 30, 5, Arrays.asList("GLUTEN", "WHEAT", "MILK", "DAIRY", "EGGS", "SOY", "TREE-NUTS", "PEANUTS")));
            sampleFoods.add(new Food("FRUIT SALAD", 20, 8, new ArrayList<>())); // Fully vegan/allergy-friendly
            
            // Calorie values for the Fitness-Based Calorie Tracking use case - without these,
            // every food is excluded from the calorie-budget recommendation (a food with no
            // recorded calorie value can't be judged, so it's never guessed at as zero).
            final java.util.Map<String, Integer> calorieByName = new java.util.HashMap<>();
            calorieByName.put("COFFEE", 5);
            calorieByName.put("TEA", 2);
            calorieByName.put("BAGEL", 250);
            calorieByName.put("BANANA", 105);
            calorieByName.put("APPLE", 95);
            calorieByName.put("ORANGE JUICE", 110);
            calorieByName.put("YOGURT", 150);
            calorieByName.put("GRANOLA BAR", 190);
            calorieByName.put("TURKEY SANDWICH", 450);
            calorieByName.put("GARDEN SALAD", 120);
            calorieByName.put("CAESAR SALAD", 470);
            calorieByName.put("PASTA", 600);
            calorieByName.put("PIZZA SLICE", 285);
            calorieByName.put("BURRITO", 650);
            calorieByName.put("VEGETABLE SOUP", 110);
            calorieByName.put("CHICKEN NOODLE SOUP", 180);
            calorieByName.put("SUSHI ROLL", 255);
            calorieByName.put("CHICKEN WRAP", 450);
            calorieByName.put("STEAK", 679);
            calorieByName.put("SALMON", 412);
            calorieByName.put("LOBSTER", 320);
            calorieByName.put("SUSHI PLATTER", 500);
            calorieByName.put("RIBEYE", 720);
            calorieByName.put("VEGGIE BURGER", 390);
            calorieByName.put("TOFU BOWL", 380);
            calorieByName.put("QUINOA SALAD", 350);
            calorieByName.put("HUMMUS WRAP", 420);
            calorieByName.put("CHOCOLATE CAKE", 450);
            calorieByName.put("ICE CREAM", 270);
            calorieByName.put("COOKIES", 160);
            calorieByName.put("FRUIT SALAD", 130);
            for (final Food food : sampleFoods) {
                food.setCalories(calorieByName.get(food.getFoodName()));
            }

            // Save all sample foods
            foodRepository.saveAll(sampleFoods);
            System.out.println("Successfully created " + sampleFoods.size() + " sample food items with comprehensive allergen information!");
        } else {
            System.out.println("Food database already contains " + foodRepository.count() + " items - skipping sample data creation.");
        }
    }
}
