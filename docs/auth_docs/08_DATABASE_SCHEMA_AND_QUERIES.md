# Database Schema & SQL Queries

This document outlines the database tables implemented for the Authentication module and provides SQL queries to verify the data.

## 1. Schema Overview

The following tables handle user identity, authorization, and session management.

### Tables

#### `users`
Stores user account information.
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique Identifier |
| `username` | VARCHAR(50) | Unique Login ID |
| `password` | VARCHAR(255) | BCrypt Encoded Password |
| `full_name` | VARCHAR(100) | User's Display Name |
| `department_id` | BIGINT | ID of the department (FK) |
| `active` | BIT(1) | Is account active? |
| `deleted` | BIT(1) | Soft Delete flag (0=Active, 1=Deleted) |
| `created_at` | DATETIME | Timestamp of creation |

#### `roles`
Defines functional roles (e.g., ADMIN, DOCTOR).
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique Identifier |
| `name` | VARCHAR(50) | Role Name (Unique) |
| `description` | VARCHAR(255) | Human-readable description |

#### `permissions`
Granular access rights (modules/actions).
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique Identifier |
| `code` | VARCHAR(50) | Permission Code (e.g., `MOD_PATIENTS`) |
| `module` | VARCHAR(50) | Grouping (e.g., `GENERAL`) |

#### `refresh_tokens`
Stores long-lived tokens for session renewal.
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Unique Identifier |
| `token` | VARCHAR(255) | UUID String (Unique) |
| `user_id` | BIGINT | Owner of the token (FK -> users.id) |
| `expiry_date` | DATETIME | Time when token becomes invalid |

#### Join Tables
- **`user_roles`**: Links `users` <-> `roles` (Many-to-Many).
- **`role_permissions`**: Links `roles` <-> `permissions` (Many-to-Many).

---

## 2. Useful SQL Queries

Use these queries to check the state of your database.

### A. View All Users with Roles
```sql
SELECT u.id, u.username, u.full_name, r.name as role 
FROM users u 
JOIN user_roles ur ON u.id = ur.user_id 
JOIN roles r ON ur.role_id = r.id;
```

### B. View Permissions for a Specific Role (e.g., RECEPTION)
```sql
SELECT r.name as role, p.code as permission, p.module
FROM roles r
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE r.name = 'RECEPTION';
```

### C. Check Active Refresh Tokens
```sql
SELECT rt.id, u.username, rt.token, rt.expiry_date 
FROM refresh_tokens rt 
JOIN users u ON rt.user_id = u.id;
```

### D. Check for Soft-Deleted Users
```sql
SELECT * FROM users WHERE deleted = 1;
```

### E. Verify "Me" Details (Simulate Payload)
```sql
SELECT u.username, r.name as role
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'testuser';
```
