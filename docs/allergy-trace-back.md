# Allergy Incident Trace-Back

## Why

When one customer has an allergic reaction, others who ate the same dish around the same time are often at risk too, and nobody tells them. Delivery apps let you save an allergy, but in our market survey we found none that trace an incident back through the kitchen and warn the other people it may have reached.

FoodSeer now does. A report from the restaurant or from a customer starts a trace. Customers whose orders were in the window and whose saved allergies match get a private notice. The notice never names anyone else, and the restaurant attaches its own offer (a refund or a remake), so the notice reads as the restaurant's own quiet recall rather than a public accusation.

## How it works

1. **A report comes in.**
   * Staff report that dishes may carry an allergen during a time window (Allergy Incidents page).
   * A customer reports a reaction on one of their own orders (My Orders page). The window is 2 hours either side of that order (`foodseer.trace.window-hours`). If no dish in the order declares the allergen, every dish in it is treated as a suspect.
2. **Trace.** Every order that contains a suspect dish and was placed inside the window is exposed.
3. **Match.** A customer is warned if their saved restrictions overlap the allergen. Matching understands case, plurals, filler words and groups, so "peanut", "Peanuts" and "peanut allergy" match, "nuts" covers peanuts and tree nuts, and "fish" never matches "shellfish". Exact string matching, which FoodSeer used before, misses these; in the M0 eval it found under half of the truly exposed customers.
4. **Notify.** Each affected customer gets one private notice. The reporter is never notified about their own report.
5. **Respond.** Staff see every incident with counts only, acknowledge it and attach an offer. Customers then see that offer on their notice.

## API

| Method and path | Who | What |
| :- | :- | :- |
| `POST /api/incidents` | Staff, Admin | Report dishes, allergen and window. Returns the incident id and counts. |
| `POST /api/incidents/reaction` | Customer | Report a reaction on your own order. Returns the incident id only. |
| `GET /api/incidents` | Staff, Admin | Every incident, newest first, with counts and never customer names. |
| `POST /api/incidents/{id}/acknowledge` | Staff, Admin | Acknowledge and attach an offer (1 to 500 characters). |
| `GET /api/notifications/my` | Customer | Your notices, newest first, with the restaurant's offer. |
| `GET /api/notifications/my/unread-count` | Customer | How many notices you have not acknowledged. |
| `POST /api/notifications/{id}/acknowledge` | Customer | Acknowledge one of your notices. |

Example restaurant report:

```json
{ "allergen": "peanut", "foodIds": [30], "windowStart": "2026-10-12T11:00", "windowEnd": "2026-10-12T13:00" }
```

## Try it

1. Log in as a customer with "Peanuts" saved in preferences and order COOKIES.
2. Log in as staff, open **Allergy Incidents**, enter "Peanuts", tick COOKIES, keep the default window and press **Trace and warn customers**.
3. Log back in as the customer: a banner and **Safety Notices** show the notice.
4. As staff, type an offer and press **Acknowledge**. The customer's notice now shows it.

## Code map

| Piece | File |
| :- | :- |
| Matching rules | `food-seer-backend/src/main/java/FoodSeer/service/AllergenMatcher.java` |
| Trace and notify | `food-seer-backend/src/main/java/FoodSeer/service/impl/TraceServiceImpl.java` |
| Customer notices | `food-seer-backend/src/main/java/FoodSeer/service/impl/NotificationServiceImpl.java` |
| Endpoints | `IncidentController.java`, `NotificationController.java` |
| Staff page | `food-seer-frontend/src/pages/AllergyIncidents.js` |
| Customer pages | `food-seer-frontend/src/pages/SafetyNotices.js`, report button in `Orders.js` |

## M0: the risk-killer eval

**Claim:** alert recall of at least 0.95 and alert precision of at least 0.70 on simulated incidents. The baselines are exact string matching, alerting every allergic customer who ordered that day, and alerting without an allergy check.

**Instrument:** `food-seer-backend/src/test/java/FoodSeer/eval/M0TraceEval.java`. It runs the real `TraceServiceImpl` on 5 seeded simulated days with 500 incidents. The class Javadoc explains the simulation. It runs in the **M0 Eval** GitHub workflow, which posts the report to the job summary. The class name does not end in "Test", so the normal build does not include it.

```
cd food-seer-backend
mvn -Dtest=M0TraceEval test
cat target/m0-eval/m0-report.md
```

The numbers come from fixed seeds, so they are the same on every machine.

### Latest result (October 12, 2026): the claim fails

Measured with `mvn -Dtest=M0TraceEval test` on 500 simulated incidents (294 restaurant reports, 206 customer reports).

| Approach | Recall | Precision | Alerts sent | Truly exposed |
| :- | -: | -: | -: | -: |
| FoodSeer trace-back (this feature) | 0.890 | 0.287 | 3531 | 1138 |
| Same trace, exact string match (old FoodSeer matching) | 0.446 | 0.277 | 1831 | 1138 |
| Every allergic customer who ordered that day | 0.926 | 0.018 | 58683 | 1138 |
| Everyone who ordered a suspect dish in the window | 0.960 | 0.024 | 46447 | 1138 |

| Reported by | Recall | Precision |
| :- | -: | -: |
| Restaurant | 0.889 | 0.610 |
| Customer | 0.892 | 0.164 |

Trace-back doubles the recall of exact string matching and is more than ten times as precise as either no-trace baseline. It still misses both targets.

**What the eval found:**

1. Customer reports are the main precision problem (0.164). When no dish declares the allergen, every dish in the reporter's order becomes a suspect, and the 2 hour window on each side is wide, so many customers who ate an innocent dish get a notice.
2. Recall loses about 11%. Some free-text restrictions are phrases the matcher does not know ("no dairy products", "milk protein", "egg whites", "soy sauce", "tahini", "peanut butter", "finned fish"), and some restaurant windows are guessed too narrow.
3. Restaurant reports that name an innocent extra dish cost precision.

**Spiral review:** recorded in the team's GitHub issue "M0 spiral review: trace-back misses its recall and precision targets". The options are: (a) for customer reports with several undeclared dishes, alert only after staff confirm the dish; (b) narrow the customer window; (c) match known allergen words inside longer phrases; (d) revise the claim. Tuning the matcher to the simulator's own phrase list would flatter the next run, so new held-out phrasings must be added before measuring again.
