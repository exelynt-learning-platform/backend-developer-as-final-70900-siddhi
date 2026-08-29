# Resource Booking System

A RESTful API built with **Spring Boot 4**, **Spring Security**, **JWT**, and **MySQL** that allows users to book resources (rooms, vehicles, equipment) with full role-based access control.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.1.1 |
| Language | Java 17 |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| Database | MySQL 8.x |
| ORM | Spring Data JPA / Hibernate 7 |
| Docs | Springdoc OpenAPI 3.1.0 (Swagger UI) |
| Build | Maven |

---

## Prerequisites

- Java 17 (required — JDK 24 has a known `management.dll` issue on Windows)
- MySQL 8.x running locally
- Maven (or use `mvnw`)

---

## Database Setup

```sql
CREATE DATABASE booking_db;
```

Tables are auto-created by Hibernate (`ddl-auto=update`).

---

## Environment Configuration

Edit [`src/main/resources/application.properties`](src/main/resources/application.properties):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/booking_db
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD

jwt.secret=mySecretKey123456789012345678901234567890
jwt.expiration=86400000   # 24 hours in ms
```

---

## Running the Application

```powershell
# Set Java 17 (Windows)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Start (kills port 8080 automatically)
.\start.ps1
```

Or manually:
```powershell
.\mvnw.cmd spring-boot:run
```

**Swagger UI:** http://localhost:8080/swagger-ui/index.html

---

## Seed Users (Auto-created on first run)

| Role | Email | Password |
|---|---|---|
| ADMIN | `admin@booking.com` | `admin123` |
| USER | `user@booking.com` | `user123` |

---

## API Reference

### Authentication

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/auth/register` | Public | Register new user |
| POST | `/auth/login` | Public | Login → returns JWT token |

**Login response:**
```json
{ "token": "eyJhbGci...", "role": "ADMIN" }
```

---

### Resources

| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/resources` | USER, ADMIN | List all resources |
| GET | `/resources/{id}` | USER, ADMIN | Get resource by ID |
| POST | `/resources` | ADMIN | Create resource → `201 Created` |
| PUT | `/resources/{id}` | ADMIN | Update resource |
| DELETE | `/resources/{id}` | ADMIN | Delete resource → `204 No Content` |

**Resource body:**
```json
{ "name": "Conference Room A", "type": "room", "description": "Seats 10" }
```

---

### Reservations

| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/reservations` | USER, ADMIN | Create reservation → `201 Created` |
| GET | `/reservations` | ADMIN | All reservations (optional filters) |
| GET | `/reservations/my` | USER, ADMIN | Own reservations (optional filters) |
| PATCH | `/reservations/{id}/status` | ADMIN | Update status |
| DELETE | `/reservations/{id}` | ADMIN | Delete reservation → `204 No Content` |

**Reservation body:**
```json
{
  "resourceId": 1,
  "startTime": "2026-09-10T10:00:00",
  "endTime": "2026-09-10T12:00:00",
  "price": 500.00
}
```

**Filtering & Pagination:**
```
GET /reservations?status=PENDING&minPrice=100&maxPrice=1000&page=0&size=10&sort=price,asc
GET /reservations/my?status=CONFIRMED&page=0&size=5
```

All filter params are optional. Sorting supports any field (e.g., `price`, `startTime`, `status`).

**Reservation statuses:** `PENDING` | `CONFIRMED` | `CANCELLED`

---

## Authentication Flow

1. Call `POST /auth/login` with email + password
2. Copy the `token` from the response
3. Send `Authorization: Bearer <token>` header with all protected requests

In Swagger UI: click **🔒 Authorize** → paste token → click **Authorize**

---

## Error Responses

All errors return structured JSON:
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Resource not found with id: 99",
  "timestamp": "2026-08-29T15:30:00"
}
```

| HTTP Code | Meaning |
|---|---|
| 400 | Validation error / bad request |
| 401 | Missing or invalid JWT token |
| 403 | Insufficient role permissions |
| 404 | Resource / Reservation not found |

---

## Running Tests

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd test
```

Test coverage:
- `AuthServiceTest` — login, register, duplicate email
- `ResourceServiceTest` — getAll, getById (not found), create
- `ReservationServiceTest` — create, time validation, filter, updateStatus, delete
- `SecurityIntegrationTest` — 401 without auth, 403 USER on ADMIN endpoints, 200/204 ADMIN access

---

## Project Structure

```
src/main/java/com/example/booking/
├── config/          # SecurityConfig, OpenApiConfig
├── controller/      # AuthController, ResourceController, ReservationController
├── dto/             # LoginRequest/Response, RegisterRequest, ResourceRequest
│                      ReservationRequest, ReservationResponse
├── entity/          # User, Resource, Reservation
├── enums/           # Role (ADMIN, USER), ReservationStatus (PENDING, CONFIRMED, CANCELLED)
├── exception/       # ResourceNotFoundException, ErrorResponse, GlobalExceptionHandler
├── repository/      # UserRepository, ResourceRepository, ReservationRepository
├── security/        # JwtUtil, JwtFilter, UserDetailsServiceImpl
└── service/         # AuthService, ResourceService, ReservationService
```
