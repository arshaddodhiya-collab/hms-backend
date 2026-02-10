# Admin Module Overview

This document provides an overview of the **Admin Module** in the Hospital Management System (HMS) backend, which handles **Department Management** and **User Management**.

## 1. Core Components

The Admin module is built on Spring Boot with Spring Security for role-based access control (RBAC).

### A. Entities (`com.hms.HospitalManagementSystem.entity`)

The admin module manages two primary entities:

1.  **Department (`departments` table)**:
    *   Represents a hospital department (e.g., Cardiology, Emergency, Radiology).
    *   Contains `name` (unique), `description`, `headOfDepartment` (User reference), `active` status.
    *   Has a `One-to-Many` relationship with **Users** (staff members).
    *   Has a `Many-to-One` relationship with **User** (head of department).

2.  **User (`users` table)**:
    *   Represents a system user (Admin, Doctor, Nurse, etc.).
    *   Contains `username` (unique), `password` (BCrypt encoded), `fullName`, `active` status.
    *   Has a `One-to-One` relationship with **Department**.
    *   Has a `Many-to-Many` relationship with **Roles**.

### B. DTOs (`com.hms.HospitalManagementSystem.dto`)

Data Transfer Objects for API communication:

1.  **DepartmentDto**:
    *   Fields: `id`, `name`, `description`, `headOfDepartmentName`, `headOfDepartmentId`, `staffCount`, `active`
    *   Used for both request and response payloads
    *   `staffCount` is a derived field (calculated from users collection)

2.  **UserDto**:
    *   Fields: `id`, `username`, `password`, `fullName`, `departmentName`, `departmentId`, `roles`, `active`
    *   `password` field is only used for creation/updates (not returned in responses for security)
    *   `roles` is a collection of role names

### C. Services (`com.hms.HospitalManagementSystem.service`)

Business logic layer:

1.  **DepartmentService**:
    *   CRUD operations for departments
    *   Validation: unique name constraint, cannot delete departments with active staff
    *   Mapping between Entity and DTO

2.  **UserService**:
    *   User creation and updates
    *   Password encoding using BCrypt
    *   Department assignment
    *   Role management

### D. Controllers (`com.hms.HospitalManagementSystem.controller`)

REST API endpoints:

1.  **DepartmentController** (`/api/v1/departments`):
    *   Secured with `@PreAuthorize` annotations
    *   Required permissions: `CMP_ADMIN_DEPT_READ`, `CMP_ADMIN_DEPT_WRITE`, `CMP_ADMIN_DEPT_DELETE`

2.  **UserController** (`/api/v1/users`):
    *   Secured with `@PreAuthorize` annotations
    *   Required permissions: `CMP_ADMIN_USER_READ`, `CMP_ADMIN_USER_WRITE`

## 2. Directory Structure

```
com.hms.HospitalManagementSystem
├── controller
│   ├── DepartmentController.java    # Department REST API
│   └── UserController.java          # User REST API
├── dto
│   ├── DepartmentDto.java           # Department DTO
│   └── UserDto.java                 # User DTO
├── entity
│   ├── Department.java              # Department Entity
│   └── User.java                    # User Entity (also used in Auth)
├── repository
│   ├── DepartmentRepository.java    # Department Data Access
│   └── UserRepository.java          # User Data Access
└── service
    ├── DepartmentService.java       # Department Business Logic
    └── UserService.java             # User Business Logic
```

## 3. Security Model

### Permissions

The Admin module uses fine-grained permissions:

*   **Department Permissions**:
    *   `CMP_ADMIN_DEPT_READ`: View departments
    *   `CMP_ADMIN_DEPT_WRITE`: Create/Update departments
    *   `CMP_ADMIN_DEPT_DELETE`: Delete departments

*   **User Permissions**:
    *   `CMP_ADMIN_USER_READ`: View users
    *   `CMP_ADMIN_USER_WRITE`: Create/Update users

### Role Assignment

By default, these permissions are assigned to the `ADMIN` role during database initialization.

## 4. Key Features

### Department Management
*   Create, Read, Update, Delete (CRUD) operations
*   Assign Head of Department (any user can head multiple departments)
*   Track staff count per department
*   Prevent deletion of departments with active staff

### User Management
*   Create and update users
*   Assign users to departments
*   Manage user roles
*   Password encryption (BCrypt)
*   Active/Inactive status management

## 5. Database Schema

### departments Table
```sql
CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    head_of_department_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    deleted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (head_of_department_id) REFERENCES users(id)
);
```

### users Table (Department-related fields)
```sql
ALTER TABLE users ADD COLUMN department_id BIGINT;
ALTER TABLE users ADD CONSTRAINT fk_users_department 
    FOREIGN KEY (department_id) REFERENCES departments(id);
```

## 6. Related Documentation

*   [Department Service & Endpoints](./02_DEPARTMENT_SERVICE_AND_ENDPOINTS.md)
*   [User Service & Endpoints](./03_USER_SERVICE_AND_ENDPOINTS.md)
*   [Database Schema](./04_DATABASE_SCHEMA.md)
*   [API Testing Guide](./05_API_TESTING_GUIDE.md)
*   [Security & Permissions](./06_SECURITY_AND_PERMISSIONS.md)
