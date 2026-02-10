# API Testing Guide

This document provides a comprehensive guide for testing the Admin Module API endpoints using tools like Postman, cURL, or similar REST clients.

## 1. Prerequisites

### Authentication
All Admin module endpoints require a valid JWT access token. You must first authenticate to obtain a token.

**Login Request**:
```bash
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**Response**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "c6a71ce3-47d0-432d-9d47-ea8062e6781e",
  "username": "admin",
  "role": "ADMIN",
  "permissions": ["CMP_ADMIN_DEPT_READ", "CMP_ADMIN_DEPT_WRITE", ...]
}
```

### Required Permissions
- **Department Operations**: `CMP_ADMIN_DEPT_READ`, `CMP_ADMIN_DEPT_WRITE`, `CMP_ADMIN_DEPT_DELETE`
- **User Operations**: `CMP_ADMIN_USER_READ`, `CMP_ADMIN_USER_WRITE`

Default `admin` user has all required permissions.

## 2. Department API Tests

### 2.1 Get All Departments

**Request**:
```bash
GET http://localhost:8080/api/v1/departments
Authorization: Bearer {accessToken}
```

**Expected Response** (200 OK):
```json
[
  {
    "id": 1,
    "name": "General",
    "description": "General Practice and Triage",
    "headOfDepartmentName": null,
    "headOfDepartmentId": null,
    "staffCount": 0,
    "active": true
  },
  {
    "id": 2,
    "name": "Cardiology",
    "description": "Heart and cardiovascular system",
    "headOfDepartmentName": "Dr. John Smith",
    "headOfDepartmentId": 5,
    "staffCount": 3,
    "active": true
  }
]
```

**Test Cases**:
- ✅ Returns 200 with valid token
- ✅ Returns 401 without token
- ✅ Returns 403 with token lacking `CMP_ADMIN_DEPT_READ` permission
- ✅ Returns array of departments (may be empty)

### 2.2 Get Department by ID

**Request**:
```bash
GET http://localhost:8080/api/v1/departments/2
Authorization: Bearer {accessToken}
```

**Expected Response** (200 OK):
```json
{
  "id": 2,
  "name": "Cardiology",
  "description": "Heart and cardiovascular system",
  "headOfDepartmentName": "Dr. John Smith",
  "headOfDepartmentId": 5,
  "staffCount": 3,
  "active": true
}
```

**Test Cases**:
- ✅ Returns 200 with valid ID
- ✅ Returns 404 with non-existent ID
- ✅ Returns 401 without token
- ✅ Returns 403 without permission

### 2.3 Create Department

**Request**:
```bash
POST http://localhost:8080/api/v1/departments
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "Oncology",
  "description": "Cancer treatment and care",
  "headOfDepartmentId": 7,
  "active": true
}
```

**Expected Response** (200 OK):
```json
{
  "id": 14,
  "name": "Oncology",
  "description": "Cancer treatment and care",
  "headOfDepartmentName": "Dr. Emily Brown",
  "headOfDepartmentId": 7,
  "staffCount": 0,
  "active": true
}
```

**Test Cases**:
- ✅ Creates department with valid data
- ✅ Creates department without head (headOfDepartmentId = null)
- ✅ Returns 409 if department name already exists
- ✅ Returns 400 if headOfDepartmentId is invalid
- ✅ Returns 403 without `CMP_ADMIN_DEPT_WRITE` permission

**Edge Cases**:
```json
// Minimal valid request
{
  "name": "Psychiatry",
  "active": true
}

// With all optional fields null
{
  "name": "Dentistry",
  "description": null,
  "headOfDepartmentId": null,
  "active": true
}
```

### 2.4 Update Department

**Request**:
```bash
PUT http://localhost:8080/api/v1/departments/14
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "Oncology",
  "description": "Cancer treatment, research, and palliative care",
  "headOfDepartmentId": 8,
  "active": true
}
```

**Expected Response** (200 OK):
```json
{
  "id": 14,
  "name": "Oncology",
  "description": "Cancer treatment, research, and palliative care",
  "headOfDepartmentName": "Dr. Michael Chen",
  "headOfDepartmentId": 8,
  "staffCount": 0,
  "active": true
}
```

**Test Cases**:
- ✅ Updates department with valid data
- ✅ Can change head of department
- ✅ Can remove head of department (set to null)
- ✅ Can change active status
- ✅ Returns 404 if department doesn't exist
- ✅ Returns 409 if new name conflicts with existing department
- ✅ Returns 403 without permission

**Edge Cases**:
```json
// Remove head of department
{
  "name": "Oncology",
  "description": "Cancer treatment",
  "headOfDepartmentId": null,
  "active": true
}

// Deactivate department
{
  "name": "Oncology",
  "description": "Cancer treatment",
  "headOfDepartmentId": 8,
  "active": false
}
```

### 2.5 Delete Department

**Request**:
```bash
DELETE http://localhost:8080/api/v1/departments/14
Authorization: Bearer {accessToken}
```

**Expected Response** (204 No Content)

**Test Cases**:
- ✅ Deletes department with no staff
- ✅ Returns 409 if department has active staff members
- ✅ Returns 404 if department doesn't exist
- ✅ Returns 403 without `CMP_ADMIN_DEPT_DELETE` permission

**Validation**:
```bash
# Verify deletion
GET http://localhost:8080/api/v1/departments/14
# Should return 404
```

## 3. User API Tests

### 3.1 Get All Users

**Request**:
```bash
GET http://localhost:8080/api/v1/users
Authorization: Bearer {accessToken}
```

**Expected Response** (200 OK):
```json
[
  {
    "id": 1,
    "username": "admin",
    "fullName": "System Administrator",
    "departmentName": "Administration",
    "departmentId": 13,
    "roles": ["ADMIN"],
    "active": true
  },
  {
    "id": 2,
    "username": "dr.smith",
    "fullName": "Dr. John Smith",
    "departmentName": "Cardiology",
    "departmentId": 2,
    "roles": ["DOCTOR"],
    "active": true
  }
]
```

**Test Cases**:
- ✅ Returns 200 with valid token
- ✅ Returns 401 without token
- ✅ Returns 403 without `CMP_ADMIN_USER_READ` permission
- ✅ Password is never included in response

### 3.2 Get User by ID

**Request**:
```bash
GET http://localhost:8080/api/v1/users/2
Authorization: Bearer {accessToken}
```

**Expected Response** (200 OK):
```json
{
  "id": 2,
  "username": "dr.smith",
  "fullName": "Dr. John Smith",
  "departmentName": "Cardiology",
  "departmentId": 2,
  "roles": ["DOCTOR"],
  "active": true
}
```

**Test Cases**:
- ✅ Returns 200 with valid ID
- ✅ Returns 404 with non-existent ID
- ✅ Password is not included in response

### 3.3 Create User

**Request**:
```bash
POST http://localhost:8080/api/v1/users
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "username": "dr.jones",
  "password": "SecurePass123!",
  "fullName": "Dr. Sarah Jones",
  "departmentId": 3,
  "roles": ["DOCTOR"],
  "active": true
}
```

**Expected Response** (200 OK):
```json
{
  "id": 10,
  "username": "dr.jones",
  "fullName": "Dr. Sarah Jones",
  "departmentName": "Neurology",
  "departmentId": 3,
  "roles": ["DOCTOR"],
  "active": true
}
```

**Test Cases**:
- ✅ Creates user with valid data
- ✅ Password is encrypted (BCrypt)
- ✅ Password is not returned in response
- ✅ Returns 409 if username already exists
- ✅ Returns 400 if departmentId is invalid
- ✅ Returns 400 if role is invalid
- ✅ Returns 403 without `CMP_ADMIN_USER_WRITE` permission

**Edge Cases**:
```json
// Minimal valid request
{
  "username": "nurse.williams",
  "password": "Password123!",
  "active": true
}

// Multiple roles
{
  "username": "dr.admin",
  "password": "SecurePass456!",
  "fullName": "Dr. Admin User",
  "departmentId": 13,
  "roles": ["DOCTOR", "ADMIN"],
  "active": true
}

// No department
{
  "username": "receptionist",
  "password": "Reception123!",
  "fullName": "Front Desk",
  "departmentId": null,
  "roles": ["RECEPTION"],
  "active": true
}
```

### 3.4 Update User

**Request**:
```bash
PUT http://localhost:8080/api/v1/users/10
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "username": "dr.jones",
  "password": "NewPassword456!",
  "fullName": "Dr. Sarah Jones-Williams",
  "departmentId": 4,
  "roles": ["DOCTOR", "ADMIN"],
  "active": true
}
```

**Expected Response** (200 OK):
```json
{
  "id": 10,
  "username": "dr.jones",
  "fullName": "Dr. Sarah Jones-Williams",
  "departmentName": "Orthopedics",
  "departmentId": 4,
  "roles": ["DOCTOR", "ADMIN"],
  "active": true
}
```

**Test Cases**:
- ✅ Updates user with valid data
- ✅ Can change password (re-encrypted)
- ✅ Can update without password (keeps existing)
- ✅ Can change department
- ✅ Can add/remove roles
- ✅ Can change active status
- ✅ Returns 404 if user doesn't exist
- ✅ Returns 409 if new username conflicts
- ✅ Returns 403 without permission

**Edge Cases**:
```json
// Update without changing password
{
  "username": "dr.jones",
  "fullName": "Dr. Sarah Jones-Williams",
  "departmentId": 4,
  "roles": ["DOCTOR"],
  "active": true
}

// Deactivate user
{
  "username": "dr.jones",
  "fullName": "Dr. Sarah Jones-Williams",
  "departmentId": 4,
  "roles": ["DOCTOR"],
  "active": false
}
```

## 4. Integration Test Scenarios

### Scenario 1: Complete Department Setup
```bash
# 1. Create department
POST /api/v1/departments
{
  "name": "Psychiatry",
  "description": "Mental health services",
  "active": true
}

# 2. Create users for the department
POST /api/v1/users
{
  "username": "dr.psychiatrist",
  "password": "Psych123!",
  "fullName": "Dr. Mind Doctor",
  "departmentId": 15,
  "roles": ["DOCTOR"],
  "active": true
}

# 3. Assign head of department
PUT /api/v1/departments/15
{
  "name": "Psychiatry",
  "description": "Mental health services",
  "headOfDepartmentId": 11,
  "active": true
}

# 4. Verify setup
GET /api/v1/departments/15
# Should show staffCount: 1, headOfDepartmentName: "Dr. Mind Doctor"
```

### Scenario 2: User Transfer Between Departments
```bash
# 1. Get user's current department
GET /api/v1/users/5

# 2. Transfer to new department
PUT /api/v1/users/5
{
  "username": "dr.smith",
  "fullName": "Dr. John Smith",
  "departmentId": 8,  # New department
  "roles": ["DOCTOR"],
  "active": true
}

# 3. Verify old department staff count decreased
GET /api/v1/departments/2

# 4. Verify new department staff count increased
GET /api/v1/departments/8
```

### Scenario 3: Department Deactivation
```bash
# 1. Deactivate department
PUT /api/v1/departments/5
{
  "name": "Old Department",
  "description": "Being phased out",
  "active": false
}

# 2. Verify department is inactive
GET /api/v1/departments/5
# Should show active: false

# 3. Users remain assigned but department is inactive
GET /api/v1/users
# Users still show departmentName but department is inactive
```

## 5. Error Response Examples

### 401 Unauthorized
```json
{
  "timestamp": "2026-02-10T12:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required",
  "path": "/api/v1/departments"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2026-02-10T12:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/v1/departments"
}
```

### 404 Not Found
```json
{
  "timestamp": "2026-02-10T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Department not found with ID: 999",
  "path": "/api/v1/departments/999"
}
```

### 409 Conflict
```json
{
  "timestamp": "2026-02-10T12:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Department with name 'Cardiology' already exists.",
  "path": "/api/v1/departments"
}
```

## 6. Postman Collection

A sample Postman collection structure:

```
Admin Module Tests
├── Authentication
│   └── Login (POST /api/v1/auth/login)
├── Departments
│   ├── Get All Departments (GET /api/v1/departments)
│   ├── Get Department by ID (GET /api/v1/departments/:id)
│   ├── Create Department (POST /api/v1/departments)
│   ├── Update Department (PUT /api/v1/departments/:id)
│   └── Delete Department (DELETE /api/v1/departments/:id)
└── Users
    ├── Get All Users (GET /api/v1/users)
    ├── Get User by ID (GET /api/v1/users/:id)
    ├── Create User (POST /api/v1/users)
    └── Update User (PUT /api/v1/users/:id)
```

**Environment Variables**:
- `baseUrl`: `http://localhost:8080`
- `accessToken`: (set from login response)
- `userId`: (for parameterized tests)
- `departmentId`: (for parameterized tests)
