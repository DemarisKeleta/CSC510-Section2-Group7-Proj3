# FoodSeer Frontend Recreation - Setup Guide

This guide will help you set up and run the FoodSeer application.

## 📑 Table of Contents

- [Prerequisites](#prerequisites)
- [Dependencies](#dependencies)
- [Backend Setup](#backend-setup)
  - [1. Configure MySQL Database](#1-configure-mysql-database)
  - [2. Review application.properties](#2-review-applicationproperties)
  - [3. How to Set Up Ollama](#3-how-to-set-up-ollama)
  - [4. Build and Run Backend](#4-build-and-run-backend)
  - [5. Verify Backend](#5-verify-backend)
- [Frontend Setup](#frontend-setup)
- [Notes](#notes)
- [Using the Application](#using-the-application)
- [API Endpoints](#api-endpoints)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
  - [Common Issues](#common-issues)
  - [Chatbot-Specific Issues](#chatbot-specific-issues)
- [Getting Help](#getting-help)
- [Quick Start Summary](#quick-start-summary)


## Prerequisites

- [Java JDK](https://www.oracle.com/java/technologies/downloads/) (version 11 or higher, Java 21 recommended)
- [Maven](https://maven.apache.org/download.cgi) (version 3.6 or higher)
- [Node.js](https://nodejs.org/) (version 16.x or higher)
- [npm](https://www.npmjs.com/) (version 8.x or higher)
- [MySQL](https://dev.mysql.com/downloads/workbench/) (version 8.0 or higher)
- Git
- A modern web browser (Chrome, Firefox, Safari, or Edge)
- A chromium based web browser (Google Chrome)

## Dependencies 

Every library, its version, and its license are listed in [THIRD_PARTY_LIBRARIES.md](THIRD_PARTY_LIBRARIES.md).

| Language | Tool         | Command                  |
| -------- | ------------ | ------------------------ |
| Java     | Maven        | `mvn clean install`      |
| Node     | npm          | `npm install`            |
| Tests    | Maven + Jest | `mvn test` / `npm test`  |
| Coverage | JaCoCo       | `mvn test jacoco:report` |

## Frontend Setup

### 1. Install Dependencies

```bash
cd food-seer-frontend
npm install
```

### 2. Run Frontend

```bash
npm start
```

The frontend will start on `http://localhost:3000`

## Notes

- The frontend uses JWT tokens stored in localStorage for authentication
- Tokens expire after 7 days
- All API calls include the Bearer token in the Authorization header
- Protected routes redirect to login if not authenticated

## Backend Setup

### 1. Configure MySQL Database

Make sure MySQL is running on your machine. The application will automatically create the database if it doesn't exist.

Default configuration:
- Database: `users`
- Host: `localhost:3306`
- Username: `root`
- Password: `` (empty)

If your MySQL setup is different, update `food-seer-backend/src/main/resources/application.properties`

### 2. Review application.properties

`food-seer-backend/src/main/resources/application.properties` is already committed with working defaults (MySQL `root` user, no password, admin password `admin123`), so most setups can run as-is with no edits.

If your MySQL setup differs, edit that file directly:
- `spring.datasource.password`: set to whatever your MySQL password is
- `app.jwt-secret`: optionally replace with your own SHA256 string (e.g. via https://emn178.github.io/online-tools/sha256.html)
- `app.admin-user-password`: set to whatever you want the default admin password to be

### 3. How to Set Up Ollama

#### Step 1: Download Ollama

##### For Windows:
1. Go to https://ollama.com/download
2. Click "Download for Windows"
3. Run the installer (`OllamaSetup.exe`)
4. Follow the installation wizard

##### For Mac:
1. Go to https://ollama.com/download
2. Click "Download for macOS"
3. Open the `.dmg` file and drag Ollama to Applications

##### For Linux:
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

#### Step 2: Pull the gemma3:1b Model

After installation, open a terminal and run:

```bash
ollama pull gemma3:1b
```

This will download the model (~1GB). Wait for it to complete.

#### Step 3: Start Ollama Server

Ollama should start automatically after installation. To verify it's running:

```bash
ollama list
```

You should see `gemma3:1b` in the list.

If Ollama isn't running, start it:
```bash
ollama serve
```

The server will run on `http://localhost:11434` by default.


### 4. Build and Run Backend

```bash
cd food-seer-backend
mvn clean install
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### 5. Verify Backend

The application automatically creates an admin user with credentials:
- Username: `admin`
- Password: `what you have app.admin-user-password set to in the application.properties file`

You can test the login endpoint:
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## Using the Application

### Application Flow:

1. **Login Page** (`/`)
   - Use demo credentials: `admin` / `admin123`
   - Or register a new account (`/register`) - customers get a starter fitness-data file automatically

2. **Preferences Page** (`/preferences`)
   - After login, set your cost preference and dietary restrictions
   - Click "Next" between steps; saved to backend on completion

3. **Recommendations Page** (`/recommendations`)
   - View personalized recommendations based on saved preferences

4. **Personal Diet** (`/personal-diet`) - **Customer Only**
   - Set a daily calorie goal; see today's burned/consumed/remaining budget
   - Browse a menu filtered to what fits your remaining budget and is free of your saved allergies
   - Add items to a cart and place an order
   - View per-day consumption history at `/calorie-history`

5. **AI Assistant** (`/chatbot`) - **Customer Only**
   - Click "AI Assistant" in navigation and answer 3 questions about how you're feeling, how hungry you are, and what you're in the mood for
   - Get one personalized food recommendation; order it directly or ask for another suggestion

6. **Quiz** (`/quiz`) - **Customer Only** - a short guided quiz alternative to the chatbot

7. **Create Order / My Orders** (`/create-order`, `/orders`)
   - Browse the full menu and place an order directly
   - View your own order history, rate fulfilled items, and report an allergic reaction on an order

8. **Browse Foods** (`/inventory`) - view the full menu/inventory

9. **Staff & Admin pages**
   - **Order Management** (`/order-management`) - fulfill orders
   - **Inventory Management** (`/inventory-management`) - add/edit/delete menu items
   - **Allergy Incidents** (`/allergy-incidents`) - report an incident and trace which customers were affected
   - **Safety Notices** (`/notices`) - customers see any safety notices here
   - **User Management** (`/users`, admin only) - manage user roles
   - **Dashboards** (`/inventory-dashboard`, `/order-dashboard`, admin only)

10. **Driver Dashboard** (`/driver-dashboard`) - **Driver Only**
    - Claim available orders, track active deliveries, and acknowledge allergy warnings before pickup/delivery

---

## API Endpoints

### Authentication
- `POST /auth/login` - Login with username and password
- `POST /auth/register` - Register a new user

### User Management
- `GET /api/users` - List all users (admin only)
- `GET /api/users/{id}` - Get a user by id (admin only)
- `PUT /api/users/{id}/role` - Update a user's role (admin only)
- `DELETE /api/users/{id}` - Delete a user (admin only)
- `GET /api/users/me` - Get current user info
- `PUT /api/users/me/preferences` - Update cost preference, dietary restrictions, and daily calorie goal
- `GET /api/users/me/calorieBudget` - Today's burned/goal/consumed/remaining calorie budget
- `POST /api/users/me/mealLog` - Log (or update) calories for one meal slot today
- `GET /api/users/me/calorieHistory` - Calories consumed per day, most recent first

### Foods / Menu
- `GET /api/foods` - List all foods
- `GET /api/foods/{id}` - Get a food by id
- `GET /api/foods/withinCalorieBudget` - Menu items that fit the current user's remaining calorie budget
- `POST /api/foods` - Create a food (staff/admin)
- `POST /api/foods/updateFood` - Update a food (staff/admin)
- `DELETE /api/foods/{id}` - Delete a food (staff/admin)
- `POST /api/foods/orders/{orderId}/{foodId}/rate` - Rate a food from a fulfilled order

### Inventory
- `GET /api/inventory` - Get current inventory
- `POST /api/inventory` - Update inventory

### Orders
- `GET /api/orders` - List all orders
- `GET /api/orders/fulfilledOrders` / `GET /api/orders/unfulfilledOrders` - Filtered order lists
- `POST /api/orders` - Create an order
- `POST /api/orders/fulfillOrder` - Mark an order fulfilled
- `GET /api/orders/{id}` - Get an order by id
- `GET /api/orders/my-orders` (`/fulfilled`, `/unfulfilled`) - Current user's own orders
- `POST /api/orders/{id}` - Update an order's status (driver/admin)
- `POST /api/orders/{id}/claim` - A driver claims an available order
- `GET /api/orders/availableOrders` - Orders available for a driver to claim
- `GET /api/orders/activeOrders/{username}` - A driver's active (picked-up) orders

### Allergy Handoff
- `POST /api/orders/{orderId}/allergy-handoff/acknowledge` - Staff/driver acknowledge an order's allergy warning
- `GET /api/orders/{orderId}/allergy-handoff/status` - Check acknowledgment status

### Allergy Incident Trace-Back
- `POST /api/incidents` - Staff report dishes/allergen/window
- `POST /api/incidents/reaction` - Customer reports a reaction on their own order
- `GET /api/incidents` - List all incidents (staff/admin)
- `POST /api/incidents/{id}/acknowledge` - Staff acknowledge and attach an offer
- `GET /api/notifications/my` - Customer's own safety notices
- `GET /api/notifications/my/unread-count` - Unread notice count
- `POST /api/notifications/{id}/acknowledge` - Acknowledge a notice

### Allergens
- `GET /api/allergens` - The shared, site-wide allergen list
- `POST /api/allergens` - Add a new allergen if it isn't already known

### Driver Stats
- `GET /api/driverStats` - A driver's stats (earnings, deliveries)

### Chat/AI
- `POST /api/chat` - Send message to AI chatbot (requires authentication)

### Request/Response Examples

**Login Request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Login Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer"
}
```

**Update Preferences Request:**
```json
{
  "costPreference": "under-20",
  "dietaryRestrictions": "vegan, lactose-intolerant"
}
```

**Chat Request:**
```json
{
  "message": "I'm feeling tired and want comfort food"
}
```

**Chat Response:**
```json
{
  "message": "Based on your preferences, I recommend..."
}
```

---

## Testing

### Backend Tests

Using Maven:
```bash
cd food-seer-backend
mvn test

# Run specific test class
mvn test -Dtest=YourTestClass

# Run with coverage
mvn test jacoco:report
```

### Frontend Tests

```bash
cd food-seer-frontend
npm test
```

### Testing the Chatbot Integration

1. Start Ollama, backend, and frontend
2. Login as a **customer** (not admin/staff)
3. Navigate to "🤖 AI Assistant"
4. Complete the 3-question conversation
5. Verify recommendation is received
6. Test "Order This Now!" functionality
7. Test "Get Another Suggestion"

### Code Coverage

**Backend (Maven with JaCoCo):**
```bash
cd food-seer-backend
mvn clean test jacoco:report
# View report at: target/site/jacoco/index.html
```

---

## Troubleshooting

### Common Issues

#### Port Already in Use

**Problem:** Port 8080 (backend) or 3000 (frontend) already in use

```bash
# Find and kill process using port
# On macOS/Linux:
lsof -i :8080
lsof -i :3000
# Kill process:
lsof -ti:8080 | xargs kill -9

# On Windows:
netstat -ano | findstr :8080
# Kill using task manager or:
taskkill /PID <PID> /F
```

#### Java Version Issues

**Problem:** Wrong Java version
```bash
# Check current Java version
java -version

# Set JAVA_HOME environment variable
# On macOS/Linux:
export JAVA_HOME=/path/to/java11
# On Windows:
set JAVA_HOME=C:\Program Files\Java\jdk-11

# Verify
echo $JAVA_HOME  # macOS/Linux
echo %JAVA_HOME%  # Windows
```

#### MySQL Connection Issues

**Problem:** Cannot connect to database

Solutions:
- Verify MySQL service is running:
  ```bash
  # On macOS:
  brew services start mysql
  # On Linux:
  sudo systemctl start mysql
  # On Windows: Start MySQL service from Services panel
  ```
- Check database credentials in `application.properties`
- Ensure database exists and user has proper permissions
- Verify MySQL is listening on port 3306

#### Maven Build Failures

**Problem:** Maven dependencies not downloading
```bash
# Clear Maven cache and rebuild
mvn clean install -U

# Force update snapshots
mvn clean install -U -DskipTests

# Clear local repository cache
rm -rf ~/.m2/repository
mvn clean install
```

#### Spring Boot Application Won't Start

**Problem:** Application fails to start
```bash
# Check logs for specific errors
mvn spring-boot:run

# Run with debug enabled
mvn spring-boot:run -Dspring-boot.run.arguments=--debug

# Verify all required dependencies
mvn dependency:tree
```

#### Frontend Module Not Found Errors

```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Clear npm cache
npm cache clean --force
npm install
```

#### CORS Errors

**Problem:** Frontend can't connect to backend

Solutions:
- Verify backend is running on port 8080
- Check `@CrossOrigin` annotation in controllers
- Verify `REACT_APP_API_URL` in frontend `.env`
- Check browser console for specific CORS error

#### Login Fails

**Problem:** Cannot login with admin credentials

Solutions:
- Verify backend is running
- Check that admin user was created (check logs)
- Verify API_BASE_URL in `src/services/api.js`
- Check browser console and network tab
- Test login endpoint directly:
  ```bash
  curl -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}'
  ```

### Chatbot-Specific Issues

#### "Failed to send message to AI"
- **Cause:** Ollama is not running
- **Fix:** Run `ollama serve` in a terminal

#### "No response from AI"
- **Cause:** Model not downloaded
- **Fix:** Run `ollama pull gemma3:1b`

#### "Error communicating with Ollama"
- **Cause:** Ollama running on different port
- **Fix:** Check Ollama is on port 11434 (default)
- **Verify:** `curl http://localhost:11434`

#### Backend 500 Error for Chat
- **Cause:** Backend can't reach Ollama
- **Fix:** Ensure Ollama is running
- **Test:** `curl http://localhost:11434`

#### AI Assistant Link Not Visible
- **Cause:** Not logged in as customer
- **Fix:** Ensure you're logged in with customer role, not admin/staff

---

## Getting Help

For questions or issues:
1. Check the [GitHub Issues](https://github.com/DemarisKeleta/CSC510-Section2-Group7-Proj3/issues)
2. Check the demo video linked in the README
3. Review contribution guidelines before submitting PRs

---

## Quick Start Summary

```bash
# 1. Clone repo
git clone https://github.com/DemarisKeleta/CSC510-Section2-Group7-Proj3.git
cd CSC510-Section2-Group7-Proj3

# 2. Setup MySQL database

# 3. Install and start Ollama
ollama pull gemma3:1b
ollama serve

# 4. Start backend
cd food-seer-backend
mvn spring-boot:run

# 5. Start frontend (in new terminal)
cd food-seer-frontend
npm install
npm start

# 6. Access application
# http://localhost:3000
# Login: admin / admin123
```









