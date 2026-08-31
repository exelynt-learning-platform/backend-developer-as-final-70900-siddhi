# Resource Booking System - RESTful API

A secure, scalable RESTful backend service for managing bookable resources (e.g., conference rooms, equipment, vehicles) and user reservations. Built with **Spring Boot 3**, **Java 17**, **Spring Security 6 (Stateless JWT)**, **Spring Data JPA / Hibernate**, and **MySQL 8**.

---

## 🚀 Tech Stack & Dependencies

| Layer | Technology |
| :--- | :--- |
| **Language** | Java 17 (OpenJDK 17) |
| **Framework** | Spring Boot 3.3.4 (Spring Web, Spring Security 6, Spring Data JPA, Validation) |
| **Security & Auth** | Stateless JWT (JJWT `io.jsonwebtoken:0.12.6`), BCrypt password hashing, RBAC |
| **Database & ORM** | MySQL 8.x, Hibernate / Spring Data JPA (JPA Criteria API / Specifications) |
| **Test Database** | In-Memory H2 Database (MySQL compatibility mode for zero-config test execution) |
| **API Documentation** | SpringDoc OpenAPI 3 / Swagger UI 2.6.0 |
| **Testing** | JUnit 5, Mockito, Spring Security Test, MockMvc |

---

## 👥 Seed Test Accounts

The system automatically initializes default user accounts with pre-hashed BCrypt passwords upon application startup:

| Role | Username | Email | Password | Access Level |
| :--- | :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` | `admin@example.com` | `Admin@123` | Full CRUD on resources & all reservations |
| **USER** | `user1` | `user1@example.com` | `User@123` | Read resources, create & manage own reservations |
| **USER** | `user2` | `user2@example.com` | `User@123` | Multi-tenant isolation testing |

---

## ⚙️ Environment Variables & Configuration

The application uses standard Spring configuration properties with sensible defaults and support for environment variable overrides:

| Variable Name | Default Value | Description |
| :--- | :--- | :--- |
| `DB_HOST` | `localhost` | MySQL hostname / IP |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `resource_booking_db` | MySQL database name |
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | `root` | MySQL password |
| `SERVER_PORT` | `8080` | Application HTTP server port |
| `SEED_ADMIN_PASSWORD` | `Admin@123` | Default password for seeded `admin` account |
| `SEED_USER_PASSWORD` | `User@123` | Default password for seeded `user1`/`user2` accounts |

---

## 🛠️ Getting Started & Setup Instructions

### Prerequisites
- **JDK 17+** installed and configured in `PATH`
- **MySQL 8.0+** running locally on port `3306`
- **Maven 3.8+** (or use the included wrapper script)

### 1. Database Setup
Ensure MySQL is running, then create the database:
```sql
CREATE DATABASE IF NOT EXISTS resource_booking_db;
```

### 2. Configure Environment (Optional)
If your MySQL credentials differ from the defaults (`root` / `root`):

**PowerShell (Windows):**
```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="resource_booking_db"
$env:DB_USERNAME="your_username"
$env:DB_PASSWORD="your_password"
```

**Bash (Linux / macOS):**
```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=resource_booking_db
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
```

### 3. Build the Application
```bash
mvn clean package -DskipTests
```

### 4. Run the Application
```bash
mvn spring-boot:run
```
The application will start on `http://localhost:8080`.

---

## 📖 API Documentation (Swagger / OpenAPI)

Once the application is running, open your browser and navigate to:
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI 3 JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

> **Swagger Bearer Token:** To execute secured requests in Swagger UI, click the **Authorize** button at the top right and enter: `Bearer <your_jwt_token>`.

---

## 🧪 Running Automated Tests

Run the complete test suite (25+ tests covering authentication, RBAC boundaries, reservation ownership isolation, dynamic JPA filtering, and decimal pricing):

```bash
mvn test
```

---

## 📡 REST API Reference & Endpoints

### 1. Authentication Endpoints (`/auth`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/login` | Public | Authenticate credentials and receive JWT Bearer token |
| `POST` | `/auth/register` | Public | Register a new user account with `ROLE_USER` |

### 2. Resource Management Endpoints (`/api/resources`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/resources` | User / Admin | Paginated list of resources (optional `type`, `availableOnly`) |
| `GET` | `/api/resources/{id}` | User / Admin | Retrieve resource details by ID |
| `POST` | `/api/resources` | Admin Only | Create a new bookable resource |
| `PUT` | `/api/resources/{id}` | Admin Only | Update an existing resource |
| `DELETE` | `/api/resources/{id}` | Admin Only | Remove a resource |

### 3. Reservation Management Endpoints (`/api/reservations`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/reservations` | User / Admin | Book a resource (Identity resolved from JWT) |
| `GET` | `/api/reservations` | User / Admin | Filtered & paginated bookings (User: own, Admin: all) |
| `GET` | `/api/reservations/{id}` | User / Admin | View booking by ID (User: own, Admin: all) |
| `PATCH` | `/api/reservations/{id}/status` | User / Admin | Update status (User: CANCELLED, Admin: any) |
| `DELETE` | `/api/reservations/{id}` | User / Admin | Cancel or remove a reservation |

---

## 💻 Sample cURL Requests

### 1. Login (Admin)
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "admin@example.com",
    "password": "Admin@123"
  }'
```

### 2. Create Resource (Admin Only)
```bash
curl -X POST http://localhost:8080/api/resources \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Grand Conference Room A",
    "description": "20-person conference room with 4K projector and AV system",
    "type": "CONFERENCE_HALL",
    "pricePerHour": 150.00,
    "isAvailable": true
  }'
```

### 3. Create Reservation (User)
```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Authorization: Bearer <USER_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceId": 1,
    "startTime": "2026-09-10T10:00:00",
    "endTime": "2026-09-10T13:00:00"
  }'
```

### 4. Filter & Paginate Reservations
```bash
curl -X GET "http://localhost:8080/api/reservations?status=CONFIRMED&minPrice=100.00&maxPrice=500.00&page=0&size=10&sortBy=totalPrice&sortDirection=DESC" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

### 5. Cancel Reservation (User)
```bash
curl -X PATCH http://localhost:8080/api/reservations/1/status \
  -H "Authorization: Bearer <USER_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "CANCELLED"
  }'
```
