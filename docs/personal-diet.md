# Fitness-Based Calorie Tracking

## Why

Calorie-counting apps and food-delivery apps are separate today, so a customer who's tracking their diet has to do the math themselves before ordering: look up how many calories they've burned, subtract what they've already eaten, then guess which menu items still fit. Get it wrong and the app has already nudged them toward an order that blows their budget.

FoodSeer closes that loop. A customer sets a daily calorie goal once; from then on, every menu recommendation is already filtered to what actually fits what's left of their budget today, and to foods free of their saved allergies. The budget updates live as they log meals or place orders, and resets to a clean slate every day with no leftover carryover.

## How it works

1. **Set a goal.** A customer sets a daily calorie goal on the Personal Diet page (optional - leave blank to turn budget filtering off).
2. **Burned.** Today's calories burned come from a per-user fitness-activity file, standing in for a real fitness-app export.
3. **Consumed.** Today's calories consumed are summed from two sources: every food in every order the customer placed today, and any meals they manually log (for food eaten outside FoodSeer).
4. **Compute.** `remaining = goal + burnedToday - consumedToday`, computed only when both a goal and fitness data are available; otherwise the full menu is shown unfiltered rather than guessing.
5. **Recommend.** The menu is filtered server-side to items within the remaining budget, then filtered again client-side to exclude anything matching the customer's saved allergies.
6. **Cart and order.** Customers add items to a cart and can order several at once. If the combined cart total would exceed the remaining budget - even when each item fit on its own - a warning appears before they place the order.
7. **Reset.** Nothing about "today" is stored as a running total; every figure is recomputed from today's date on each request, so a new calendar day is automatically a clean slate.
8. **History.** A History page lists every day the customer has consumption recorded for, most recent first.

## API

| Method and path | Who | What |
| :- | :- | :- |
| `GET /api/users/me/calorieBudget` | Customer | Today's burned/goal/consumed/remaining budget. |
| `POST /api/users/me/mealLog` | Customer | Log (or update) calories for one meal slot today. |
| `GET /api/users/me/calorieHistory` | Customer | Total calories consumed per day, most recent day first. |
| `GET /api/foods/withinCalorieBudget` | Customer | Menu items that fit the remaining budget; full menu if the budget can't be computed. |
| `PUT /api/users/me/preferences` | Customer | Save cost preference, saved allergies, and daily calorie goal. |
| `GET /api/allergens` | Any logged-in user | The shared, site-wide allergen list. |
| `POST /api/allergens` | Any logged-in user | Add a new allergen to the shared list if it isn't already known. |

Example meal log:

```json
{ "mealType": "LUNCH", "calories": 650 }
```

## Try it

1. Log in as a customer, open **Personal Diet**, set a daily calorie goal (e.g. 1800) and press **Save Goal**.
2. Watch Burned Today / Consumed Today / Remaining Budget populate.
3. Add an allergy - pick one or type a new one - and the menu list below narrows to items that are both within budget and free of it.
4. Click a few food cards to add them to your cart. If their combined calories exceed what's left of your budget, a warning banners appears even though each one fit alone.
5. Name the order and press **Place Order**.
6. Press **History** (top right) to see every day you've consumed calories, most recent first.

## Code map

| Piece | File |
| :- | :- |
| Budget computation, meal logging, recommendation filtering, history | `food-seer-backend/src/main/java/FoodSeer/service/impl/CalorieBudgetServiceImpl.java` |
| Reads the per-user fitness-activity file | `food-seer-backend/src/main/java/FoodSeer/service/impl/FitnessDataServiceImpl.java` |
| Shared allergen list | `food-seer-backend/src/main/java/FoodSeer/service/impl/AllergenServiceImpl.java` |
| Endpoints | `UserController.java` (budget, meal log, history, preferences), `FoodController.java` (within-budget menu), `AllergenController.java` |
| Customer page | `food-seer-frontend/src/pages/PersonalDiet.js` |
| History page | `food-seer-frontend/src/pages/CalorieHistory.js` |
| Mock fitness data | `food-seer-backend/fitness-data/<username>.json`, generated via `food-seer-backend/scripts/generate_fitness_data.py` |
