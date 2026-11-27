# Backend Steps: 11 Features (Step-by-Step Flow, No Code)

**Stack:** Java Spring Boot + MySQL (backend), React (frontend)

## Contents

0. Setup Guide (local PC)
1. Login
2. Products
3. Arrivals
4. Buyers (and Sales)
5. Donations
6. Reports
7. Alerts
8. Transport Cost
9. Warehouses
10. Drivers
11. Dashboard

---

# 0. Setup Guide (Local PC)

The backend lives in its own folder, MySQL runs on your local PC, you test with Postman, and later connect React through the APIs.

## A. Install and create the project

- Install: JDK 17 (or 21), IntelliJ IDEA, MySQL 8 + MySQL Workbench, Postman, Git.
- Go to https://start.spring.io: Project = Maven, Language = Java, Spring Boot 3.x (stable), Packaging = Jar, Java 17.
- Group = `com.sphwms`, Artifact = `sphwms-backend`. Download the zip, extract into a separate folder named `sphwms-backend`, open in IntelliJ. Create an empty GitHub repo with the same name (`sphwms-backend`); the frontend gets its own repo, `sphwms-frontend` (see section B).

**Dependencies (pom.xml)**

| Dependency | Why |
|---|---|
| spring-boot-starter-web | REST API |
| spring-boot-starter-security | Login, JWT filter, role check |
| spring-boot-starter-data-jpa | Database (JPA/Hibernate) |
| spring-boot-starter-validation | @NotBlank, @Positive (block empty fields) |
| mysql-connector-j | MySQL driver |
| jjwt-api, jjwt-impl, jjwt-jackson (0.12.x) | JWT token |
| lombok | Less getter/setter boilerplate |
| poi-ooxml | Export XLSX |
| openpdf | Receipt / PDF |
| spring-boot-devtools (optional) | Auto restart |

## B. GitHub repositories and folder structure

### B1. Two separate GitHub repositories

| Repo | Name |
|---|---|
| Backend | `sphwms-backend` |
| Frontend | `sphwms-frontend` |

Use lowercase letters and hyphens only. This matches the Artifact name (`sphwms-backend`) and avoids trouble with Git and the command line.

### B2. Backend folder format (sphwms-backend)

Module style like your company project (ab-smart-visit-backend): each feature is its own module, with an `internal` package. At the start you don't need a public interface like MediaApi; modules can use each other's service/repository directly.

```
sphwms-backend/
├── .gitignore
├── README.md
├── pom.xml
├── docs/
│   ├── features.md
│   └── api-collection.json
└── src/
    ├── main/
    │   ├── java/com/sphwms/
    │   │   ├── SphwmsApplication.java
    │   │   │
    │   │   ├── auth/internal/
    │   │   │   ├── config/          (SecurityConfig, CorsConfig)
    │   │   │   ├── controller/      (AuthController)
    │   │   │   ├── dto/             (LoginRequest, LoginResponse)
    │   │   │   ├── security/        (JwtService, JwtFilter)
    │   │   │   └── service/         (AuthService)
    │   │   │
    │   │   ├── user/internal/       (entity, enums, repository, service)
    │   │   ├── warehouse/internal/
    │   │   ├── driver/internal/
    │   │   ├── product/internal/
    │   │   ├── stock/internal/      (StockBatch, StockTransaction)
    │   │   ├── arrival/internal/    (Arrival, QualityCheck)
    │   │   ├── buyer/internal/      (Buyer, DuePayment)
    │   │   ├── sale/internal/       (Sale, SaleAllocation, PaymentAllocation)
    │   │   ├── donation/internal/   (Receiver, Donation)
    │   │   ├── trip/internal/       (Trip, section G)
    │   │   ├── transportcost/internal/ (TripCost)
    │   │   ├── alert/internal/      (Alert, AlertSetting, RefreshJob)
    │   │   ├── report/internal/     (queries + export)
    │   │   ├── dashboard/internal/  (Feature 11)
    │   │   │
    │   │   └── shared/
    │   │       ├── exception/       (BusinessException, NotFoundException, GlobalExceptionHandler)
    │   │       ├── audit/           (AuditLog, AuditService)
    │   │       └── entity/          (BaseEntity: createdAt, createdBy, updatedAt, updatedBy)
    │   │
    │   └── resources/
    │       ├── application.properties          (does NOT go to Git)
    │       └── application-example.properties  (goes to Git, with a fake password)
    │
    └── test/java/com/sphwms/                   (tests per feature)
```

Every feature module (warehouse, driver, product, etc.) uses the same layers:

```
<feature>/internal/
├── controller/
├── dto/
│   ├── requests/
│   └── responses/
├── entity/
├── enums/
├── exception/
├── repository/
└── service/
```

**Tip:** Products, Arrivals, Sales, Donations, Warehouses and Alerts all use stock_batch, so keep it in its own `stock` module. That avoids circular dependencies.

### B3. Must-have files (backend)

- **`.gitignore`**: `target/`, `.idea/`, `application.properties`, `*.log`
- **`application-example.properties`**: a copy of `application.properties` without the real password. Anyone who clones the repo renames it to `application.properties` and puts in their own password.
- **`README.md`**: project name, stack (Java 17, Spring Boot 3, MySQL 8), how to run, test users (manager / admin / driver1), where the Postman collection is (see the template in B6).
- **`docs/`**: the Postman collection and notes (this steps file too).

### B4. Git conventions

- **Branches:** `main` (stable), `develop` (work in progress), feature branches such as `feature/warehouses`, `feature/arrivals`.
- **Commit messages:** `feat: add warehouse CRUD`, `fix: arrival double confirm lock`, `docs: update README`.
- Commit each feature once it passes in Postman, following the routine in section F.

### B5. Frontend repo (sphwms-frontend)

React + Vite. Only the name and the starting structure for now:

```
sphwms-frontend/
├── .env.example          (VITE_API_URL=http://localhost:8080)
├── .gitignore            (node_modules/, .env, dist/)
├── README.md
└── src/
    ├── api/              (axios setup, feature-wise API calls)
    ├── components/
    ├── pages/            (Login, Dashboard, Warehouses, ...)
    ├── hooks/
    └── utils/
```

### B6. README.md template (backend)

````markdown
# sphwms-backend

Backend API for SPHWMS (warehouse management system).

## Stack
- Java 17, Spring Boot 3.x, Maven
- MySQL 8
- Spring Security + JWT

## Run locally
1. Install JDK 17, MySQL 8, Maven (or use the IntelliJ built-in Maven).
2. Copy `src/main/resources/application-example.properties` to
   `src/main/resources/application.properties` and set your MySQL password and JWT secret.
3. Start MySQL. The database `sphwms` is created automatically.
4. Run `SphwmsApplication` from IntelliJ, or `mvn spring-boot:run`.
5. API runs at http://localhost:8080

## Test users (development only)
| Username | Password | Role |
|---|---|---|
| manager | <your-password> | MANAGER |
| admin | <your-password> | ADMIN |
| driver1 | <your-password> | DRIVER |

## Postman
Import `docs/api-collection.json` into Postman. Set the environment variable
`baseUrl = http://localhost:8080`. Log in first; the token is saved automatically.

## Branches and commits
- `main` stable, `develop` in progress, `feature/<name>` per feature
- Commit style: `feat:`, `fix:`, `docs:`

## Docs
See `docs/features.md` for the step-by-step flow of every feature.
````

The frontend `README.md` can follow the same shape: name, stack (React + Vite), copy `.env.example` to `.env`, `npm install`, `npm run dev` (http://localhost:5173), and a link to the backend repo.

## C. application.properties (local MySQL)

```
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/sphwms?createDatabaseIfNotExist=true&serverTimezone=Asia/Dhaka
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.jackson.time-zone=Asia/Dhaka
app.jwt.secret=put-a-long-random-string-at-least-32-characters
app.jwt.expiry-minutes=30
app.cors.allowed-origin=http://localhost:5173
```

- The database `sphwms` is created automatically (`createDatabaseIfNotExist`). Data stays in your PC's MySQL; you can view tables in MySQL Workbench.
- `ddl-auto=update` is for development only (tables are auto-created). In production use Flyway migrations.
- React with Vite runs on port 5173, Create React App on 3000. Set `app.cors.allowed-origin` accordingly.
- The file contains your password. Add it to `.gitignore` before pushing to Git, or use environment variables.
- Keep a copy named `application-example.properties` (same content, fake password and fake JWT secret). That copy goes to Git; the real `application.properties` does not.

## D. Login and token (most important)

**Rule:** Every `/api/**` endpoint except `/api/auth/login` needs a valid token. No token gives 401. Wrong role gives 403. Write this rule once in SecurityConfig and it applies to every feature.

1. `POST /api/auth/login` (username, password).
2. Backend finds the user, checks status, matches the password with BCrypt (Login Steps 1-6).
3. If correct, it creates a JWT (user id, role, expiry 30 min) and returns it in the response.
4. Every later request sends the header `Authorization: Bearer <token>`. JwtFilter checks it.
5. If the token expires: 401, and React sends the user to the login page.

**Login request / response**

```
POST http://localhost:8080/api/auth/login
{ "username": "manager", "password": "<your-password>" }

200 OK
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "name": "Rahim Uddin",
  "role": "MANAGER", "expiresInSeconds": 1800 }
```

- The PDF's Login flow says to send the token in an HttpOnly cookie. For Postman and local development, also return the token in the response body, and have JwtFilter check the Authorization header first, then the cookie. In production, prefer the cookie.
- Logout: clear the cookie. With header tokens the server can't invalidate a token (it stays valid until expiry), so keep the expiry short.

**Test users** (auto-created on startup in development). Use a CommandLineRunner to seed them when the user table is empty, storing BCrypt-hashed passwords:

| Username | Password | Role | Purpose |
|---|---|---|---|
| manager | <your-password> | MANAGER | All 10 features + Dashboard |
| admin | <your-password> | ADMIN | User management (Login section) |
| driver1 | <your-password> | DRIVER | Own profile |

- These are for testing only. Change passwords for real use.
- Test with the manager token in Postman. Trying a driver token should return 403 (role check works).

## E. Block empty fields (validation)

Block in two places: React (disable the Save button) and the backend (more important, since Postman or any other client could bypass React).

```java
public record WarehouseRequest(
  @NotBlank(message = "Warehouse name is required.") String name,
  @NotBlank(message = "Location is required.") String location,
  @NotBlank(message = "In-charge is required.") String inCharge,
  @NotNull @Positive(message = "Capacity must be greater than zero.")
  BigDecimal capacityKg) {}

@PostMapping
public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody WarehouseRequest req) {...}
```

Put `@NotBlank` (text), `@NotNull` (number/date/id), `@Positive` / `@PositiveOrZero` (money, quantity) on every POST/PUT request DTO, and `@Valid` in the controller. A `GlobalExceptionHandler` (`@RestControllerAdvice`) converts these errors to 400:

```
400 Bad Request
{ "message": "Warehouse name is required.",
  "errors": { "name": "Warehouse name is required.",
              "capacityKg": "Capacity must be greater than zero." } }
```

- Keep messages exactly as written in the PDF (e.g. "Required fields are missing.", "Invalid quantity - enter a positive number.").
- Business-rule errors (no stock, duplicate phone): `BusinessException` gives 400. Missing id: `NotFoundException` gives 404. No token: 401. Wrong role: 403.
- Trim text fields. A blank space " " is caught by `@NotBlank`; whether a single character like "A" is acceptable is your decision.
- Test: send body `{}` in Postman. If you get 400, validation works.

## F. Postman setup

1. Create a collection "SPHWMS". Create an environment with `baseUrl = http://localhost:8080` and `token =` (empty).
2. In the collection's Authorization tab: Type = Bearer Token, Token = `{{token}}`. Keep folders/requests on "Inherit auth from parent".
3. In the Login request's Tests tab, add this so the token is saved automatically on login:
   ```
   const res = pm.response.json();
   pm.environment.set("token", res.token);
   ```
4. Make a separate folder per feature (Login, Warehouses, Drivers, Products...). Request URL example: `{{baseUrl}}/api/warehouses`.
5. Test 3 cases per endpoint: (a) correct data gives 200/201, (b) empty or wrong data gives 400, (c) no token gives 401.
6. Export the collection as `api-collection.json` and keep it in the project's `docs/` folder so a friend or your future self can use it.

**Build and test order (by dependency).** Don't follow the PDF's feature numbers; this order avoids one feature blocking another:

| Stage | Feature | Why this order |
|---|---|---|
| 1 | Login + User seed | Nothing can be tested without a token |
| 2 | Warehouses | Products, Arrivals, Sales all need warehouses |
| 3 | Drivers | Needed for Trips and Alerts |
| 4 | Products (+ stock module) | Product, StockBatch, StockTransaction |
| 5 | Arrivals | Stock increases on confirm |
| 6 | Buyers + Sales | Stock decreases (the biggest piece) |
| 7 | Donations | Stock decreases from near-expiry batches |
| 8 | Trip (section G) + Transport Cost | Needs delivered trips |
| 9 | Alerts | Needs data from all tables |
| 10 | Reports | SUMs from Sale, Arrival, Donation |
| 11 | Dashboard (Feature 11) | When all data is ready |

Routine for every feature: entity, repository, DTO (validation), service, controller, Postman test, Git commit. Don't move to the next feature until the current one passes in Postman.

## G. Trip table (extra)

Transport Cost, Drivers (ongoing trip check) and Alerts depend on trips, but there is no Trip page among the 10 features.

Create a minimal Trip table: `trip_id`, `trip_no` (T-032), `driver_id` (FK), `origin`, `destination`, `trip_date`, `status` (PLANNED / ONGOING / DELIVERED / CANCELLED), `warehouse_id` (optional; needed if Reports adds transport cost).

- For Postman testing, build a small `POST /api/trips` and `PATCH /api/trips/{id}/status` (or insert a few rows in MySQL Workbench). When a Trip page is built later it will use these APIs.
- A new trip allows only an active driver (Drivers Step 8) and an active warehouse.
- Transport Cost "available trips" means: status = DELIVERED and no trip_cost yet.

## H. Endpoint list per feature (for Postman)

Role: MANAGER on all endpoints, except Login.

| Feature | Endpoints |
|---|---|
| Login | `POST /api/auth/login`, `POST /api/auth/logout` |
| Warehouses | `GET /api/warehouses?q=&status=ALL`, `GET /{id}`, `GET /next-code`, `POST`, `PUT /{id}`, `PATCH /{id}/status`, `GET /options?includeInactive=` |
| Drivers | `GET /api/drivers?q=&status=ALL&page=1&size=6`, `GET /{id}`, `POST`, `PUT /{id}`, `PATCH /{id}/status`, `GET /options` |
| Products | `GET /api/products?search=&category=&warehouse=&status=`, `POST`, `PUT /{id}`, `DELETE /{id}` (soft), `GET /summary/category` |
| Arrivals | `GET /api/arrivals?status=`, `GET /summary`, `GET /next-batch-no`, `POST`, `POST /{id}/confirm`, `POST /{id}/reject` |
| Buyers / Sales | `GET /api/buyers?search=`, `GET /{id}`, `GET /{id}/summary`, `POST /api/buyers`, `PUT /{id}`, `DELETE /{id}`, `POST /{id}/payments`, `GET /api/sales?buyerId=&from=&to=`, `POST /api/sales`, `GET /api/sales/next-no`, `GET /api/sales/export` |
| Donations | `GET /api/donations/eligible`, `GET /api/receivers`, `POST /api/receivers`, `POST /api/donations`, `GET /api/donations`, `GET /summary/by-receiver`, `GET /{id}/receipt`, `GET /export` |
| Transport Cost | `GET /api/trip-costs/monthly`, `GET /api/trip-costs?q=&page=&size=`, `GET /available-trips`, `POST`, `PUT /{id}`, `DELETE /{id}`, `GET /export` |
| Alerts | `GET /api/alerts/summary`, `/by-type`, `/unread-count`, `GET /api/alerts`, `PATCH /{id}/read`, `PATCH /read-all`, `GET /{id}`, `GET, PUT /settings` |
| Reports | `GET /api/reports/summary`, `/revenue-by-category`, `/top-products`, `/export` (from, to, warehouseId) |
| Dashboard | `GET /api/dashboard/summary`, `GET /api/dashboard/inventory-ranking?limit=15` |

**Sample bodies** (Postman: Body > raw > JSON)

```
POST /api/warehouses
{ "name": "Warehouse A", "location": "Dhaka", "inCharge": "Karim",
  "capacityKg": 12000, "status": "ACTIVE" }

POST /api/drivers
{ "name": "Jamal Hossain", "licenseNo": "DH-123456", "licenseExpiry": "2027-06-30",
  "phone": "01711000001", "vehicleType": "TRUCK", "vehicleNo": "DH-11-2201" }

POST /api/products
{ "name": "Rice (Miniket)", "category": "Grain", "warehouseId": 1,
  "purchasePrice": 60, "sellingPrice": 70, "quantity": 900,
  "entryDate": "2026-11-01", "expiryDate": "2027-05-01" }

POST /api/arrivals
{ "productId": 1, "warehouseId": 1, "quantity": 500, "grade": "A",
  "expiryDate": "2027-03-01" }

POST /api/arrivals/1/confirm
{ "packagingOk": true, "noDamage": true, "quantityMatch": true,
  "expiryValid": true, "gradeMatch": true }

POST /api/sales
{ "buyerId": 1, "productId": 1, "warehouseId": 1, "quantity": 100,
  "saleDate": "2026-11-24", "paidNow": 3000 }
```

- Field names must match between backend and React; align them with your own DTOs. The above are only examples that match the PDF's step fields.
- If quality-check checkboxes are sent in the body, the confirm endpoint needs a body (PDF Step 5a: the backend checks all 5 checkboxes).

## I. Common errors and fixes

| Error | Cause / Fix |
|---|---|
| 401 Unauthorized | Token missing, expired, or the Authorization header has "Bearer " (with space) wrong. Log in again. |
| 403 Forbidden | Role mismatch (e.g. driver token on a manager endpoint). Or CSRF: if using JWT headers, disable CSRF. |
| 400 Bad Request | Validation failed. Read the response `message` and `errors`. |
| 404 Not Found | Wrong URL or id missing. Match controller `@RequestMapping` and URL. |
| CORS error (in browser) | Match `app.cors.allowed-origin` with React's address. Postman doesn't need CORS, so if Postman works and the browser fails, this is the cause. |
| Communications link failure | Check MySQL is running (Services), port 3306, and the password. |
| Access denied for user root | Wrong password in application.properties. |
| Port 8080 in use | Set `server.port=8081` or stop the old process. |
| Tables not being created | Check `ddl-auto=update`, `@Entity` on the class, and the package is under the main class's package. |

## Connecting React from your local PC

1. Backend runs on http://localhost:8080, React on http://localhost:5173. Run both together.
2. Keep the API base URL in one place in React (`.env`: `VITE_API_URL=http://localhost:8080`). Keep the token in state/memory; a cookie is safer than localStorage.
3. Send `Authorization: Bearer <token>` on every request. On 401, send the user to the login page.
4. Get each feature passing in Postman first, then connect React. If something fails, try the same request in Postman to find out whether the problem is backend or frontend.
5. Git: two separate repos, `sphwms-backend` and `sphwms-frontend` (section B). `git init`, `.gitignore` (backend: `target/`, `.idea/`, `application.properties`, `*.log`; frontend: `node_modules/`, `.env`, `dist/`). Work on `feature/<name>` branches off `develop`, and commit after each feature (`feat: ...`, `fix: ...`, `docs: ...`).

---

# 1. Login

Backend flow of the login page, step by step.

**Step 1: React form**
- The user enters username and password and clicks Log In.
- React sends `POST /api/auth/login` with username and password (over HTTPS).

**Step 2: Controller takes the request**
- `AuthController` receives the request and passes it to `AuthService`.

**Step 3: Find the user in the database**
- `UserRepository` (Spring Data JPA, parameterized query, so no SQL injection) looks up the user by username.
- If not found: "Invalid username or password." (never reveal whether the username or the password was wrong.)

**Step 4: Check account status**
- If the account is Locked or Blocked, login is refused: "Account locked after 5 failed attempts. Please contact the administrator."

**Step 5: Check password**
- `BCryptPasswordEncoder.matches` compares the raw password with `password_hash`. Passwords are never stored in plain text.
- If wrong: `failed_attempts` + 1. At 5, status = Locked. Return "Invalid username or password."

**Step 6: If correct**
- `failed_attempts` = 0, `last_login` = now.
- Add a "Login" entry to the audit log.

**Step 7: Create token**
- Server builds a JWT (user id, role, expiry such as 30 minutes).
- Send the token in an HttpOnly cookie (safer than localStorage); the response also carries name and role.

**Step 8: React redirect**
- Redirect to the dashboard based on role, and show only allowed pages in the 3-dot menu (Admin: user management; Manager: all features; Driver: own profile).

**Step 9: Every later request**
- `JwtFilter` checks the token and places user and role in Spring Security.
- The controller uses `@PreAuthorize` for access control. If the token has expired, 401 is returned and React shows "Session expired - please sign in again." and goes to the login page.

**Step 10: Logout**
- `POST /api/auth/logout` clears the cookie; React goes to the login page.

**What you need**
- Dependencies: Spring Web, Spring Security, Spring Data JPA, MySQL Driver, a JWT library (jjwt).
- Tables: User (user_id, username, password_hash, role, status, failed_attempts, last_login) and AuditLog.
- Config: HTTPS, CORS (allow React's address), CSRF protection (if using cookies).

**Build order**
1. User entity, repository, AuthService (login logic).
2. JWT service and cookie.
3. JwtFilter and Spring Security config.
4. Logout, audit log, CORS/CSRF.

**Remember:** Forgot Password, Profile and Admin Unlock flows also use this same User table.

---

# 2. Products

**Page does:** product list (search, category, warehouse, status filters), Add Product (pop-up), Edit / Delete, Products by Category chart.

**Step 1: Database tables**
- `product`: product_id, code (P001), name, category, purchase_price, selling_price, reorder_level, active.
- `warehouse`: warehouse_id, name, capacity.
- `stock_batch`: batch_id, product_id (FK), warehouse_id (FK), quantity, entry_date, expiry_date.
- `stock_transaction`: txn_id, product_id, batch_id, type, quantity, date, user_id.
- Quantity, Warehouse, Entry date and Expiry date on the form are batch data, not product data. So one form save writes to two tables: product and stock_batch.

**Step 2: Load the list when the page opens**
- React calls `GET /api/products?search=&category=&warehouse=&status=`.
- `ProductController` passes the filters to `ProductService`.
- `ProductRepository` uses a JPQL query with parameters (no SQL injection). Empty filters are not applied.
- For each product, sum stock_batch to build "Available In" (e.g. "A: 900 kg, B: 300 kg").
- Response: code, name, category, purchase, selling, availableIn, entryDate, expiryDate, status.

**Step 3: Status is calculated in the backend**
- Status is not stored in the database; it changes when quantity or dates change. So calculate it every time the list is fetched.
- Order matters: Expired first, then Out of Stock, Near Expiry, Low Stock, and finally In Stock.
- The 7-day threshold comes from alert settings. The status filter applies here too.

**Step 4: Add Product (Save)**
- React sends `POST /api/products` with name, category, warehouseId, purchasePrice, sellingPrice, quantity, entryDate, expiryDate.
- Validation (DTO): required fields; price and quantity positive; expiryDate must be after entryDate. Errors: "Required fields are missing." or "Invalid price - enter a positive number."
- If selling price < purchase price, show a warning (it saves, but a warning is returned).
- Auto-generate code: the backend creates P + 3 digits (P001, P002...).
- `@Transactional`: save product, save stock_batch, log stock_transaction together. If one fails, everything rolls back.
- Reject if the warehouse is inactive or capacity is full.
- Success: "Product saved successfully"; React refreshes the list.

**Step 5: Edit**
- `PUT /api/products/{id}`: name, category, prices can change.
- Editing a price does not change old arrivals or sales, because those store their own prices.

**Step 6: Delete**
- `DELETE /api/products/{id}` after confirmation.
- The SRS says a product with stock records can't be deleted ("Cannot delete: product has stock records."). But Add Product with a quantity creates a stock record, so every product would be blocked. Two options:
  - Soft delete (better): set `active = false`. It disappears from the list but old reports and history stay correct.
  - Or delete only if stock is 0 and there are no sales/donations.

**Step 7: Products by Category chart**
- `GET /api/products/summary/category`: counts active products per category.
- Response: list of category and count (e.g. Grain: 3). React draws the pyramid chart from it.

**Step 8: Security**
- All endpoints need the Manager role (role comes from the JWT cookie).
- HTTPS, and never concatenate user input into queries (use JPA parameters).

**Build order**
1. Product, Warehouse, StockBatch entities and repositories.
2. `GET /api/products` (no filters), then filters and status.
3. `POST` (Add Product) and validation.
4. Category summary endpoint.
5. Edit and Delete (soft delete).

---

# 3. Arrivals

**Page does:** arrival list, 4 count cards at the top with All / Pending / Confirmed / Rejected filters, New Arrival pop-up (creates a Pending record), Quality Check pop-up (Confirm or Reject).

**Step 1: Database tables**
- `arrival`: arrival_id, batch_no (unique), product_id, warehouse_id, quantity, grade, expiry_date, purchase_price (snapshot at save time), total_cost, status (PENDING / CONFIRMED / REJECTED), rejection_reason, created_by, created_at, confirmed_by, confirmed_at, version.
- `quality_check`: check_id, arrival_id (unique), packaging_ok, no_damage, quantity_match, expiry_valid, grade_match, checked_by, checked_at.
- `stock_batch`: batch_id, product_id, warehouse_id, arrival_id, remaining_quantity, expiry_date.
- `stock_transaction`: txn_id, product_id, batch_id, warehouse_id, type (ARRIVAL), quantity, date, user_id.
- The `version` column is needed to prevent double-confirm.

**Step 2: Load list and counts**
- `GET /api/arrivals?status=PENDING` (no status means all): batchNo, product name, warehouse name, quantity, expiryDate, totalCost, status.
- `GET /api/arrivals/summary`: status-wise counts for the 4 cards. Total = sum of the three statuses.
- Pending rows show a Quality Check button; other rows show "-".

**Step 3: New Arrival pop-up**
- Dropdowns: only active products and ACTIVE warehouses. Inactive warehouses don't appear.
- Purchase Price (auto) and Total Cost are previews only. The server recalculates; it does not trust the frontend's numbers.
- Batch No in the pop-up is a preview from `GET /api/arrivals/next-batch-no`. The real batch no is generated at save time (B- + year + sequence), with a unique constraint in the database.

**Step 4: Save Arrival (`POST /api/arrivals`)**
- Validation: quantity positive, grade A/B/C, expiry date in the future, productId and warehouseId required. Errors: "Invalid quantity - enter a positive number." or "Required fields are missing."
- Check the product is active and the warehouse is ACTIVE.
- Copy the product's purchase price into the arrival (snapshot). Later product price changes don't alter old arrivals.
- total_cost = quantity x purchase_price, generate batch no, status = PENDING, created_by = logged-in user.
- Save. Stock does not increase yet.
- Send success message; React refreshes the list.

**Step 5a: Quality Check, Confirm**
- The pop-up has 5 checkboxes, a Confirm Arrival button (enabled only when all are ticked), and Reject Arrival (needs a reason). The backend checks too and does not rely on the frontend.
- `POST /api/arrivals/{id}/confirm`, all inside one `@Transactional`:
  1. Load the arrival with a lock (PESSIMISTIC_WRITE); otherwise two people confirming at once would add stock twice.
  2. If status is not PENDING, stop: "Arrival already confirmed."
  3. Check all 5 checkboxes are true. If not: "Arrival data is invalid - cannot confirm."
  4. Check the warehouse is still Active and that current stock + quantity is within capacity.
  5. Save the quality_check row (which user, when).
  6. Set arrival status CONFIRMED, set confirmed_by, confirmed_at.
  7. Create the stock_batch (remaining_quantity = quantity, expiry_date).
  8. Log a stock_transaction (type = ARRIVAL).
  9. Tell the Alert service so it can check whether a low-stock alert should resolve.
  10. If any step fails, roll everything back: "System error - unable to update inventory."

**Step 5b: Quality Check, Reject**
- `POST /api/arrivals/{id}/reject`: load the arrival with a lock and check status is PENDING.
- If the reason is blank: "Rejection reason is required."
- Save quality_check (it records which items failed).
- Set status REJECTED and save rejection_reason. Stock does not change.

**Step 6: Error handling and security**
- `BusinessException` is caught by `@RestControllerAdvice` and returned as a 400 message. React shows it in the pop-up.
- All endpoints need the Manager role. Arrival save, confirm and reject all go in the audit log (who, when, which arrival).

**Build order**
1. Arrival, QualityCheck, StockBatch entities and repositories.
2. `GET /api/arrivals` and `/summary`.
3. `POST /api/arrivals` (New Arrival) and the batch no generator.
4. Confirm (lock, transaction, stock batch).
5. Reject.
6. Alert recheck and audit log.

**Remember**
- The Add Product form also has Quantity, Warehouse and Expiry date. Per the SRS, stock should increase only from Confirmed arrivals. If both paths exist, stock gets counted two ways and quality check is bypassed.
- Keep one path: when Add Product has a quantity, the backend creates a Confirmed opening-stock arrival (in the same transaction), so all stock records also exist in the arrival table.

---

# 4. Buyers (and Sales)

**Page does:** buyer search (name or phone) and chips, buyer summary (donut, Total Purchase / Paid / Due / Last Purchase), Add Purchase, Receive Due Payment, Buyer info (edit, delete), Purchase History and Export XLSX. Add Purchase means a sale, and a sale reduces stock, so this depends on stock_batch from Products and Arrivals.

**Step 1: Database tables**
- `buyer`: buyer_id, name, phone (unique), address, buyer_type, status (ACTIVE / INACTIVE), created_at, created_by.
- `sale`: sale_id, sale_no (unique), buyer_id, product_id, quantity, rate, purchase_price (for the profit report), total_amount, paid_amount, payment_status (PAID / DUE), status (CONFIRMED / CANCELLED), sale_date, created_by, created_at.
- `sale_allocation`: alloc_id, sale_id, batch_id, warehouse_id, quantity.
- `due_payment`: payment_id, buyer_id, amount, payment_date, received_by.
- `payment_allocation`: id, payment_id, sale_id, amount.
- Money columns are DECIMAL(12,2), and BigDecimal in Java (not double).
- Why sale_allocation: one sale of 100 kg can come from two batches. Storing how much came from which batch lets you return stock exactly if a sale is cancelled.

**Step 2: Search and buyer chips**
- `GET /api/buyers?search=`: only ACTIVE buyers, matched by name or phone.
- If the search box is empty, return a list of recent buyers.
- React debounces the search (300ms).
- Clicking a chip calls `GET /api/buyers/{id}/summary`.

**Step 3: Buyer summary (donut and 4 numbers)**
- Total Purchase = sum(total_amount); Total Paid = sum(paid_amount); Total Due = Purchase - Paid; Last Purchase = max(sale_date).
- Paid % = paid / total x 100 (if total is 0, show 0%, no divide-by-zero).
- Cancelled sales are not counted.
- Calculate these from the database every time. If you keep a due column on the buyer table, it will drift after cancels or payments.

**Step 4: Buyer info pop-up**
- `GET /api/buyers/{id}`: name, phone, address, type.
- Edit: `PUT /api/buyers/{id}`. Checks: required fields ("Required buyer information is missing."), phone format ("Invalid phone number."), duplicate phone ("Phone number already exists."). On success: "Buyer updated successfully".
- Delete: confirm pop-up first. If the buyer has sales, set status = INACTIVE ("Buyer marked inactive"); if no sales, delete.
- Inactive buyers don't appear in dropdowns and chips, but remain in old history and reports.

**Step 5: Add Purchase pop-up**
- Buyer dropdown (active only), Product dropdown (only products with stock). Selecting a buyer auto-fills Phone and Address (read-only).
- Rate = the product's selling price, Total = quantity x rate. This is only a preview. Sale No preview comes from `GET /api/sales/next-no`; the real sale no comes from a database sequence at save time.
- `POST /api/sales` (buyerId, productId, warehouseId, quantity, saleDate, paidNow). The backend, inside `@Transactional`:
  1. Validate: quantity positive, paidNow >= 0, date not in the future.
  2. Check buyer, product and warehouse are active.
  3. Get prices on the server: it doesn't trust the frontend's rate/total. Copy selling price and purchase price into the sale. total = quantity x rate.
  4. If paidNow > total: "Paid amount cannot be more than the total."
  5. Lock stock: fetch the batches for that product and warehouse in expiry-date order (earliest expiry first) with a PESSIMISTIC_WRITE lock. Expired batches can't be sold from.
  6. If the sum of remaining quantities is less than the quantity: "Insufficient stock for this sale."
  7. Deduct quantity from batches, save sale_allocation rows, update inventory.
  8. paid_amount = paidNow; payment_status = PAID if paid = total, otherwise DUE.
  9. Save the sale, log stock_transaction (SALE), audit log.
  10. If any step fails, roll everything back: "System error - unable to save sale."
  11. Success: "Sale recorded successfully". React refreshes history and summary.
- When stock drops, a low-stock alert may be created, so trigger the alert recheck.

**Step 6: Receive Due Payment**
- The pop-up shows the buyer's total due; the Amount field defaults to the due amount.
- `POST /api/buyers/{id}/payments`, `@Transactional`:
  1. Check the amount is positive.
  2. Lock all of the buyer's DUE sales, oldest first (sale_date ascending).
  3. Total due = sum(total - paid). If amount > due: "Payment amount exceeds due amount."
  4. Split the amount across the oldest first: settle the first sale's remainder first, then carry any leftover to the next sale. Fully paid sales become PAID.
  5. Save a due_payment row and payment_allocation rows.
  6. If the buyer's due becomes zero, the due alert is Resolved.
  7. Success: "Due payment recorded".
- Why lock: if two people pay at the same time, the same due could be reduced twice.

**Step 7: Purchase History**
- `GET /api/sales?buyerId=&from=&to=`. If the "All buyers" checkbox is ticked, don't send buyerId.
- Response: saleNo, date, buyer name, product, quantity, rate, total, payment status.
- If from > to: "Invalid date range." Paginate large lists.
- If there are no sales: "No buyers found." or "No purchases yet."

**Step 8: Export XLSX**
- `GET /api/sales/export`: use exactly the same filters as the screen, so file numbers match the screen.
- Use Apache POI (XSSFWorkbook): header row, data rows, total at the end.
- Content-Type is xlsx, Content-Disposition attachment, filename `purchase-history.xlsx`. React downloads it as a blob.
- No data: "No data to export." Failure: "Export failed - please try again."

**Step 9: Security**
- All endpoints need the Manager role. Sale save, payment, buyer edit/delete go in the audit log.
- HTTPS, and all queries use parameters (JPA).

**Build order**
1. Buyer entity, repository, search.
2. Buyer info, edit, delete (soft).
3. Sale entity and `POST /api/sales` (with stock locking). This is the biggest piece; finish Products and Arrivals first.
4. Summary endpoint (donut and 4 numbers).
5. Purchase History and date filter.
6. Receive Due Payment.
7. Export XLSX.

**Remember**
- There's no Warehouse field in the design: keep a Warehouse dropdown (with available quantity) in Add Purchase, otherwise the backend can't tell which warehouse to deduct from.
- No Add Buyer button: add "+ New Buyer" using `POST /api/buyers`.
- If Paid now is partial, status stays "Due" and the rest goes to due.
- Sale cancel is not in the design but is in the SRS. You can add a "Cancel" button in the history row and `POST /api/sales/{id}/cancel`, using sale_allocation to return stock.

---

# 5. Donations

**Page does:** search (product, receiver, donation no), Eligible for Donation list (1-2 days left), Add Donation pop-up (with Quick Add Receiver), Donated by Receiver (Venn diagram), Donation History, print receipt, Export XLSX. Donation reduces stock, so it depends on stock_batch from Arrivals.

**Step 1: Database tables**
- `receiver`: receiver_id, name, contact_person, phone, address, type (NGO / ORPHANAGE / MOSQUE_MADRASA / OTHERS), status (ACTIVE / INACTIVE), created_at, created_by.
- `donation`: donation_id, donation_no (unique, D-0021), receiver_id, batch_id, product_id, warehouse_id, quantity, purchase_price (at donation time), total_value, condition_note, safe_confirmed, status (CONFIRMED / CANCELLED), cancel_reason, donation_date, donated_by, created_at.
- Already existing: stock_batch, stock_transaction (type = DONATION / DONATION_CANCEL), alert_setting (donation_days, default 2).
- One donation covers one batch. To donate from two batches, make two donations.

**Step 2: Eligible list**
- `GET /api/donations/eligible?search=`: remaining > 0 and expiry between today+1 and today+donation_days.
- Batches expiring today (0 days) and already expired batches don't appear (not eligible per the SRS).
- left = expiryDate - today. The frontend shows "1 day" in orange (urgent) and "2 days" in light blue.
- Response: batchId, product, batch no, warehouse, remaining qty, expiry, daysLeft. If empty: "No items nearing expiry."
- Clicking the gift icon opens the Add Donation pop-up with that batch pre-selected.

**Step 3: Receiver dropdown and Quick Add Receiver**
- Dropdown: `GET /api/receivers?status=ACTIVE`.
- Quick Add: `POST /api/receivers` (name, contactPerson, phone, address, type).
- Required fields ("Required receiver information is missing."), phone format ("Invalid phone number.").
- It returns the new receiver and React auto-selects it in the dropdown, so the donation form's data isn't lost.
- Success: "Receiver saved successfully".

**Step 4: Add Donation (Confirm Donation)**
- The Confirm Donation button enables only when the checkbox is ticked, but the backend checks it too.
- `POST /api/donations` (batchId, quantity, receiverId, conditionNote, safeConfirmed). `@Transactional`:
  1. Validate: quantity positive, receiverId and batchId required.
  2. If safeConfirmed is not true: "Please confirm the food is safe to eat."
  3. Lock the batch (PESSIMISTIC_WRITE) so two people can't donate the same batch.
  4. Re-check eligibility. If not eligible: "This product is not eligible for donation." (The day may have changed while the pop-up was open.)
  5. If quantity > remaining: "Selected quantity is not available."
  6. Check the receiver is ACTIVE.
  7. Reduce the batch's remaining_quantity and warehouse inventory.
  8. Generate donation no (D- + sequence), copy purchase_price from the product, total_value = quantity x purchase_price.
  9. Save the donation, log stock_transaction (DONATION), audit log.
  10. If the batch's remaining becomes 0, its near-expiry alert is Resolved.
  11. On failure, roll back: "System error - donation could not be saved."
  12. Success: "Donation recorded successfully". React refreshes the list, chart and history.
- This is not a stock loss; it is booked as a donation. Reports show donation value separately and it's not added to wastage.

**Step 5: Donated by Receiver (Venn diagram and legend)**
- `GET /api/donations/summary/by-receiver`: sum of quantity per receiver and product (CONFIRMED only).
- Legend: each receiver's total kg and percent (e.g. Hope 330 / total 590 = 56%). If total is 0, 0%.
- Venn data: the set of products for each receiver. Where a product name is in two sets, show the overlap.
- Response: totalKg and a receivers list (name, kg, percent, products).
- Cancelled donations aren't counted. A Venn looks good only with 3 sets. With 4 or more receivers, a bar chart in React is better.

**Step 6: Donation History and search**
- `GET /api/donations?search=&receiverId=&from=&to=&page=&size=`.
- Search by product name, receiver name or donation no. The receiver dropdown and From/To date filters work together. If from > to: "Invalid date range."
- Response: donationNo, date, product, quantity, receiver name, phone, status. If none: "No donations found."
- Summary: total donation count, total quantity, total value, receiver-wise total.

**Step 7: Print Receipt**
- `GET /api/donations/{id}/receipt`: the server builds a PDF (OpenPDF or iText).
- Receipt contents: donation no, date, product, batch no, quantity, receiver name, contact, phone, address, food condition note, who donated.
- Content-Type `application/pdf`. React opens it in a new tab and prints.

**Step 8: Export XLSX**
- `GET /api/donations/export`: same filters as history so the figures match the screen.
- Apache POI sheet with total quantity and total value at the end.
- No data: "No data to export." Failure: "Export failed - please try again."

**Step 9: Security and audit**
- All endpoints need the Manager role. Donation save and cancel go in the audit log. All queries use JPA parameters.

**Build order**
1. Receiver entity, repository, `GET` and `POST /api/receivers`.
2. `GET /api/donations/eligible`.
3. `POST /api/donations` (batch lock, transaction). This is the biggest piece; finish Arrivals first.
4. History list, search, date filter.
5. By-receiver summary (Venn and legend).
6. Print receipt (PDF).
7. Export XLSX.

**Remember**
- Donation cancel is not in the design but is in the SRS. Add a "Cancel" button in the history row and `POST /api/donations/{id}/cancel` (with a reason). The backend adds back remaining_quantity, sets status CANCELLED, and excludes it from summaries.
- No receiver management page: add a small list pop-up for editing or deactivating, using `PUT /api/receivers/{id}`.
- After a donation, Products' "Available In" and Warehouses' stock amount drop automatically, but that isn't this page's job.

---

# 6. Reports

This only calculates and shows data after the fact, so no new tables are needed. It sums data from Sale, Arrival and Donation.

**Page does:** calendar icon (period), home icon (warehouse filter), 4 cards (Total Revenue, Total Expense, Net Profit, Donated Value), Revenue by Category (donut), Top 5 Products (pyramid), Printer and Export XLSX.

**Step 1: Where data comes from**
- Total Revenue: sale and sale_allocation, CONFIRMED sales' quantity x rate.
- Total Expense: arrival, CONFIRMED arrivals' total_cost.
- Net Profit: Revenue - Expense.
- Donated Value: donation, CONFIRMED donations' total_value.
- Revenue by Category and Top 5: sale_allocation + product, revenue by category/product.
- Cancelled sales, Pending or Rejected arrivals, and Cancelled donations are not counted.
- For warehouse-wise revenue, use sale_allocation (not sale), because one sale can come from two warehouses' batches. Revenue = allocation.quantity x sale.rate.

**Step 2: Endpoints**
- `GET /api/reports/summary` - the 4 cards.
- `GET /api/reports/revenue-by-category` - donut.
- `GET /api/reports/top-products` - pyramid.
- `GET /api/reports/export` - XLSX file.
- All endpoints take filters: `from`, `to`, `warehouseId` (optional). React calls the three together and calls them again when filters change.

**Step 3: Period filter (calendar icon)**
- React sends month/year or From-To. With nothing, the default is the current month.
- Check from <= to. If wrong: "Invalid date range."
- Use an inclusive date range. For datetime columns (arrival confirmed_at), use >= from 00:00 and < (to + 1 day) 00:00, otherwise the last day's data is missed.
- Which date: Sale = sale_date, Arrival (expense) = confirmed_at (cost counts when goods are confirmed), Donation = donation_date.

**Step 4: Warehouse filter (home icon)**
- Without warehouseId, include all warehouses. It's an optional filter in the queries.
- Old data of inactive warehouses still appears in reports, since history must stay correct.

**Step 5: Summary cards**
- Revenue, Expense and Donated Value come from separate database SUMs. If there is no data, use 0 (coalesce), not null.
- Net Profit = Revenue - Expense (in the service). It can be negative; showing it in red or with "-" in the frontend is good.
- Response: revenue, expense, netProfit, donatedValue.

**Step 6: Revenue by Category (donut)**
- Category-wise sum(allocation quantity x rate), largest to smallest.
- Total = sum of all categories. If total is 0, percent is 0 and the chart shows "No data".
- Percent = category / total x 100. Rounding can make the sum 99 or 101. Rule: round all, then add or subtract the error on the largest slice so the sum is 100.
- Also send the total in the response for the number in the donut's center.

**Step 7: Top 5 Products (pyramid)**
- Per product: revenue, quantity sold, cost, profit; order by revenue desc, then name. The service fetches just 5.
- If fewer than 5 products were sold, fewer layers are shown.
- profit = revenue - cost. Cost comes from the purchase_price saved at sale time, not the product's current price, so changing prices later doesn't change old reports.
- Backend order has rank 1 first. In the pyramid rank 1 is at the bottom (wide layer), so the frontend reverses it.

**Step 8: Money format**
- The backend always sends numbers (BigDecimal), not strings.
- React formats lakh style (6,40,000) with the en-IN locale, and "Tk 6.4L" via amount/100000. If the backend formats, the XLSX and chart numbers won't match.

**Step 9: No data**
- Cards show 0, donut and pyramid are empty, and the page shows "No data available for the selected period." This is not an error; return HTTP 200.
- For server problems: "Unable to load report data."

**Step 10: Print**
- Browser print (simple): React calls `window.print()`; print CSS hides sidebar/buttons/bell. Nothing needed in the backend.
- Server PDF: `GET /api/reports/pdf` (OpenPDF), only if needed later.
- Start with the first.

**Step 11: Export XLSX**
- Use the same service methods (summary, revenue-by-category, top-products). Writing separate queries would make file numbers differ from the screen.
- Apache POI, 3 sheets: Summary, Revenue by Category, Top 5 Products. Write the filters (period, warehouse) at the top.
- Money is stored as number cells (not text). Content-Type xlsx, filename `report-2026-11.xlsx`.
- If all data is zero: "No data to export." Failure: "Unable to generate the export file." and React shows "Export failed - please try again." Success: "Report exported successfully".
- (Optional) `report_export_log` table: report_type, period, exported_by, exported_at.

**Step 12: Security**
- All endpoints need the Manager role (only the manager sees financial reports). Other roles get 403 and React shows "You are not allowed to view this report." Exports go in the audit log.

**Step 13: Performance**
- Queries run in read-only transactions. Indexes: sale(sale_date, status), arrival(confirmed_at, status), donation(donation_date, status), sale_allocation(warehouse_id).
- Do the math with database SUMs, not by loading all rows into Java. No caching (the report must refresh after a sale or arrival).

**Build order**
1. ReportFilter and date validation.
2. Revenue, expense, donatedValue queries, then `GET /summary`.
3. revenueByCategory query and percent calculation.
4. topProducts query (top 5).
5. Export XLSX (reuse the service methods).
6. Indexes, 403 handler, audit log.

**Remember**
- Net Profit = Revenue - confirmed arrival cost. This is a cash-style figure, not true profit. Product-wise profit (Step 7) is more accurate.
- Transport cost is not currently part of Total Expense. To include it, add the trip cost sum to expense (the trip needs a warehouse field for the warehouse filter).
- The SRS has these but the screen doesn't: monthly revenue vs expense, product-wise table, warehouse-wise comparison, buyer due report, stock value report, Total Due card. These need more endpoints (`/monthly`, `/by-warehouse`, `/due`, `/stock-value`).
- The calendar and home icons have no labels: showing the selected period and warehouse next to the icons is good (e.g. "Nov 2026 - All Warehouses").

---

# 7. Alerts

**Alert types:** Low Stock, Near Expiry, Due Payment, License Expiry.
**Page has:** 4 cards, Alerts by Type donut, alert table (search, filter, paging), bell badge, Mark All as Read, eye icon, gear icon (settings).

**Step 1: Where data comes from**
- Low Stock: for a product and warehouse, remaining quantity sum <= reorder level (0 kg also counts).
- Near Expiry: remaining > 0 and expiry_date <= today + N days (default 7).
- Due Payment: CONFIRMED sale's due > 0 and the due date has passed, summed per buyer.
- License Expiry: active driver's license_expiry <= today + 30 days.
- Warehouse column: present for Low Stock and Near Expiry. Due Payment and License Expiry have none, so send null and React shows "-".

**Step 2: New alert table**
- Columns: id, type, entity_type (PRODUCT / BATCH / BUYER / DRIVER), entity_id, warehouse_id, item_name (for search), details ("40 kg <= reorder level 50 kg"), dedupe_key (unique), is_read, read_at, read_by, created_at, resolved_at.
- `is_read` and `resolved_at` are separate. "Read" = the user has seen it; "resolved" = the problem is gone. Cards and donut count only those with resolved_at null, whether read or unread.
- dedupe_key formats: `LOW_STOCK:productId:warehouseId`, `NEAR_EXPIRY:batchId`, `DUE_PAYMENT:buyerId`, `LICENSE_EXPIRY:driverId`.

**Step 3: Creating alerts (refresh job)**
- Users don't create alerts; the system does. Two triggers: (1) a scheduled job every 15 minutes, (2) an event when a Sale, Arrival or Donation is confirmed, for an immediate refresh.
- The job finds the live conditions of the 4 types and upserts each one. For active alerts whose condition no longer holds, it sets resolved_at.
- Upsert rule: if the key doesn't exist, create a new row (unread). If it exists and is resolved, reopen it (resolved_at null, is_read false). If it exists and is active, only update details and item_name; don't change read status.
- Near Expiry also includes batches that have already expired. In details write "Expired" or "Expires in 3 days".

**Step 4: Endpoints**
- `GET /api/alerts/summary` - 4 cards, total, unread.
- `GET /api/alerts/by-type` - donut.
- `GET /api/alerts` - table (search, filter, paging).
- `GET /api/alerts/unread-count` - bell badge.
- `PATCH /api/alerts/{id}/read` - tick icon.
- `PATCH /api/alerts/read-all` - Mark All as Read.
- `GET /api/alerts/{id}` - eye icon.
- `GET` / `PUT /api/alerts/settings` - gear icon.
- After marking read, call summary, by-type and the list again, otherwise the cards and bell stay stale.

**Step 5: Summary cards**
- Count of unresolved alerts per type. For a type with no alerts, send 0, not null.
- Response: lowStock, nearExpiry, duePayment, licenseExpiry, total, unread.

**Step 6: Alerts by Type (donut)**
- Always send all 4 types (even with count 0). Fixed order: Low Stock, Near Expiry, Due Payment, License Expiry.
- If total is 0, percent is 0 and the donut shows "No alerts".
- Rounding rule is the same as Reports: round all, then add or subtract the error on the largest slice so the sum is 100.

**Step 7: Alert list (table)**
- Filters: type (optional), status (UNREAD / READ), q (item_name search, trimmed, null if empty), page and size (default 5, max 50).
- Order: unread first, then newest first, then id desc (so the order doesn't flip between pages).
- Response: items (id, type, item, warehouse, details, read), page, size, totalElements, totalPages.
- Spring pages are 0-based and React's are 1-based: the controller uses page - 1. React builds "Showing 1-5 of 13".
- Searching doesn't change the cards or donut; they count all active alerts.

**Step 8: Marking read**
- `PATCH /api/alerts/{id}/read`: idempotent (200 even if already read). Sets read_at and read_by. The response includes the new unread count so the bell can update.
- `PATCH /api/alerts/read-all`: reads all unread, unresolved alerts. Response: updated count. If none are unread, return 0, not an error. Message: "All alerts marked as read."
- Read doesn't mean delete. Card and donut counts stay the same; only the Status changes from "Unread" to "Read".

**Step 9: Eye icon (detail)**
- `GET /api/alerts/{id}` returns the whole alert plus entityType and entityId. React uses these to go to the related page: PRODUCT/BATCH goes to Products or the stock page, BUYER to Buyers, DRIVER to Drivers.
- Whether the eye click auto-marks it read is your decision; keep the tick separate.

**Step 10: Settings (gear icon)**
- `alert_setting` (key, value): `near_expiry_days = 7`, `license_expiry_days = 30`. Reorder level is a field on the product itself.
- When a setting changes, run the refresh job again, otherwise alerts for the new threshold appear only after 15 minutes.

**Step 11: No data and errors**
- With no alerts: HTTP 200, empty list, cards show 0, donut "No alerts", table "No alerts found."
- Server error: "Unable to load alerts." Missing alert id: 404.

**Step 12: Security**
- All endpoints need the Manager role. Other roles get 403 and React shows "You are not allowed to view alerts."
- Store read_by on read and read-all.
- Right now read status is global (when one person reads it, it's read for everyone). With more than one manager you need an `alert_read(alert_id, user_id)` table.

**Step 13: Performance**
- Indexes: alert(resolved_at, is_read), alert(type), alert(dedupe_key) unique. Also stock_batch.expiry_date and driver.license_expiry.
- The bell's unread-count is polled every 60 seconds. If needed later, use SSE/WebSocket.
- With more than one server instance, the refresh job can run twice at once. The unique dedupe_key prevents duplicates, but ShedLock is better. Catch the duplicate-key exception and ignore it.
- No caching.

**Build order**
1. Alert table, entity, repository.
2. Refresh job: Low Stock first, then Near Expiry, Due Payment, License Expiry.
3. `GET /summary`, `/by-type`, `/unread-count`.
4. `GET /alerts` (search, filter, paging).
5. Read and read-all.
6. Detail and settings.
7. Event-triggered refresh on confirm, indexes, 403 handler.

**Remember**
- The bell's unread count and the card's total are two different numbers; that is correct.
- There's no warehouse filter: currently all warehouses' alerts show. If a manager is tied to a specific warehouse, scope by the user's warehouse.
- Due Payment here is per buyer (4 buyers). For per-sale alerts use the key `DUE_PAYMENT:saleId`, but the count will grow.
- Event triggers are needed alongside cron; otherwise a low-stock alert appears 15 minutes after a sale.

---

# 8. Transport Cost

**Page does:** + Add Trip Cost, printer icon, chart "Transport Cost by Month" (6 months), table (Trip, Date, Driver, Route, Fuel, Driver Cost, Other, Total, Actions), search (trip no. or driver), Export XLSX, pagination (5 per page), Add modal.

**What the screenshot tells us**
- The dropdown shows only Delivered trips that don't have a cost yet.
- Total = Fuel + Driver + Other (e.g. 3200 + 1500 + 300 = 5000).
- The chart's months are based on the trip's date, not the cost entry's date.
- The bell number was shown differently on different pages; a single bell should show one number (see Remember).

**Step 1: Where data comes from**
- Chart: trip_cost + trip, month-wise SUM of total_cost by trip date.
- Table: trip_cost, trip, driver; one row per trip.
- Dropdown: trips with status DELIVERED and no cost.
- Route: origin - destination ("Dhaka - Gazipur").
- Rule: one cost row per trip (unique). Only DELIVERED trips can have a cost. Both the date column and the chart use the trip date.

**Step 2: New table (trip_cost)**
- Columns: id, trip_id (unique, FK), fuel_cost, driver_cost, other_cost, total_cost, note, created_by, created_at, updated_by, updated_at. Check: all three costs >= 0.
- The backend calculates total_cost itself (fuel + driver + other). It doesn't accept React's total, otherwise a wrong total can reach the DB. This keeps chart and export SUMs always consistent.

**Step 3: Endpoints**
- `GET /api/trip-costs/monthly` - bar chart.
- `GET /api/trip-costs?q=&page=&size=` - table.
- `GET /api/trip-costs/available-trips` - modal dropdown.
- `POST /api/trip-costs` - Save Trip Cost.
- `PUT /api/trip-costs/{id}` - edit. `DELETE /api/trip-costs/{id}` - delete.
- `GET /api/trip-costs/export?q=` - Export XLSX.
- After add, edit or delete, call monthly, list and available-trips again.

**Step 4: Chart (Transport Cost by Month)**
- The last 6 months including the current month. Send months with no cost as 0 too, so the bar positions and labels stay correct. Works across years.
- Use the Asia/Dhaka zone. If the server is in UTC, the month can be wrong after night time on the 30th.
- Search doesn't affect the chart. If all 6 months are 0: HTTP 200, and React shows "No transport cost data."
- The backend sends exact numbers (15150.00); the frontend builds "Tk 15.2K".

**Step 5: Trip Costs table**
- `q` searches trip no or driver name; order by trip date desc, then id desc. q is trimmed, empty becomes "". Size default 5, max 50.
- Spring pages are 0-based, React's 1-based. React builds "Showing 1-5 of 17".
- A page outside the range returns HTTP 200 with an empty list. If the last row of the last page is deleted, React steps back one page.
- Searching returns React to page 1, with a 300ms debounce.
- Trips of old, inactive drivers also appear, to keep history correct.
- Send dates as ISO; the frontend builds "15 Nov". Showing the year is good for trips from other years.

**Step 6: Add Trip Cost (modal)**
- Dropdown: DELIVERED trips with no cost, newest first. React label: `T-032 - Sohel Rana (Dhaka - Narayanganj)`. If empty: "No delivered trips without cost." and Save is disabled.
- Save: `POST /api/trip-costs` (tripId, fuelCost, driverCost, otherCost, note). Blank/null costs are treated as 0.
- Validation: trip exists ("Trip not found."), is DELIVERED ("Only delivered trips can have a cost."), no existing cost ("Cost already added for this trip.").
- If negative: "Amount cannot be negative." If all three fields are 0, it won't save: "Total cost must be greater than zero." One field being 0 is fine.
- If two managers save at the same time, the database unique constraint stops it and it's shown as "Cost already added for this trip."
- The live "Total: Tk X" in the modal is frontend-only. It goes in the audit log.
- Success: "Trip cost saved." HTTP 201. Errors show inside the modal, and the modal doesn't close.

**Step 7: Edit and Delete**
- Edit: only fuel, driver, other and note can change. The trip can't change (the dropdown is disabled in the edit modal). If the trip is wrong, delete and add again. Set updated_by, updated_at. Success: "Trip cost updated." Missing id: 404 "Trip cost not found."
- Delete: React confirms first ("Delete this trip cost?"). It's a hard delete, but the audit log keeps the old fuel/driver/other/total. After deletion the trip returns to the dropdown. Success: "Trip cost deleted."

**Step 8: Export XLSX and Print**
- Export: uses the same service method as the list but without pagination (all matching rows), including q if present. Apache POI, one sheet "Trip Costs". Columns: Trip, Date, Driver, Route, Fuel, Driver Cost, Other, Total, Note, and a Total row at the end.
- Money cells are numbers, date cells are dates. Filename `trip-costs-2026-11-23.xlsx`.
- No rows: "No data to export." Failure: "Unable to generate the export file." and React shows "Export failed - please try again." Success: "Trip costs exported successfully". Export goes in the audit log.
- Print: browser `window.print()`, with print CSS hiding sidebar/buttons/bell. Print shows only the 5 rows of the current page; use Export XLSX for all rows.

**Step 9: No data and errors**
- With no trip costs: HTTP 200, empty list, "No trip costs found.", chart "No transport cost data."
- Server error: "Unable to load transport cost data." Save errors show inside the modal.

**Step 10: Security and performance**
- All endpoints need the Manager role. Other roles get 403 and React shows "You are not allowed to view transport costs." Create, update, delete and export go in the audit log.
- Indexes: trip(status, trip_date), trip_cost(trip_id) unique. Chart and totals use database SUMs. No caching. Queries run in read-only transactions.

**Build order**
1. trip_cost table, entity (with the total calculation), repository.
2. `GET /available-trips` and `POST` (with validation).
3. `GET /trip-costs` (search, paging).
4. `GET /monthly` (6 months, empty months as 0).
5. `PUT` and `DELETE`.
6. Export XLSX (reuse the list's service method).
7. Index, 403 handler, audit log.

**Remember**
- Bell: the layout has one bell, so every page must get its number from `GET /api/alerts/unread-count`, never hardcode it.
- Reports' Total Expense doesn't include transport cost right now. To add it, sum by trip date in Reports.
- In Excel, `#,##,##0` does not give lakh-style grouping (it gives 3-3 grouping). `#,##0` or `#,##0.00` is fine for trip cost. For lakh style in Reports you need a custom conditional format.
- Date = trip date: if you add the cost of an old trip later, it adds to that trip's month in the chart. That's correct, but tell the manager.
- One cost per trip: if there's a second expense, edit and add it to Other. If you need multiple cost lines, you need a separate `trip_cost_item` table.

---

# 9. Warehouses

**Page does:** + Add Warehouse, search (name, code, location), status dropdown (All / Active / Inactive), cards (name + code WH-01, location, in-charge, capacity kg, stock amount Tk, progress bar, % used, badge), edit, deactivate/activate, detail modal, Add modal. No pagination, no delete button.

**What the screenshot tells us**
- A: 12,000 kg x 62% = 7,440 kg used, 4,560 kg free.
- D is inactive with stock Tk 0: stock must be empty before deactivating (rule in Step 7).
- Next code is WH-05, because 4 warehouses already exist. In-charge is a text input.
- Put the placeholder "e.g. Warehouse E" in the modal so it isn't confused with existing names.

**Step 1: Where data comes from**
- Code, Name, Location, In-charge, Capacity, Status: columns of the warehouse table.
- Used space (kg): per warehouse SUM(remaining_quantity) from stock_batch.
- Free space: capacity - used, never below 0. Used % = used / capacity x 100.
- Stock amount (Tk): SUM(remaining_quantity x unit_cost). It uses purchase cost, not selling price. Batches with remaining 0 are not counted.

**Step 2: Table (warehouse)**
- Columns: id, code_number, code (WH-01), name, location, in_charge, capacity_kg, active, created_by, created_at, updated_by, updated_at.
- Unique: code_number, code, name. Check: capacity > 0.
- Don't store used space or stock amount as columns (they go stale); always compute from stock_batch.
- MySQL collation is case-insensitive, so "warehouse a" and "Warehouse A" are treated as duplicates.
- There's no delete, so a warehouse row is never removed. History in Arrivals, Sales and Reports stays correct.

**Step 3: Endpoints**
- `GET /api/warehouses?q=&status=ALL` - card list.
- `GET /api/warehouses/{id}` - detail modal.
- `GET /api/warehouses/next-code` - "Code (auto)" in the Add modal.
- `POST /api/warehouses` - Save. `PUT /api/warehouses/{id}` - edit.
- `PATCH /api/warehouses/{id}/status` - deactivate / activate.
- `GET /api/warehouses/options?includeInactive=false` - dropdowns for other pages (Arrival, Donation, Sale, Reports). Reports uses `includeInactive=true`, because warehouses with old data must remain in the filter.

**Step 4: Card list**
- status: ALL (default), ACTIVE, INACTIVE. q searches name, code or location (trimmed). Status filter and search work together.
- Order: code_number asc. No pagination; return everything.
- Fetch all warehouses' stock in one grouped query (not a separate query per card), then merge with the warehouses. No N+1.
- Response (same for list and detail): id, code, name, location, inCharge, capacityKg, usedKg, freeKg, usedPercent, stockAmount, status.
- Percent: if capacity is 0, percent is 0. If it is 99.6% full, don't show "100%" (show 99).
- The backend sends numbers; the frontend builds "Tk 14,20,000" with en-IN. If there are no warehouses: HTTP 200, "No warehouses found."

**Step 5: Add Warehouse**
- Next code: `GET /next-code` is for display only. The backend assigns the real code at save time and ignores the code React sends.
- Save: `POST /api/warehouses` (name, location, inCharge, capacityKg, status). Trim name/location/in-charge, and collapse extra spaces in the name to one.
- Duplicate name (case-insensitive): "Warehouse name already exists." Code = WH- + 2 digits (after 99 comes WH-100). If two managers save at once, the unique constraint conflict gives: "Could not save. Please try again."
- A new warehouse has no stock, so used is 0 and stock amount is 0. It goes in the audit log.
- Messages: "Warehouse name is required.", "Location is required.", "In-charge is required.", "Capacity must be greater than zero."
- Success: "Warehouse saved." HTTP 201. Errors show inside the modal.

**Step 6: Edit**
- `PUT /api/warehouses/{id}`: name, location, in-charge, capacity can change. Code doesn't change (read-only), because Arrival and Report reference it.
- Status is not changed through PUT, only through PATCH (so the deactivate rule lives in one place).
- When the name changes, exclude itself in the duplicate check.
- Capacity can't go below current stock: "Capacity cannot be less than current stock (7,440 kg)."
- Set updated_by, updated_at. Success: "Warehouse updated." Missing id: 404 "Warehouse not found."

**Step 7: Deactivate / Activate**
- `PATCH /api/warehouses/{id}/status` (ACTIVE / INACTIVE). The rule (your decision, can be changed):
  - Deactivate is allowed only if stock is 0 and there are no Pending arrivals. Otherwise: "Cannot deactivate: warehouse still has stock." or "Cannot deactivate: pending arrivals exist."
  - Activate is always allowed. If it's already in that status, return HTTP 200 (idempotent).
- React confirms first ("Deactivate Warehouse D?"). Tooltip "Deactivate" / "Activate".
- The row is locked, because between the check and the save another manager could confirm an arrival and stock could land in an inactive warehouse. Arrival confirm also takes the lock and checks the warehouse is active.

**Step 8: Detail modal**
- `GET /api/warehouses/{id}`: Code, Location, In-charge, Capacity, used kg + percent, free kg, stock amount, progress bar. The card's data can fill the modal, but calling again on click keeps stock fresh. The edit icon opens the Edit modal.

**Step 9: No data and errors**
- With no warehouses: HTTP 200, "No warehouses found." Server error: "Unable to load warehouses." Missing id: 404. Save errors show inside the modal.

**Step 10: Security**
- All endpoints need the Manager role. Other roles get 403 and React shows "You are not allowed to view warehouses."
- `GET /options` is used by other pages, so check its role against the SRS.
- Create, update and status change go in the audit log (on edit, keep the old and new capacity). Set created_by and updated_by.

**Step 11: Performance**
- Indexes: warehouse(active), stock_batch(warehouse_id, remaining_quantity). Calculations use database SUMs. No caching. Read-only transactions.

**Build order**
1. Warehouse entity and columns (code_number, code, audit columns), unique constraints.
2. `GET /warehouses` (search, status, stock totals) and `GET /{id}`.
3. `GET /next-code` and `POST` (with code creation).
4. `PUT` (with the capacity check).
5. `PATCH /status` (stock and pending arrival checks, lock).
6. `GET /options`, active checks in Arrival/Donation/Sale.
7. Index, 403 handler, audit log.

**Remember**
- Block inactive warehouses in the backend too. If Arrival, Donation and Sale backends don't check for active warehouses, stock can be pushed into an inactive warehouse through the API.
- Capacity check in Arrival: at confirm time, if used + new quantity > capacity, it can go over 100%. React clamps the bar at 100%.
- There's one bell, so get the number from `GET /api/alerts/unread-count` in the layout, not a different number per page.
- In Reports' warehouse filter, use `GET /options?includeInactive=true`.
- In the Alerts job, only consider active warehouses for low stock, otherwise inactive warehouses with 0 stock create noise alerts for every product.
- In-charge is text. To link it to a user later, you'll need an `in_charge_user_id` column.
- No delete: deactivate is the only route, because deleting would break history foreign keys.

---

# 10. Drivers

**Page does:** + Add Driver, search (name, phone, license), status dropdown (All / Available / Not Available), cards (avatar, name, License, Phone, Vehicle, badge), edit, deactivate/activate, pagination (6 per page, "Showing 1-6 of 14 drivers").

**What the screenshot tells us**
- 14 drivers, 6 per page: 3 pages. Order is id asc (not alphabetical, not by status). A new driver goes to the last page.
- Not Available = inactive. Vehicle = type (Truck, Pickup, Van) + number, as two separate columns.
- Phone `01711-000001`: the dash is display only; store digits only in the DB.
- The Add/Edit modal isn't in the screenshot, so the fields are a guess: name, license, phone, vehicle type, vehicle number.

**Step 1: Where data comes from**
- Name, License, Phone, Vehicle: columns of the driver table. Badge: `driver.active` (true = Available, false = Not Available). Avatar: the first letter of the name, built by the frontend. No SUMs from other tables are needed.

**Step 2: Table (driver)**
- Columns: id, name, license_no (unique), license_expiry (NULL allowed, for Alerts), phone (unique, 11 digits), vehicle_type (TRUCK / PICKUP / VAN), vehicle_no, active, created_by, created_at, updated_by, updated_at.
- license_expiry is NULL for old drivers; when it's NULL, no alert is created. This field must be added to the Add/Edit modal.
- Name is not unique. vehicle_no is not unique (one vehicle can be driven by two drivers); add a unique constraint if you want one (your decision).
- No delete; rows are never removed, so Transport Cost's old trip history stays correct.

**Step 3: Endpoints**
- `GET /api/drivers?q=&status=ALL&page=1&size=6` - card list.
- `GET /api/drivers/{id}` - fills the Edit modal. `POST /api/drivers` - Add. `PUT /api/drivers/{id}` - edit.
- `PATCH /api/drivers/{id}/status` - block / tick icon.
- `GET /api/drivers/options?includeInactive=false` - wherever a driver is chosen for a trip.
- After Add, Edit or a status change, call the list again. If the filter is "Available" and a card is deactivated it disappears from the list, so if the page becomes empty, React steps back one page.

**Step 4: Card list**
- status: ALL, ACTIVE, INACTIVE ("Available" = ACTIVE, "Not Available" = INACTIVE). q searches name, license or phone (trimmed). Size default 6, max 50. Order id asc.
- If q contains only digits, dashes, spaces or +, treat it as a phone search (digits only). If it contains letters, turn phone search off. Reason: if the digits are empty, every row matches, and typing "DH-11" would match "11" in every phone number.
- Frontend format: phone `01711-000001`, vehicle `Truck DH-11-2201`, "Showing 1-6 of 14 drivers".
- Avatar color should be fixed by `id % palette.length`, otherwise the color changes when the page changes.
- totalElements is the count after filter and search. A page outside the range returns HTTP 200 with an empty list. Searching returns React to page 1, with a 300ms debounce.

**Step 5: Add Driver**
- `POST /api/drivers` (name, licenseNo, licenseExpiry, phone, vehicleType, vehicleNo).
- Normalize: collapse extra spaces in the name to one; trim and uppercase license and vehicle no. For phone keep only digits and strip +88. So `01711-000001` and `+8801711000001` both become `01711000001`, and the duplicate check works.
- Phone format: starts with 01, second digit 3-9, 11 digits total. Check duplicate license and phone. If two managers save at once, the unique constraint stops it.
- A new driver is always Active. License expiry is optional. A past date is still allowed, because data for drivers with an expired license may exist; a future-only rule would block editing other fields of an expired driver.
- Messages: "Driver name is required.", "License number is required.", "Phone number is invalid.", "Vehicle type is required.", "Vehicle number is required.", "License number already exists.", "Phone number already exists."
- Success: "Driver saved." HTTP 201. Errors show inside the modal, and the modal doesn't close. It goes in the audit log.

**Step 6: Edit**
- Fill the modal with `GET /api/drivers/{id}`, save with PUT. All fields can change, including the license number (for typos), but the duplicate check excludes itself.
- Status is not changed through PUT, only through PATCH. Set updated_by, updated_at. If license expiry changes, Alerts must refresh.
- Success: "Driver updated." Missing id: 404 "Driver not found."

**Step 7: Deactivate / Activate**
- `PATCH /api/drivers/{id}/status` (ACTIVE / INACTIVE). Lock the driver row.
- Your decision: a driver on an ongoing trip can't be deactivated: "Cannot deactivate: driver has an ongoing trip." Match "ongoing" to the Trip statuses (everything except Delivered and Cancelled).
- Activate is always allowed. If already in that status, return HTTP 200.
- React confirms first ("Deactivate Jamal Hossain?"). Tooltip "Deactivate" / "Activate". Success: "Driver deactivated." / "Driver activated."
- Why the lock: between the check and the save another manager could assign the driver to a trip, leaving an inactive driver on an ongoing trip. Trip assignment also takes the lock and checks that the driver is active.

**Step 8: Options (choosing a driver for a trip)**
- `GET /api/drivers/options?includeInactive=false`: id, name, vehicle ("Truck DH-11-2201").
- A new trip allows only active drivers. The Transport Cost table still shows old trips of inactive drivers (no active filter), so history stays correct.

**Step 9: Connection with Alerts**
- License Expiry alerts consider only active drivers.
- When a driver is deactivated, activated, or their license expiry is edited, trigger an Alerts refresh (after commit); otherwise the update comes after 15 minutes.
- When a driver's name changes, the Alerts item_name updates at the next refresh.

**Step 10: No data and errors**
- With no drivers: HTTP 200, empty list, "No drivers found." Server error: "Unable to load drivers." Missing id: 404.

**Step 11: Security**
- All endpoints need the Manager role. Other roles get 403 and React shows "You are not allowed to view drivers." Check the role of `GET /options` against the SRS.
- Create, update and status change go in the audit log. Set created_by and updated_by.

**Step 12: Performance**
- Index: driver(active, id). License and phone already have unique indexes. LIKE search doesn't use an index, but there are few drivers so it's not a problem. Read-only transactions. No caching.

**Build order**
1. Driver entity and columns (license_expiry, active, audit columns), unique constraints.
2. `GET /drivers` (search, status, paging) and `GET /{id}`.
3. `POST` (with phone normalization).
4. `PUT`.
5. `PATCH /status` (ongoing trip check, lock).
6. `GET /options`, block inactive drivers in Trip.
7. Alerts refresh trigger, index, 403 handler, audit log.

**Remember**
- "Available" doesn't mean free: Available = active, Not Available = inactive. A driver busy on a trip still shows "Available". To show "On trip" you need a separate computed status from the trip table.
- Block inactive drivers in the backend too. If the trip-assignment API doesn't check active, an inactive driver can be assigned.
- License expiry field: the Alerts License Expiry card doesn't work without it. It wasn't in the screenshot, so it's an assumption.
- One bell: get the number from `GET /api/alerts/unread-count` in the layout.
- A new driver (id asc order) goes to the last page. If you want the newest first, use id desc, but that would reverse the screenshot's order.
- No delete: deactivate is the only route, since deleting would break the driver reference on Transport Cost trips.
- One vehicle per driver. For more than one vehicle you need a separate vehicle table.

---

# 11. Dashboard

**Page does:** header on top (calendar icon, bell badge, user name + role, logout), a bar chart "Ranking of products based on inventory value" (quantity input + Out of total), and 4 cards below (Total Products, Arrivals This Month, Stock Value, Total Due). The Dashboard stores no data of its own; it sums data from other features.

**What the screenshot tells us**
- Chart: "Enter quantity to display: 15", "Out of total: 20". 15 bars shown, largest to smallest (Rice Miniket Tk 4.8L, Rice Nazirshail Tk 3.4L ...).
- Cards: Total Products 128, Arrivals This Month 17, Stock Value Tk 24,86,500, Total Due Tk 1,32,000.
- Header: Rahim Uddin / Warehouse Manager, bell shows 5 (Alerts' unread count), logout icon, calendar icon (no label, so its purpose isn't clear from the screenshot).
- Dashboard is the first sidebar menu item. After login the Manager lands here (Login Step 8).

**Step 1: Where data comes from**
- No new tables. Everything is a database SUM, not loading rows into Java.
- Inventory value (per product) = SUM(stock_batch.remaining_quantity x stock_batch.unit_cost). It uses purchase cost, not selling price (like the Warehouses stock amount). So stock_batch needs a `unit_cost` column: copy the arrival's purchase_price when the arrival is confirmed.
- Total Products: count of active products (active = true).
- Arrivals This Month: CONFIRMED arrivals with confirmed_at in the current month (Asia/Dhaka). Pending/Rejected are not counted.
- Stock Value: SUM(remaining x unit_cost) across all stock_batch. It must equal the sum of all stock amount cards on the Warehouses page.
- Total Due: SUM(total_amount - paid_amount) of CONFIRMED sales. It must equal the sum of every buyer's Total Due on the Buyers page.
- Cancelled sales are not counted.

**Step 2: Endpoints**
- `GET /api/dashboard/summary` - the 4 cards.
- `GET /api/dashboard/inventory-ranking?limit=15` - bar chart.
- `GET /api/alerts/unread-count` - bell badge (this is an Alerts endpoint, not Dashboard).
- `GET /api/auth/me` - name and role for the header (from the Login user table). The login response's name/role can also be used, but state is lost on page refresh, so keep `/me`.
- `POST /api/auth/logout` - logout icon (Login Step 10).
- When the page opens, React calls summary, inventory-ranking and unread-count together. If the limit changes, only inventory-ranking is called again.

**Step 3: Cards (`GET /summary`)**
- Response: totalProducts, arrivalsThisMonth, stockValue, totalDue.
- Stock value and total due are BigDecimal. If there's no data, send 0, not null (coalesce).
- The month is determined using the server's Asia/Dhaka zone. If the server is in UTC, the month can be wrong at night on the 30th/1st.

**Step 4: Inventory ranking chart**
- Group by product (a product's stock across several batches or warehouses is summed). Batches with remaining 0 are not counted.
- Order: inventoryValue desc, then product name asc (so ties don't flip order). Rank 1 first.
- limit: default 15, min 1, max 50. If empty, 0 or negative: "Enter a valid quantity." (400). If the limit is greater than the total number of products, it's not an error; return what exists.
- Response: items (productId, name, quantityKg, inventoryValue), displayed (number of items), totalProducts ("Out of total").
- "Out of total" = the number of products whose stock value is > 0 (decision). So with 20 products the chart says 20, while the Total Products card says 128 (all active products, including those without stock).
- Products with stock value 0 don't appear in the chart.
- The backend sends exact numbers (480000.00). The frontend builds "Tk 4.8L", "Tk 38K" (amount/100000, en-IN). The Y-axis ticks (Tk 5L, 3.8L ...) are also computed by the frontend from the max.
- Send the product name as it is in the database (Rice (Miniket)); the frontend rotates the label.

**Step 5: Header (bell, user, logout)**
- Bell: one bell, so every page must get its number from `GET /api/alerts/unread-count`, not hardcoded. Poll every 60 seconds.
- User name and role: `GET /api/auth/me`. The role is "MANAGER" in the backend; the frontend builds the label "Warehouse Manager".
- Logout: `POST /api/auth/logout` clears the cookie; React goes to the login page.
- Calendar icon: no endpoint needed now. To add a period filter, add from/to params to summary and ranking (like Reports), but the screenshot has no label, so this is not assumed.

**Step 6: No data and errors**
- With no products or stock, HTTP 200: cards show 0, items empty, chart shows "No inventory data available." This is not an error.
- Server error: "Unable to load dashboard data." Cards and chart are separate calls, so if one fails the other still shows.
- If the token has expired: 401, and React shows "Session expired - please sign in again."

**Step 7: Security**
- All endpoints need the Manager role. Other roles get 403 and React shows "You are not allowed to view the dashboard." Financial numbers (stock value, due) are visible to the manager only.
- Dashboard is read-only, so no audit log is needed.

**Step 8: Performance**
- Read-only transactions. Indexes: stock_batch(product_id, remaining_quantity), arrival(confirmed_at, status), sale(status).
- Ranking query: GROUP BY product, ORDER BY SUM desc, LIMIT :limit (limit is a parameter, never string concatenation).
- No caching: after a sale, arrival or donation the Dashboard must show fresh data.

**What you need**
- No new tables. Dependencies: Spring Web, Spring Data JPA, Spring Security (already present).
- These must exist first: product, stock_batch (with unit_cost), arrival, sale, alert (for unread-count). So the Dashboard is built last.
- Module: `dashboard/internal/` with controller, service, repository (custom `@Query`), dto/responses (DashboardSummary, InventoryRankingResponse).

**Build order**
1. Check that product, stock_batch, arrival and sale are ready (Dashboard comes last).
2. `GET /summary`: 4 queries (total products, arrivals this month, stock value, total due).
3. `GET /inventory-ranking`: GROUP BY query, limit validation.
4. Connect `GET /auth/me` and Alerts' unread-count to the Dashboard header.
5. 403 handler, index, Postman tests (limit=0 gives 400, no token gives 401, driver token gives 403).

**Remember**
- Total Products (128) and Out of total (20) are different numbers. If you want them to be the same, use one label in the frontend.
- Stock Value and Total Due should match the Warehouses and Buyers page numbers. If they don't, you may have written a separate query; use one shared service method.
- Expired batches' stock is also counted in Stock Value right now (the stock still exists). To exclude it, add an `expiry_date >= today` filter (your decision).
- Transport cost is not included in Stock Value or the cards.
- No delete: the Dashboard has no create/edit/delete, only GET.
