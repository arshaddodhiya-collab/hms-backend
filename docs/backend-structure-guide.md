# Backend Structure & Best Practices Guide

This document defines the **architecture, coding standards, and operational best practices** for the backend project.
It serves as a single source of truth to ensure **consistency, security, auditability, and scalability**, especially for a healthcare (HMIS) system.

---

## 1. Technology Stack & Key Components

### 1.1 Database & Persistence

**MySQL**

* **What**: Relational Database Management System.
* **Why**:

  * ACID compliance for medical and financial data integrity.
  * Widely adopted in healthcare and enterprise systems.
  * Strong transactional guarantees.
  * Cost-effective and well supported.
* **Where**:

  * Primary data store for patients, encounters, appointments, admissions, billing, and audit logs.
* **Connection**:

  * Configured via `spring.datasource.*` properties in `application-*.yml`.
* **Driver**:

  * MySQL Connector/J (defined in `pom.xml`).

---

### 1.2 Authentication (JWT)

**JSON Web Tokens (JWT)**

* **What**:

  * Stateless authentication mechanism using signed tokens.
* **Why**:

  * No server-side session storage.
  * Scales well with distributed systems.
  * Frontend-friendly (Angular).
  * Secure when combined with short expiry and proper signing.
* **Where**:

  * Used for all authenticated API requests.
* **Implementation Details**:

  * **Library**: `jjwt 0.11.5` (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
  * Tokens contain:

    * `userId`
    * roles
    * permissions
  * Signing key configured in `application-*.yml`
  * Token expiration enforced strictly.

> ⚠️ This project uses **JWT with Spring Security**, not a full OAuth2 Authorization Server.

---

### 1.3 Authorization (RBAC – Role Based Access Control)

**Role-Based Access Control**

* **What**:

  * Access control based on roles and permissions.
* **Why**:

  * Mandatory for healthcare systems.
  * Matches frontend permission configuration.
  * Enables fine-grained access (module + action).
* **How**:

  * A user can have multiple roles.
  * A role can have multiple permissions.
  * Permissions are evaluated at API level.
* **Implementation**:

  * Permissions loaded during login.
  * Embedded into JWT claims.
  * Enforced using method-level security:

    ```java
    @PreAuthorize("hasAuthority('PATIENT_READ')")
    ```
* **Database Alignment**:

  * `USER → ROLE → PERMISSION`

---

### 1.4 CORS Configuration

**Cross-Origin Resource Sharing**

* **What**:

  * Browser security mechanism.
* **Why**:

  * Allow controlled access from Angular frontend.
* **Where**:

  * Configured centrally using Spring Security.
* **Configuration**:

  * Allowed origins defined via `ALLOWED_ORIGINS` environment variable.
  * No wildcard origins in production.

---

## 2. Architecture Patterns & Design Principles

The backend follows a **layered architecture** with RESTful APIs.

---

### 2.1 REST API Design

* **Style**: RESTful resource-oriented APIs.
* **Versioning**:

  ```text
  /api/v1/...
  ```
* **Resource Examples**:

  ```text
  /api/v1/patients
  /api/v1/appointments
  /api/v1/encounters
  ```
* **Security**:

  * Authorization is enforced using Spring Security annotations.
  * URLs are **not** used to distinguish public vs secured endpoints.

---

### 2.2 DTO Pattern (Data Transfer Objects)

* **What**:

  * DTOs represent API contracts.
* **Why**:

  * Prevent entity exposure.
  * Enable API versioning.
  * Avoid lazy-loading issues.
* **Rules**:

  * Controllers **only** accept and return DTOs.
  * Entities never cross the API boundary.
* **Location**:

  * `dto` package.

---

### 2.3 Service Layer Pattern

* **What**:

  * Encapsulates business logic.
* **Why**:

  * Clear separation of concerns.
  * Reusable and testable logic.
* **Responsibilities**:

  * Business rules
  * Transaction management
  * Authorization checks (business-level)
* **Location**:

  * `service` package.

---

### 2.4 Repository Pattern

* **What**:

  * Data access abstraction using Spring Data JPA.
* **Why**:

  * Cleaner persistence logic.
  * Database independence.
* **Rules**:

  * Extend `JpaRepository`.
  * No business logic inside repositories.
* **Location**:

  * `repository` package.

---

## 3. Recommended Project Structure

```text
com.test.practice
├── controller     # REST Controllers
├── service        # Business Logic
├── repository     # Data Access (JPA)
├── entity         # JPA Entities
├── dto            # API Request / Response Models
├── mapper         # DTO ↔ Entity mapping
├── security       # JWT filters, auth providers
├── config         # CORS, Security, Swagger configs
├── exception      # Global exception handling
└── PracticeApplication.java
```

---

## 4. Coding Standards by Layer

### 4.1 Controller Layer

* **Responsibilities**:

  * Handle HTTP requests.
  * Validate input.
  * Delegate to services.
* **Rules**:

  * Use constructor injection.
  * Use `@Valid` for validation.
  * No business logic.
  * No `@Transactional`.

---

### 4.2 Service Layer

* **Responsibilities**:

  * Business logic.
  * Orchestrating multiple repositories.
  * Transaction boundaries.
* **Rules**:

  * Annotate transactional methods with `@Transactional`.
  * Defensive coding (`Objects.requireNonNull`).
  * Never return entities to controllers.

---

### 4.3 Repository Layer

* **Responsibilities**:

  * Database operations only.
* **Rules**:

  * No transactions.
  * No business logic.

---

### 4.4 Entity Layer

* **Responsibilities**:

  * Map database tables.
* **Rules**:

  * Use JPA annotations.
  * Include audit fields where applicable.
  * Prefer `LAZY` fetching by default.

---

## 5. Database Management & Migrations

### 5.1 Migration Strategy

The project follows a **manual SQL migration strategy** to maintain full control and auditability.

---

### 5.2 `/db` Directory Structure

```text
/db
├── manual_migration.sql
└── schema_versions/
```

---

### 5.3 Migration Rules (IMPORTANT)

* Migration scripts are **append-only**.
* Never modify previously applied SQL.
* Each schema change must be additive or versioned.
* Production databases must never rely on Hibernate auto-DDL.

---

### 5.4 Migration Workflow

1. Add new DDL to `manual_migration.sql`
2. Apply manually:

   ```bash
   mysql -u user -p db_name < manual_migration.sql
   ```
3. Update corresponding JPA entities.
4. Restart application.
5. Hibernate validation ensures schema correctness:

   ```yaml
   spring.jpa.hibernate.ddl-auto=validate
   ```

---

## 6. Cross-Cutting Concerns

### 6.1 Logging

* Use SLF4J + Logback.
* Never use `System.out.println`.
* Do not log sensitive data (passwords, tokens).

---

### 6.2 Exception Handling

* Centralized handling using `@ControllerAdvice`.
* Consistent error response format.
* No stack traces exposed to clients.

---

<!-- ### 6.3 Auditing

* Critical write operations must generate audit records.
* Stored in `AUDIT_LOG` table.
* Includes:

  * user
  * action
  * entity
  * timestamp
  * IP address -->


