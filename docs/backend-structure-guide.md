# Backend Structure & Best Practices Guide

This document outlines the architectural patterns, coding standards, and best practices for the backend project. It serves as a comprehensive guide for developers to ensure consistency, security, and scalability.

## 1. Technology Stack & Key Components

### 1.1. Database & Persistence
**MySQL**
- **What**: Relational database management system.
- **Why**:
    - ACID compliance for data integrity.
    - Widely used in healthcare systems.
    - Excellent performance for structured data.
    - Cost-effective for enterprise deployments.
- **Where**: Primary data storage for patient, appointment, billing, and medical records data.
- **Connection**: Configured via `spring.datasource.*` properties in `application-*.yml` (or `.properties`).
- **Driver**: MySQL Connector/J (specified in `pom.xml`).

### 1.2. Authentication (JWT)
**JSON Web Tokens**
- **What**: Compact, URL-safe means of representing claims to be transferred between two parties.
- **Why**:
    - Stateless authentication (no session storage required).
    - Self-contained token with encoded user information.
    - Suitable for microservices and distributed systems.
    - Easy to verify and tamper-proof with cryptographic signature.
- **Where**: Token-based authentication system for API requests.
- **Implementation Details**:
    - **Library**: `JJWT 0.11.5` (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`).
    - Signing key configured in `application-*.yml`.
    - Token issuer URI configured for verification.

### 1.3. CORS Configuration
**Cross-Origin Resource Sharing**
- **What**: Cross-Origin Resource Sharing policy.
- **Why**: Enable secure communication with frontend applications.
- **Where**: Configured via `ALLOWED_ORIGINS` environment variable.
- **Configuration**: `cors.allowed-origins` in application files.

---

## 2. Architecture Patterns & Approaches

The application follows a standard layered architecture using **REST** principles.

### 2.1. REST API Architecture
- **Pattern**: RESTful web services.
- **Endpoints**:
    - `/api/v1/[resource]/public` (unauthenticated)
    - `/api/v1/[resource]/secured` (authenticated)
- **Implementation**: Spring Web with Spring Security OAuth2.

### 2.2. DTO Pattern (Data Transfer Objects)
- **What**: Segregation of domain models from API contracts.
- **Why**:
    - Version API independently.
    - Prevent information leakage (e.g., hiding password hashes).
    - Cleaner API design.
- **Where**: All request/response objects in REST endpoints in the `dto` package.

### 2.3. Service Layer Pattern
- **What**: Business logic encapsulation.
- **Why**:
    - Separation of concerns.
    - Reusable business logic.
    - Testability.
- **Where**: Service classes (`[Resource]Service`) handling business operations in the `service` package.

### 2.4. Repository Pattern
- **What**: Abstraction for data persistence.
- **Why**:
    - Database-agnostic data access.
    - Easier testing.
    - Decoupled business logic.
- **Where**: Spring Data JPA repositories in the `repository` package.

---

## 3. Directory Structure

```text
com.test.practice
├── controller       # REST Controllers (API Layer)
├── service          # Business Logic Layer
├── repository       # Data Access Layer (Interfaces)
├── entity           # Database Entities (JPA)
├── dto              # Data Transfer Objects (Request/Response)
├── exception        # Global Exception Handling
└── PracticeApplication.java # Entry Point
```

---

## 4. Coding Standards by Layer

### 4.1. Controller Layer
- **Responsibility**: Handle HTTP requests, validate input, and call services.
- **Best Practices**:
    - Use **Constructor Injection**.
    - Accept and return **DTOs**, never Entities.
    - Use `@Valid` for input validation.

### 4.2. Service Layer
- **Responsibility**: Business logic, transaction management (`@Transactional`).
- **Best Practices**:
    - defensive coding with `Objects.requireNonNull`.
    - Never return Entities to the controller; map to DTOs first.

### 4.3. Repository Layer
- **Responsibility**: Interface with the database extending `JpaRepository`.

### 4.4. Entity Layer
- **Responsibility**: Represents database tables using JPA annotations.

---

## 5. Database Management & Migrations

The project uses a **manual migration strategy** to ensure full control over the database schema.

### 5.1. The `/db` Directory
Located at the project root:
```text
/db
├── manual_migration.sql   # The master script containing all DDL statements
└── schema_versions/       # (Optional) Archived scripts for version history
```

### 5.2. Migration Workflow
1. **Draft**: Add `CREATE`/`ALTER` statements to `db/manual_migration.sql`.
2. **Apply**: Run `mysql -u [user] -p [db_name] < db/manual_migration.sql`.
3. **Update Entity**: specific Java Entity classes to match.
4. **Validate**: Restart the app. `spring.jpa.hibernate.ddl-auto=validate` will ensure synchronization.
