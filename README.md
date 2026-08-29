# Resource Booking System

A RESTful booking system built with Spring Boot, JWT, and MySQL.

## Tech Stack
- Java 17
- Spring Boot 4.1.1
- Spring Security + JWT
- MySQL
- JPA/Hibernate
- Lombok

## Setup Instructions

### 1. Database Setup
Create database in MySQL:
```sql
CREATE DATABASE booking_db;
```

### 2. Create application.properties
Create file at `src/main/resources/application.properties`:
```properties
spring.application.name=booking_system

spring.datasource.url=jdbc:mysql://localhost:3306/booking_db
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

jwt.secret=mySecretKey123456789012345678901234567890
jwt.expiration=86400000
```

### 3. Run the Application
```
./mvnw spring-boot:run
```

## Seed Users (auto-created on first run)
| Email | Password | Role |
|-------|----------|------|
| admin@booking.com | admin123 | ADMIN |
| user@booking.com | user123 | USER |

## API Endpoints

### Auth
- `POST /auth/login` — Login and get JWT token

### Resources
- `GET /resources` — View all (ADMIN + USER)
- `POST /resources` — Create (ADMIN only)
- `PUT /resources/{id}` — Update (ADMIN only)
- `DELETE /resources/{id}` — Delete (ADMIN only)

### Reservations
- `POST /reservations` — Create reservation (USER)
- `GET /reservations/my` — View my reservations (USER)
- `GET /reservations` — View all reservations (ADMIN)
- `GET /reservations/filter?status=PENDING&minPrice=100&maxPrice=500` — Filter
- `PATCH /reservations/{id}/status?status=CONFIRMED` — Update status (ADMIN)

## Swagger UI
```
http://localhost:8080/swagger-ui/index.html
```
