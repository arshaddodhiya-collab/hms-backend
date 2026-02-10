# Database Schema & Queries

This document details the database schema for the Admin module and common queries used for department and user management.

## 1. Database Tables

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
    FOREIGN KEY (head_of_department_id) REFERENCES users(id),
    INDEX IDX_DEPT_HOD (head_of_department_id)
);
```

**Fields**:
- `id`: Primary key, auto-increment
- `name`: Unique department name (e.g., "Cardiology")
- `description`: Optional text description
- `head_of_department_id`: Foreign key to users table (nullable)
- `created_at`: Timestamp of creation
- `updated_at`: Timestamp of last update
- `active`: Boolean flag for active/inactive status
- `deleted`: Boolean flag for soft delete

**Indexes**:
- Primary key on `id`
- Unique index on `name`
- Index on `head_of_department_id` for performance

### users Table (Department-related fields)

```sql
-- Existing users table with department additions
ALTER TABLE users ADD COLUMN department_id BIGINT;
ALTER TABLE users ADD CONSTRAINT fk_users_department 
    FOREIGN KEY (department_id) REFERENCES departments(id);
CREATE INDEX IDX_USER_DEPT ON users(department_id);
```

**Department-related Fields**:
- `department_id`: Foreign key to departments table (nullable)
- `department_legacy`: Old department field (VARCHAR, kept for migration)

**Complete users Table Structure**:
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    department_id BIGINT,
    active BOOLEAN DEFAULT TRUE,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id)
);
```

### user_roles Table (Many-to-Many)

```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);
```

## 2. Entity Relationships

```
Department (1) ----< (Many) User [staff members]
User (1) ----< (Many) Department [as head of department]
User (Many) >----< (Many) Role [via user_roles]
```

### Relationship Details

1. **Department → Users (One-to-Many)**:
   - One department can have many users (staff members)
   - Mapped via `users.department_id`
   - Lazy-loaded in JPA

2. **User → Department (Many-to-One as Head)**:
   - One user can be head of multiple departments
   - Mapped via `departments.head_of_department_id`
   - Lazy-loaded in JPA

3. **User → Roles (Many-to-Many)**:
   - Users can have multiple roles
   - Roles can be assigned to multiple users
   - Join table: `user_roles`

## 3. Common Queries

### Department Queries

#### Get All Departments with Staff Count
```sql
SELECT 
    d.id,
    d.name,
    d.description,
    d.active,
    u_hod.full_name AS head_of_department_name,
    d.head_of_department_id,
    COUNT(u_staff.id) AS staff_count
FROM departments d
LEFT JOIN users u_hod ON d.head_of_department_id = u_hod.id
LEFT JOIN users u_staff ON d.id = u_staff.department_id
WHERE d.deleted = FALSE
GROUP BY d.id, d.name, d.description, d.active, u_hod.full_name, d.head_of_department_id
ORDER BY d.name;
```

#### Get Department by ID with Details
```sql
SELECT 
    d.*,
    u.full_name AS head_name,
    COUNT(staff.id) AS staff_count
FROM departments d
LEFT JOIN users u ON d.head_of_department_id = u.id
LEFT JOIN users staff ON d.id = staff.department_id
WHERE d.id = ? AND d.deleted = FALSE
GROUP BY d.id;
```

#### Check Department Name Uniqueness
```sql
SELECT COUNT(*) 
FROM departments 
WHERE name = ? AND deleted = FALSE;
```

#### Get Departments by Active Status
```sql
SELECT * 
FROM departments 
WHERE active = ? AND deleted = FALSE
ORDER BY name;
```

### User Queries

#### Get All Users with Department and Roles
```sql
SELECT 
    u.id,
    u.username,
    u.full_name,
    u.active,
    d.name AS department_name,
    d.id AS department_id,
    GROUP_CONCAT(r.name) AS roles
FROM users u
LEFT JOIN departments d ON u.department_id = d.id
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.deleted = FALSE
GROUP BY u.id, u.username, u.full_name, u.active, d.name, d.id
ORDER BY u.username;
```

#### Get Users by Department
```sql
SELECT 
    u.id,
    u.username,
    u.full_name,
    u.active
FROM users u
WHERE u.department_id = ? AND u.deleted = FALSE
ORDER BY u.full_name;
```

#### Get User by ID with Full Details
```sql
SELECT 
    u.*,
    d.name AS department_name,
    GROUP_CONCAT(r.name) AS roles
FROM users u
LEFT JOIN departments d ON u.department_id = d.id
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.id = ? AND u.deleted = FALSE
GROUP BY u.id;
```

#### Check Username Uniqueness
```sql
SELECT COUNT(*) 
FROM users 
WHERE username = ? AND deleted = FALSE;
```

### Role Assignment Queries

#### Assign Role to User
```sql
INSERT INTO user_roles (user_id, role_id)
VALUES (?, ?);
```

#### Remove Role from User
```sql
DELETE FROM user_roles
WHERE user_id = ? AND role_id = ?;
```

#### Get User's Roles
```sql
SELECT r.* 
FROM roles r
JOIN user_roles ur ON r.id = ur.role_id
WHERE ur.user_id = ?;
```

## 4. Migration Scripts

### V2__create_department_module.sql

This migration creates the department infrastructure:

```sql
-- Create departments table
CREATE TABLE IF NOT EXISTS departments (
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

-- Add department_id to users table
ALTER TABLE users ADD COLUMN department_id BIGINT;
ALTER TABLE users ADD CONSTRAINT fk_users_department 
    FOREIGN KEY (department_id) REFERENCES departments(id);

-- Seed standard departments
INSERT INTO departments (name, description, active) VALUES 
('General', 'General Practice and Triage', true),
('Cardiology', 'Heart and cardiovascular system', true),
('Neurology', 'Brain and nervous system', true),
('Orthopedics', 'Bones and muscles', true),
('Pediatrics', 'Medical care for infants, children, and adolescents', true),
('Gynecology', 'Female reproductive health', true),
('Dermatology', 'Skin, hair, and nails', true),
('Ophthalmology', 'Eye care', true),
('Emergency', 'Urgent critical care', true),
('Radiology', 'Medical imaging', true),
('Pathology', 'Laboratory interaction', true),
('Pharmacy', 'Medicine distribution', true),
('Administration', 'Hospital administration and HR', true);

-- Migrate existing department data (if any)
UPDATE users u
JOIN departments d ON LOWER(u.department_legacy) = LOWER(d.name)
SET u.department_id = d.id
WHERE u.department_legacy IS NOT NULL;

-- Rename old department column
ALTER TABLE users CHANGE COLUMN department department_legacy VARCHAR(100);

-- Insert Department Permissions
INSERT INTO permissions (code, module) VALUES
('CMP_ADMIN_DEPT_READ', 'ADMIN'),
('CMP_ADMIN_DEPT_WRITE', 'ADMIN'),
('CMP_ADMIN_DEPT_DELETE', 'ADMIN');

-- Assign permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN' 
AND p.code IN ('CMP_ADMIN_DEPT_READ', 'CMP_ADMIN_DEPT_WRITE', 'CMP_ADMIN_DEPT_DELETE');
```

## 5. JPA/Hibernate Queries

### Department Repository

```java
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    // Find by name
    Optional<Department> findByName(String name);
    
    // Check if name exists
    boolean existsByName(String name);
    
    // Find active departments
    List<Department> findByActiveTrue();
    
    // Find by head of department
    List<Department> findByHeadOfDepartment(User user);
}
```

### User Repository

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Find by username
    Optional<User> findByUsername(String username);
    
    // Check if username exists
    boolean existsByUsername(String username);
    
    // Find by department
    List<User> findByDepartment(Department department);
    
    // Find active users
    List<User> findByActiveTrue();
    
    // Find by role
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);
}
```

## 6. Performance Considerations

### Indexes
- Primary keys are automatically indexed
- Foreign keys should have indexes for join performance
- Unique constraints create implicit indexes
- Consider composite indexes for common query patterns

### Query Optimization
- Use `LEFT JOIN` for optional relationships
- Avoid N+1 queries by using `JOIN FETCH` in JPA
- Use pagination for large result sets
- Consider caching for frequently accessed data

### Lazy vs Eager Loading
- **Lazy** (default): Load related entities only when accessed
- **Eager**: Load related entities immediately
- Current setup:
  - `Department.users`: Lazy
  - `Department.headOfDepartment`: Lazy
  - `User.department`: Eager (for security context)
  - `User.roles`: Eager (for security context)
