# Database Schema & SQL Queries

This document outlines the database tables implemented for the Authentication module and provides SQL queries to verify the data.

## 1. Full Database Schema (MySQL DDL)

You can use the following SQL script to recreate the authentication tables.

```sql
-- 1. Users Table
-- Stores user accounts. 'department' is a free-text string.
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    department VARCHAR(100),
    active BIT(1) NOT NULL DEFAULT 1,
    deleted BIT(1) NOT NULL DEFAULT 0,
    created_at DATETIME
);

-- 2. Roles Table
-- Defines system roles (e.g., ADMIN, DOCTOR).
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- 3. Permissions Table
-- Defines granular access rights.
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    module VARCHAR(50) NOT NULL
);

-- 4. Refresh Tokens Table
-- Stores JWT refresh tokens. One-to-One with Users (Single Session).
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date DATETIME NOT NULL,
    user_id BIGINT UNIQUE,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 5. User Roles (Join Table)
-- Maps Users to Roles.
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 6. Role Permissions (Join Table)
-- Maps Roles to Permissions.
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);
```

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
