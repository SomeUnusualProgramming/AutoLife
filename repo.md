# AutoLife Repository Documentation

## Project Overview

**AutoLife** is a Spring Boot-based backend application designed as an API-first solution. The current implementation focuses on user management with a foundation for extensibility.

**Project Name:** LifeAI Backend  
**Version:** 1.0.0-SNAPSHOT  
**Language:** Java 21  
**Framework:** Spring Boot 3.2.1

---

## Tech Stack

### Core Framework
- **Spring Boot 3.2.1** - Application framework
- **Spring Data JPA** - Data access layer with ORM
- **Spring Web** - REST API support
- **Spring Validation** - Bean validation

### Database
- **PostgreSQL 42.7.1** - Relational database
- **Flyway 9.22.3** - Database migration management

### Development Tools
- **Lombok** - Boilerplate reduction (getters, setters, constructors)
- **Maven** - Build and dependency management

### Testing
- **Spring Boot Test** - Integration testing
- **TestContainers** - PostgreSQL containers for testing
- **JUnit** - Unit testing framework

---

## Project Structure

```
lifeai-backend/
├── src/
│   ├── main/
│   │   ├── java/com/lifeai/
│   │   │   ├── config/
│   │   │   │   └── JpaAuditingConfig.java          # JPA auditing configuration
│   │   │   ├── controller/
│   │   │   │   ├── UserController.java             # User REST endpoints
│   │   │   │   ├── HealthCheckController.java      # Health check endpoint
│   │   │   │   └── GlobalExceptionHandler.java     # Centralized exception handling
│   │   │   ├── service/
│   │   │   │   ├── UserService.java                # User business logic
│   │   │   │   └── HealthCheckService.java         # Health check service
│   │   │   ├── repository/
│   │   │   │   └── UserRepository.java             # User data access
│   │   │   ├── entity/
│   │   │   │   ├── User.java                       # User entity
│   │   │   │   └── BaseEntity.java                 # Base entity with audit fields
│   │   │   ├── dto/
│   │   │   │   ├── CreateUserRequest.java          # User creation request DTO
│   │   │   │   ├── UserResponse.java               # User response DTO
│   │   │   │   ├── ErrorResponse.java              # Error response DTO
│   │   │   │   └── HealthCheckResponse.java        # Health check response DTO
│   │   │   └── LifeAiBackendApplication.java       # Spring Boot entry point
│   │   └── resources/
│   │       ├── application.yml                     # Main configuration
│   │       ├── application-dev.yml                 # Development profile
│   │       └── db/migration/
│   │           └── V1__Initial_schema.sql          # Database schema migration
│   └── test/
│       ├── java/                                   # Unit and integration tests
│       └── resources/                              # Test configuration
└── pom.xml                                         # Maven project configuration
```

---

## Key Components

### Entities

#### **BaseEntity** (`com.lifeai.entity.BaseEntity`)
Abstract base class providing common audit fields for all entities:
- `id` - Auto-generated primary key (Long, SERIAL)
- `createdAt` - Timestamp of entity creation (auto-managed)
- `updatedAt` - Timestamp of last update (auto-managed)

#### **User** (`com.lifeai.entity.User`)
Represents a system user with the following attributes:
- `email` - Unique email address (required)
- `username` - Unique username (required, max 100 characters)
- `passwordHash` - Encrypted password (required)
- `firstName` - First name (optional, max 100 characters)
- `lastName` - Last name (optional, max 100 characters)
- `isActive` - Active status (defaults to true)

**Indexes:**
- `idx_email` - On email column for quick lookups
- `idx_username` - On username column for quick lookups

### Services

#### **UserService** (`com.lifeai.service.UserService`)
Business logic for user management:
- `createUser()` - Create new user with duplicate email/username validation
- `getUserById()` - Retrieve user by ID
- `getAllUsers()` - Retrieve all users
- `updateUser()` - Update user information
- `deleteUser()` - Delete user by ID

All methods are transactional, with read-only optimization for query operations.

#### **HealthCheckService** (`com.lifeai.service.HealthCheckService`)
Provides application health status information.

### Controllers

#### **UserController** (`com.lifeai.controller.UserController`)
REST API endpoints for user management (base path: `/api/users`):
- `POST /api/users` - Create user (returns 201 Created)
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users` - Get all users
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user (returns 204 No Content)

#### **HealthCheckController** (`com.lifeai.controller.HealthCheckController`)
Provides application health check endpoint.

#### **GlobalExceptionHandler** (`com.lifeai.controller.GlobalExceptionHandler`)
Centralized exception handling for REST endpoints, returning structured error responses.

### Data Transfer Objects (DTOs)

- **CreateUserRequest** - Input DTO for user creation/update
- **UserResponse** - Output DTO for user information
- **ErrorResponse** - Standardized error response format
- **HealthCheckResponse** - Health status response

---

## Database Schema

### `users` Table

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Auto-incrementing identifier |
| email | VARCHAR(255) | NOT NULL, UNIQUE | User's email address |
| username | VARCHAR(100) | NOT NULL, UNIQUE | User's login username |
| password_hash | VARCHAR(255) | NOT NULL | Encrypted password |
| first_name | VARCHAR(100) | NULL | User's first name |
| last_name | VARCHAR(100) | NULL | User's last name |
| is_active | BOOLEAN | DEFAULT true | User account status |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Last update timestamp |

**Indexes:**
- `idx_users_email` - For email lookups
- `idx_users_username` - For username lookups

---

## Configuration

### Main Configuration (`application.yml`)

**Server:**
- Port: 8080
- Context Path: `/api`

**Database:**
- Driver: PostgreSQL
- URL: `jdbc:postgresql://localhost:5432/lifeai`
- Credentials: postgres/postgres (default)

**JPA/Hibernate:**
- DDL Auto: `validate` (requires existing schema)
- Show SQL: false
- Format SQL: true

**Flyway:**
- Enabled: true
- Baseline on Migrate: true
- Migration Location: `classpath:db/migration`

**Logging:**
- Default Level: INFO
- `com.lifeai`: DEBUG
- Spring Web: DEBUG
- Hibernate SQL: DEBUG
- Hibernate Type Binding: TRACE

**Management Endpoints:**
- Health, Info, Metrics endpoints exposed

### Development Profile (`application-dev.yml`)

Overrides for development environment:
- Database: `lifeai_dev`
- Show SQL: true
- Same logging configuration as main

**To run with dev profile:**
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

---

## Getting Started

### Prerequisites
- Java 21
- Maven 3.6+
- PostgreSQL 12+

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd AutoLife
   ```

2. **Create PostgreSQL database**
   ```sql
   CREATE DATABASE lifeai;
   -- For development
   CREATE DATABASE lifeai_dev;
   ```

3. **Configure database connection**
   Edit `application.yml` or `application-dev.yml` with your PostgreSQL credentials if different from defaults (postgres/postgres).

4. **Build the project**
   ```bash
   mvn clean install
   ```

5. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   Or with development profile:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
   ```

6. **Verify application**
   - API is available at `http://localhost:8080/api`
   - Health check: `http://localhost:8080/api/actuator/health`

---

## API Examples

### Create User
```bash
POST /api/users
Content-Type: application/json

{
  "email": "john@example.com",
  "username": "john_doe",
  "password": "securepassword",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "email": "john@example.com",
  "username": "john_doe",
  "firstName": "John",
  "lastName": "Doe",
  "isActive": true,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### Get User
```bash
GET /api/users/1
```

### Get All Users
```bash
GET /api/users
```

### Update User
```bash
PUT /api/users/1
Content-Type: application/json

{
  "email": "john.updated@example.com",
  "username": "john_doe_updated",
  "password": "newpassword",
  "firstName": "Jonathan",
  "lastName": "Doe"
}
```

### Delete User
```bash
DELETE /api/users/1
```

**Response (204 No Content)**

---

## Testing

Run all tests:
```bash
mvn test
```

The test suite uses TestContainers to spin up a PostgreSQL instance for integration testing, ensuring database operations are properly tested without relying on an external database.

---

## Build and Deployment

### Build JAR
```bash
mvn clean package
```

Generated JAR: `target/lifeai-backend-1.0.0-SNAPSHOT.jar`

### Run JAR
```bash
java -jar target/lifeai-backend-1.0.0-SNAPSHOT.jar
```

With profiles:
```bash
java -jar target/lifeai-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

---

## Database Migrations

Database schema changes are managed through Flyway. Migration scripts are located in `src/main/resources/db/migration/` with versioning:
- `V1__Initial_schema.sql` - Initial database schema setup

New migrations should follow the naming convention: `V{number}__Description.sql`

---

## Future Extensibility

The project structure is designed for easy expansion:
- **New Entities**: Add to `entity/` package, extending `BaseEntity`
- **New Services**: Add to `service/` package with `@Service` annotation
- **New Controllers**: Add to `controller/` package with `@RestController` annotation
- **New DTOs**: Add to `dto/` package for request/response objects
- **Database Changes**: Create Flyway migration scripts in `db/migration/`

---

## Notes

- **Password Storage**: Currently stores passwords as plain text in `passwordHash`. Should implement proper password hashing (bcrypt/argon2) before production.
- **Exception Handling**: Implemented via `GlobalExceptionHandler` for consistent error responses.
- **Validation**: Uses Jakarta Bean Validation annotations on entities and DTOs.
- **Audit Timestamps**: Automatically managed by Hibernate annotations (`@CreationTimestamp`, `@UpdateTimestamp`).
