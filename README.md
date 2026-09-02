# 🏨 Resource Booking System

[![CI - Build and Test](https://github.com/your-org/booking_system/actions/workflows/ci.yml/badge.svg)](https://github.com/your-org/booking_system/actions/workflows/ci.yml)

A production-ready **RESTful Resource Booking System** built with **Spring Boot 3**, **Java 17**, **Spring Security (JWT)**, and **MySQL**. Supports full CRUD for resources and reservations with role-based access control (ADMIN / USER), dynamic filtering, pagination, and comprehensive test coverage.

---

## 📋 Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Features](#features)
- [API Endpoints](#api-endpoints)
- [Security Design](#security-design)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Seed Accounts](#seed-accounts)
- [Running Tests](#running-tests)
- [Swagger UI](#swagger-ui)
- [Project Structure](#project-structure)

---

## 🛠 Tech Stack

| Layer            | Technology                                     |
|-----------------|------------------------------------------------|
| Framework        | Spring Boot 3.3.4                              |
| Language         | Java 17                                        |
| Security         | Spring Security 6 + JWT (JJWT 0.12.6)        |
| Password Hashing | BCryptPasswordEncoder                          |
| Persistence      | Spring Data JPA + Hibernate                   |
| Primary Database | MySQL 8.x                                      |
| Test Database    | H2 (in-memory, MySQL mode)                    |
| Validation       | Jakarta Bean Validation                        |
| Documentation    | SpringDoc OpenAPI 2.6.0 (Swagger UI)          |
| Build            | Maven 3.9+                                     |
| Coverage         | JaCoCo 0.8.12                                  |
| Boilerplate      | Lombok 1.18.38                                 |
| CI               | GitHub Actions                                 |

---

## 🏗 Architecture

Follows strict **Layered Architecture**:

```
┌──────────────────────────────────────┐
│           Controller Layer           │  REST endpoints, request/response mapping
├──────────────────────────────────────┤
│            Service Layer             │  Business logic, RBAC enforcement
├──────────────────────────────────────┤
│          Repository Layer            │  JPA repositories, custom queries
├──────────────────────────────────────┤
│             Entity Layer             │  JPA entities (User, Resource, Reservation)
├──────────────────────────────────────┤
│              DTO Layer               │  Request/response DTOs with validation
├──────────────────────────────────────┤
│            Security Layer            │  JWT filter, UserPrincipal, SecurityConfig
└──────────────────────────────────────┘
```

---

## ✨ Features

- ✅ **JWT Authentication** — Stateless Bearer token, 24h expiry
- ✅ **RBAC** — `ADMIN` and `USER` roles with strict enforcement
- ✅ **Resource CRUD** — Admin-only create/update/delete; all roles can read
- ✅ **Reservation CRUD** — User creates own, views own; Admin manages all
- ✅ **Identity from JWT** — User identity is always extracted from the token, never the request body
- ✅ **Reservation Status** — `PENDING` → `CONFIRMED` / `CANCELLED`
- ✅ **Overlap Detection** — Prevents double-booking of the same resource time slot
- ✅ **Price Calculation** — `BigDecimal` precision; computed from duration × pricePerHour
- ✅ **Dynamic Filtering** — Filter by `status`, `minPrice`, `maxPrice`
- ✅ **Pagination & Sorting** — `page`, `size`, `sortBy`, `sortDirection` on all list endpoints
- ✅ **Bean Validation** — `@Valid` on all request bodies with detailed error messages
- ✅ **Global Exception Handling** — `@ControllerAdvice` with structured `ErrorResponse`
- ✅ **Swagger / OpenAPI** — Full API documentation with Bearer auth support
- ✅ **Seeded Users** — Admin and User accounts created on startup
- ✅ **JaCoCo Coverage** — Coverage report generated on every build
- ✅ **GitHub Actions CI** — Automated build, test, and coverage on every push

---

## 📡 API Endpoints

### Authentication

| Method | Path             | Auth Required | Description                              |
|--------|-----------------|---------------|------------------------------------------|
| POST   | `/auth/login`   | None          | Authenticate and get JWT token          |
| POST   | `/auth/register`| None          | Register new USER account               |

### Resources

| Method | Path                 | Role          | Description                         |
|--------|---------------------|---------------|-------------------------------------|
| GET    | `/api/resources`    | USER, ADMIN   | List resources (paginated, filtered)|
| GET    | `/api/resources/{id}` | USER, ADMIN | Get resource by ID                  |
| POST   | `/api/resources`    | ADMIN only    | Create a new resource               |
| PUT    | `/api/resources/{id}` | ADMIN only  | Update an existing resource         |
| DELETE | `/api/resources/{id}` | ADMIN only  | Delete a resource                   |

**Resource filter params:** `type`, `availableOnly`, `page`, `size`, `sortBy`, `sortDirection`

### Reservations

| Method | Path                          | Role          | Description                                        |
|--------|------------------------------|---------------|----------------------------------------------------|
| GET    | `/api/reservations`          | USER, ADMIN   | List reservations (user sees own; admin sees all) |
| GET    | `/api/reservations/{id}`     | USER, ADMIN   | Get reservation by ID (ownership enforced)        |
| POST   | `/api/reservations`          | USER, ADMIN   | Create reservation (identity from JWT)            |
| PATCH  | `/api/reservations/{id}/status` | USER, ADMIN | Update status (USER can only CANCEL own)        |
| DELETE | `/api/reservations/{id}`     | USER, ADMIN   | Soft-cancel reservation                           |

**Reservation filter params:** `status`, `minPrice`, `maxPrice`, `page`, `size`, `sortBy`, `sortDirection`

---

## 🔐 Security Design

```
POST /auth/login ──► AuthService ──► JWT Token (sub=username, userId, email, role)
                                          │
All other requests ─── JwtAuthenticationFilter ──► extracts UserPrincipal from token
                                          │
                              Spring Security RBAC
                         ┌────────────────────────────┐
                         │  hasRole('ADMIN')           │  Resource CRUD write, all reservations
                         │  hasAnyRole('USER','ADMIN') │  Read resources, own reservations
                         └────────────────────────────┘
```

- **Stateless**: No session, no cookies — pure JWT
- **BCrypt**: Passwords hashed with strength 12
- **Identity enforcement**: `@AuthenticationPrincipal UserPrincipal` is used in every protected endpoint to get the current user from JWT — the request body is never trusted for identity

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- MySQL 8.x (for production)

### 1. Clone the repository

```bash
git clone https://github.com/your-org/booking_system.git
cd booking_system
```

### 2. Configure database (production)

Set environment variables or edit `src/main/resources/application.yml`:

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=resource_booking_db
export DB_USERNAME=root
export DB_PASSWORD=yourpassword
```

### 3. Run the application

```bash
mvn spring-boot:run
```

Or with environment variables inline:

```bash
DB_USERNAME=root DB_PASSWORD=secret mvn spring-boot:run
```

### 4. Run with H2 (no MySQL needed)

```bash
mvn spring-boot:run -Dspring.profiles.active=test
```

---

## 🔧 Environment Variables

| Variable             | Default               | Description                   |
|---------------------|-----------------------|-------------------------------|
| `DB_HOST`           | `localhost`           | MySQL host                    |
| `DB_PORT`           | `3306`                | MySQL port                    |
| `DB_NAME`           | `resource_booking_db` | Database name                 |
| `DB_USERNAME`       | `root`                | Database username             |
| `DB_PASSWORD`       | `root`                | Database password             |
| `SEED_ADMIN_PASSWORD` | `Admin@123`         | Admin seed account password   |
| `SEED_USER_PASSWORD`  | `User@123`          | User seed account password    |
| `CORS_ALLOWED_ORIGINS`| `http://localhost:3000,...` | Allowed CORS origins   |

---

## 👤 Seed Accounts

The following accounts are automatically created on application startup:

| Role  | Username | Email                | Password   |
|-------|----------|----------------------|------------|
| ADMIN | `admin`  | `admin@example.com`  | `Admin@123`|
| USER  | `user1`  | `user1@example.com`  | `User@123` |
| USER  | `user2`  | `user2@example.com`  | `User@123` |

**Login example:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail": "admin@example.com", "password": "Admin@123"}'
```

---

## 🧪 Running Tests

Tests use an **H2 in-memory database** (MySQL compatibility mode) — no MySQL installation required.

```bash
# Run all tests with coverage report
mvn clean test

# Full build + install + tests
mvn clean install

# Skip tests (not recommended)
mvn clean install -DskipTests
```

Coverage report is generated at: `target/site/jacoco/index.html`

### Test Suite

| Test Class                              | Type        | What it covers                                    |
|-----------------------------------------|-------------|---------------------------------------------------|
| `AuthControllerIntegrationTest`         | Integration | Login, register, JWT validation                  |
| `ResourceAndReservationIntegrationTest` | Integration | Full lifecycle, RBAC, overlap, filtering          |
| `AuthServiceTest`                       | Unit        | Login/register business logic                    |
| `ResourceServiceTest`                   | Unit        | Resource CRUD service layer                      |
| `ResourceServiceImplTest`               | Unit        | All filter/sort/delete paths                     |
| `ReservationServiceTest`                | Unit        | Reservation creation, overlap, access control    |
| `ReservationServiceImplExtendedTest`    | Unit        | Edge cases, ownership, status transitions        |
| `JwtServiceTest`                        | Unit        | Token generation, validation, claim extraction   |
| `CustomUserDetailsServiceTest`          | Unit        | UserDetails load by username/email/id            |
| `GlobalExceptionHandlerTest`            | Unit        | All exception handler paths                      |
| `SecurityHandlerTest`                   | Unit        | 401/403 JSON responses                           |
| `PaginationUtilsTest`                   | Unit        | Sort validation, page/size validation            |
| `ApiResponseAndExceptionTest`           | Unit        | DTO factory methods, exception messages          |

---

## 📖 Swagger UI

After starting the application, visit:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Use the **Authorize** button to enter your Bearer token:
```
Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/multigenesys/booking/
│   │   ├── ResourceBookingApplication.java
│   │   ├── config/
│   │   │   ├── DataInitializer.java       # Seeds admin/user accounts
│   │   │   ├── OpenApiConfig.java         # Swagger/OpenAPI configuration
│   │   │   └── SecurityConfig.java        # Spring Security + JWT setup
│   │   ├── controller/
│   │   │   ├── AuthController.java        # POST /auth/login, /auth/register
│   │   │   ├── ResourceController.java    # GET/POST/PUT/DELETE /api/resources
│   │   │   └── ReservationController.java # CRUD /api/reservations
│   │   ├── dto/
│   │   │   ├── request/                   # LoginRequest, ResourceRequest, ReservationRequest...
│   │   │   └── response/                  # AuthResponse, ResourceResponse, ReservationResponse...
│   │   ├── entity/
│   │   │   ├── User.java                  # @Entity, Role enum FK
│   │   │   ├── Resource.java              # Bookable item (room/vehicle/equipment)
│   │   │   ├── Reservation.java           # Booking with status + price
│   │   │   ├── Role.java                  # ROLE_ADMIN, ROLE_USER
│   │   │   ├── ReservationStatus.java     # PENDING, CONFIRMED, CANCELLED
│   │   │   └── ResourceType.java          # CONFERENCE_HALL, VEHICLE, EQUIPMENT, DESK, ROOM
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java  # @ControllerAdvice
│   │   │   ├── ResourceNotFoundException.java
│   │   │   ├── BadRequestException.java
│   │   │   └── ConflictException.java
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── ResourceRepository.java
│   │   │   ├── ReservationRepository.java   # Custom overlap-detection queries
│   │   │   └── ReservationSpecification.java # Dynamic filtering
│   │   ├── security/
│   │   │   ├── JwtService.java              # Token generation/validation
│   │   │   ├── JwtAuthenticationFilter.java # Extracts JWT from request
│   │   │   ├── UserPrincipal.java           # Spring UserDetails impl
│   │   │   ├── CustomUserDetailsService.java
│   │   │   ├── JwtAuthenticationEntryPoint.java
│   │   │   └── CustomAccessDeniedHandler.java
│   │   ├── service/
│   │   │   ├── AuthService.java / impl/AuthServiceImpl.java
│   │   │   ├── ResourceService.java / impl/ResourceServiceImpl.java
│   │   │   └── ReservationService.java / impl/ReservationServiceImpl.java
│   │   └── util/
│   │       └── PaginationUtils.java         # Sort field whitelist validation
│   └── resources/
│       └── application.yml                  # Main config (MySQL, JWT, CORS)
└── test/
    ├── java/...                             # 97 tests across 13 suites
    └── resources/
        └── application-test.yml             # H2 in-memory config for tests
```

---

## 📊 Reservation Status Transitions

```
[Created] ──► PENDING ──► CONFIRMED  (Admin only)
                  │
                  └──► CANCELLED  (User: own reservation; Admin: any)

[CANCELLED] ──► PENDING/CONFIRMED  (Admin only, if no overlap)
```

---

## ⚖️ RBAC Summary

| Action                          | USER | ADMIN |
|---------------------------------|------|-------|
| Login / Register                | ✅   | ✅    |
| View resources                  | ✅   | ✅    |
| Create / Update / Delete resource| ❌  | ✅    |
| Create reservation              | ✅   | ✅    |
| View **own** reservations       | ✅   | ✅    |
| View **all** reservations       | ❌   | ✅    |
| Cancel **own** reservation      | ✅   | ✅    |
| Confirm / manage **any** reservation | ❌ | ✅  |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit changes: `git commit -m 'Add my feature'`
4. Push: `git push origin feature/my-feature`
5. Open a Pull Request

---

## 📄 License

This project is developed as a backend developer assignment.
