
# Pantry-Paired Ordering

## Why

Customers ordering restaurant food may also have ingredients at home that they want to use. However, deciding what to make with those ingredients can be difficult, especially when customers are unsure which recipes they can prepare with what they already have and which ingredients they would still need.

FoodSeer connects restaurant entrees with recipes based on ingredients selected from clickable pantry options. Pantry-Paired Ordering helps customers discover relevant recipes, compare ingredient matches, identify missing ingredients, and review preparation instructions before deciding whether to select or skip a suggestion.

## How it works

1. **Select pantry ingredients.** Customers choose ingredients from the available clickable pantry options rather than typing them manually.
2. **Choose an entree.** A customer selects a restaurant entree supported by Pantry-Paired Ordering.
3. **Find recipes.** FoodSeer retrieves recipes associated with the selected entree. The feature includes 16 recipes, with two recipes for each of eight supported entrees.
4. **Match pantry ingredients.** FoodSeer compares the selected pantry ingredients with each recipe's required ingredients and calculates an ingredient-match percentage.
5. **Rank suggestions.** Recipe suggestions are ranked according to their ingredient matches, helping customers identify recipes that make use of what they already have.
6. **Identify missing ingredients.** Each suggestion displays ingredients the customer would still need to prepare the recipe.
7. **Review preparation details.** Customers can review preparation time and cooking instructions before making a decision.
8. **Select or skip.** Customers can select a suggested recipe or skip it and continue with the ordering experience.

## Try it

1. Start FoodSeer and log in as a customer.
2. Navigate to the order-creation page.
3. Select pantry ingredients using the available clickable options.
4. Select a restaurant entree supported by Pantry-Paired Ordering.
5. Review the suggested recipes and compare their ingredient-match percentages.
6. Review each recipe's missing ingredients, preparation time, and cooking instructions.
7. Select a recipe or skip the suggestion, then continue through the ordering experience.

## Code map

| Piece | File |
| :- | :- |
| Pantry-pairing interface and recipe selection | `food-seer-frontend/src/components/PantryPairing.js` |
| Ingredient matching, match percentages, missing ingredients, allergen filtering, and recipe ranking | `food-seer-frontend/src/data/pantryMatcher.js` |
| Pantry-pairing recipe definitions | `food-seer-frontend/src/data/pantryRecipes.js` |
| Unit tests for recipe matching and filtering | `food-seer-frontend/src/data/pantryMatcher.test.js` |
| Integration with the order-creation page | `food-seer-frontend/src/pages/CreateOrder.js` |

## Tests

The pantry-matching test suite covers:

- The number of available recipes.
- Recipe ranking based on pantry ingredient matches.
- Identification of missing ingredients.
- Filtering recipes based on restricted allergens.
- Handling of unsupported restaurant entrees.

The test file is `food-seer-frontend/src/data/pantryMatcher.test.js`.

