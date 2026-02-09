# Security Architecture Overview

This document outlines the security architecture of the Hospital Management System (HMS) backend, implemented using **Spring Security 6**.

## 1. Core Components

The security module conforms to the standard Spring Security architecture but is customized for a **Stateless JWT (JSON Web Token)** implementation.

### A. Entities (`com.hms.HospitalManagementSystem.entity`)
The security model is built upon three core database entities:

1.  **User (`users` table)**:
    *   Represents a system user (Admin, Doctor, Nurse, etc.).
    *   Contains `username`, `password` (BCrypt encoded), `active` status.
    *   Has a `Many-to-Many` relationship with **Roles**.

2.  **Role (`roles` table)**:
    *   Represents a functional role in the hospital (e.g., `ADMIN`, `DOCTOR`, `RECEPTION`).
    *   Has a `Many-to-Many` relationship with **Permissions**.

3.  **Permission (`permissions` table)**:
    *   Represents a granular access right (e.g., `MOD_PATIENTS`, `ACT_CREATE`).
    *   Used for fine-grained authorization (Future scope: `@PreAuthorize("hasAuthority('MOD_PATIENTS')")`).

### B. Security Configuration (`SecurityConfig.java`)
The `SecurityConfig` class is the central configuration point (`@EnableWebSecurity`).

*   **SecurityFilterChain**: Defines the HTTP security policy.
    *   **CSRF**: Disabled (`csrf.disable()`) because we use stateless JWTs.
    *   **Session**: Stateless (`SessionCreationPolicy.STATELESS`). The server does not store user sessions in memory.
    *   **CORS**: Configured to allow requests from the Angular Frontend (`http://localhost:4200`).
    *   **Exception Handling**: Custom handlers for 401 and 403 errors.

### C. Exception Handlers
We override default Spring Boot error pages to return clear JSON responses.

*   **`JwtAuthenticationEntryPoint`**: Handles `401 Unauthorized`. Triggered when an unauthenticated user tries to access a protected endpoint.
*   **`CustomAccessDeniedHandler`**: Handles `403 Forbidden`. Triggered when an authenticated user tries to access a resource they don't have permission for.

### D. User Details Service (`CustomUserDetailsService.java`)
*   Implements Spring Security's `UserDetailsService`.
*   Loads the `User` entity from the database using `UserRepository`.
*   Converts the `User` entity (and its Roles/Permissions) into a Spring Security `UserDetails` object.
*   **Authority Mapping**:
    *   Roles are mapped as Authorities (e.g., `ADMIN`).
    *   Permissions are also mapped as Authorities (e.g., `MOD_PATIENTS`).

### E. Filters
*   **`JwtAuthenticationFilter`**: (Implementation Pending)
    *   Intercepts every request.
    *   Extracts the JWT from the `Authorization: Bearer <token>` header.
    *   Validates the token.
    *   Sets the `Authentication` object in the `SecurityContext`.

## 2. Directory Structure

```
com.hms.HospitalManagementSystem
├── config
│   └── SecurityConfig.java          # Main Configuration
├── entity
│   ├── User.java                    # User Entity
│   ├── Role.java                    # Role Entity
│   └── Permission.java              # Permission Entity
├── repository
│   ├── UserRepository.java          # Data Access for Users
│   └── RoleRepository.java          # Data Access for Roles
└── security
    ├── CustomAccessDeniedHandler.java
    ├── CustomUserDetailsService.java
    ├── JwtAuthenticationEntryPoint.java
    └── JwtAuthenticationFilter.java
```
