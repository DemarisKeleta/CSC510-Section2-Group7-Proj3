# Project 1b — Proposed New Use Cases

Two features that follow naturally from FoodSeer's existing data model
(`dietaryRestrictions`, `costPreference` on User; `DriverStatsDto` for
drivers) but are not implemented in the Project 1a system, and are rarely
or never implemented by mainstream food-delivery platforms (DoorDash,
Uber Eats, Grubhub). Written in the same format as `use-cases.md`.

## Use Case #21

Part: Order Safety
Name: Cross-order allergy and dietary conflict check
Primary Actor: Customer

Stakeholders and Interests:
- Customer: wants to be warned before ordering food that conflicts with their own stated dietary restrictions, not just see per-item allergen tags they have to cross-check manually.
- Platform: wants to reduce liability from a customer being served food they explicitly told the system to avoid.
- Admin/Staff: wants confidence that the platform actively enforces the restrictions customers set, rather than only displaying them.

Preconditions:
- Frontend and backend are running correctly.
- The customer is authenticated and has a saved `dietaryRestrictions` value on their profile.
- The customer has added one or more items to their cart.

Trigger:
- Customer clicks "Place Order" with items in their cart.

Main Success Scenario:
1. Customer adds items to their cart and clicks "Place Order."
2. System compares every item's allergen/ingredient tags against the customer's saved dietary restrictions.
3. System finds no conflicts.
4. System creates the order as normal.
5. System confirms the order to the customer.

Extensions:
- 2a. One or more cart items conflict with the customer's saved restrictions.
  System blocks order submission and lists the specific conflicting items and the restriction each one violates, before any order is created.
- 2b. The customer has no dietary restrictions saved.
  System skips the check and proceeds directly to order creation.
- 3a. Customer wants to order a conflicting item anyway (e.g., ordering for someone else).
  System allows an explicit "order anyway" override per conflicting item, and records that the order was placed with an acknowledged conflict.

Postconditions:
- On success: the order is created and contains no unacknowledged conflicts with the customer's saved dietary restrictions.
- On a blocked submission: no order is created, and the customer has been shown exactly which items and restrictions conflicted.

Why no current platform does this: allergen tags today are informational only — the customer is expected to manually check the label. Enforcing dietary restrictions at the cart/checkout level is workable specifically because FoodSeer already stores structured `dietaryRestrictions` per user for its recommendation feature, so the checkout check reuses existing data rather than requiring new user input.

## Use Case #22

Part: Driver Earnings
Name: Driver earnings transparency / audit trail
Primary Actor: Driver

Stakeholders and Interests:
- Driver: wants to see exactly how their current earnings total was computed, not just a final number.
- Platform: wants to build driver trust and reduce disputes about pay.
- Admin: wants an auditable record to resolve driver pay complaints without digging through raw order data by hand.

Preconditions:
- Frontend and backend are running correctly.
- The driver is authenticated.
- The driver has at least one delivered order on record.

Trigger:
- Driver opens the "Earnings" tab on their dashboard.

Main Success Scenario:
1. Driver opens the Earnings tab.
2. System retrieves every earnings-affecting event tied to the driver (one entry per delivered order): order id, delivery timestamp, and amount credited.
3. System displays the entries as a chronological ledger, plus a running total that matches the driver's current `DriverStatsDto` earnings value.
4. Driver reviews the ledger.

Extensions:
- 2a. The driver has no delivered orders yet.
  System shows an empty ledger with a running total of zero, rather than nothing.
- 3a. The ledger total does not match the driver's stored earnings total (e.g., due to a double-credit event).
  System flags the discrepancy explicitly rather than silently displaying whichever number is larger, and surfaces it to admin for review.
- 4a. Driver disputes a specific entry.
  Driver can flag an individual ledger entry, which creates a reviewable record for admin instead of an untracked support conversation.

Postconditions:
- The driver has a complete, itemized view of how their current earnings total was reached.
- Any mismatch between the ledger and the stored earnings total is visible rather than hidden.

Why no current platform does this: gig-delivery pay algorithms are a well-documented industry pain point — drivers routinely report seeing only a final per-trip number with no line-item breakdown. This is buildable now specifically because it exposes structure that already exists in the code (each delivered order already credits `DriverStatsDto` in `updateOrderStatus`) rather than requiring a new pay model — it also directly follows from a real bug this team found in Project 1a (UC10's double-crediting defect), which a ledger like this would have made visible to the driver instead of silent.

### Use Case 3: Fitness-Based Calorie Tracking

**Primary actor:** Customer
**Goal:** See a daily calorie budget that accounts for activity, instead of a static number, when deciding what to order.

**Main flow**

1. The customer's fitness activity for the day is available to FoodSeer as a generated data file, standing in for a real fitness-app export.
2. FoodSeer reads the customer's calories burned for today from that file.
3. FoodSeer combines it with the customer's saved daily calorie goal to compute a remaining calorie budget for today.
4. The customer views their remaining calorie budget on their profile or dashboard.
5. When the customer goes to order, FoodSeer recommends menu items that fit within the customer's remaining calorie budget, instead of showing the full, unfiltered menu.

**Important extensions**

- If no fitness-data file exists yet for the customer, FoodSeer treats today's calories burned as "not tracked" rather than assuming zero.
- If the customer has no daily calorie goal saved, FoodSeer shows calories burned and consumed but does not compute a remaining budget, rather than inventing a default goal.
- If a food item has no calorie value recorded, FoodSeer excludes it from the recommended list and says so, rather than silently treating it as zero calories.
- If no menu item fits within the customer's remaining calorie budget, FoodSeer says so plainly rather than recommending the closest overage silently.
- If the fitness-data file is malformed or unreadable, FoodSeer falls back to "not tracked" for that customer rather than failing the request.
