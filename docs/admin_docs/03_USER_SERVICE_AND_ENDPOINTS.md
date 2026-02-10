# User Service & Endpoints

This document details the **User Service** logic and the exposed **API Endpoints** for user management within the Admin module.

## 1. UserService (`com.hms.HospitalManagementSystem.service.UserService`)

The `UserService` handles user creation and updates, complementing the authentication-related user operations.

### Key Features

- **Transactional**: All operations are wrapped in `@Transactional`
- **Password Security**: Uses BCrypt for password encoding
- **Department Assignment**: Links users to departments
- **Role Management**: Assigns roles to users

### Methods

#### `createUser(UserDto userDto)`
Creates a new user with encrypted password.

**Process**:
1. Validates username uniqueness
2. Encodes password using BCrypt
3. Assigns department (if provided)
4. Assigns roles (if provided)
5. Sets active status
6. Persists user to database

**Parameters**: `userDto` - User data including password
**Returns**: `UserDto` of created user (password excluded)
**Throws**: `RuntimeException` if username exists or department/role invalid

#### `updateUser(Long id, UserDto userDto)`
Updates an existing user.

**Process**:
1. Retrieves existing user
2. Updates username (if changed and unique)
3. Updates password (if provided - re-encoded)
4. Updates department assignment
5. Updates roles
6. Updates active status

**Parameters**: 
- `id` - User ID to update
- `userDto` - Updated user data

**Returns**: `UserDto` of updated user
**Throws**: `RuntimeException` if user not found or validation fails

### Password Handling

- **Creation**: Password is required and must be provided in plain text
- **Encoding**: Automatically encoded using `BCryptPasswordEncoder` with strength 10
- **Updates**: If password field is provided, it's re-encoded; if null/empty, existing password is retained
- **Response**: Password is never returned in API responses

## 2. API Endpoints (`UserController`)

Base URL: `/api/v1/users`

All endpoints require authentication via JWT token.

### GET `/api/v1/users`
Retrieves all users.

**Security**: `@PreAuthorize("hasAuthority('CMP_ADMIN_USER_READ')")`

**Response**:
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

### GET `/api/v1/users/{id}`
Retrieves a single user by ID.

**Security**: `@PreAuthorize("hasAuthority('CMP_ADMIN_USER_READ')")`

**Path Parameters**: `id` - User ID

**Response**: `UserDto` (same structure as above)

**Error Responses**:
- `404 Not Found`: User does not exist
- `403 Forbidden`: User lacks required permission

### POST `/api/v1/users`
Creates a new user.

**Security**: `@PreAuthorize("hasAuthority('CMP_ADMIN_USER_WRITE')")`

**Request Body**:
```json
{
  "username": "dr.jones",
  "password": "SecurePass123!",
  "fullName": "Dr. Sarah Jones",
  "departmentId": 3,
  "roles": ["DOCTOR"],
  "active": true
}
```

**Field Requirements**:
- `username`: Required, unique, 3-50 characters
- `password`: Required for creation, min 8 characters
- `fullName`: Optional, max 100 characters
- `departmentId`: Optional, must reference valid department
- `roles`: Optional, defaults to empty array
- `active`: Optional, defaults to true

**Response**: `UserDto` of created user (password excluded, 201 Created)

**Error Responses**:
- `409 Conflict`: Username already exists
- `400 Bad Request`: Invalid department ID or role
- `403 Forbidden`: User lacks required permission

### PUT `/api/v1/users/{id}`
Updates an existing user.

**Security**: `@PreAuthorize("hasAuthority('CMP_ADMIN_USER_WRITE')")`

**Path Parameters**: `id` - User ID

**Request Body**:
```json
{
  "username": "dr.jones",
  "password": "NewSecurePass456!",
  "fullName": "Dr. Sarah Jones-Williams",
  "departmentId": 4,
  "roles": ["DOCTOR", "ADMIN"],
  "active": true
}
```

**Notes**:
- Password is optional for updates (omit to keep existing password)
- Username can be changed if new username is unique
- All other fields follow same rules as creation

**Response**: `UserDto` of updated user

**Error Responses**:
- `404 Not Found`: User does not exist
- `409 Conflict`: New username conflicts with existing user
- `400 Bad Request`: Invalid department ID or role
- `403 Forbidden`: User lacks required permission

## 3. UserDto Structure

### Request DTO (for creation/updates)
```java
{
  "id": Long,              // Auto-generated, ignore for creation
  "username": String,      // Required, unique
  "password": String,      // Required for creation, optional for updates
  "fullName": String,      // Optional
  "departmentId": Long,    // Optional, FK to departments
  "roles": String[],       // Optional, array of role names
  "active": Boolean        // Optional, defaults to true
}
```

### Response DTO
```java
{
  "id": Long,
  "username": String,
  "fullName": String,
  "departmentName": String,    // Resolved from department relationship
  "departmentId": Long,
  "roles": String[],           // Array of role names
  "active": Boolean
  // Note: password is NEVER included in responses
}
```

## 4. Role Assignment

### Available Roles
- `ADMIN`: Full system access
- `DOCTOR`: Medical staff access
- `NURSE`: Nursing staff access
- `RECEPTION`: Front desk access
- `PHARMACIST`: Pharmacy access
- `LAB_TECH`: Laboratory access

### Role Validation
- Roles must exist in the `roles` table
- Role names are case-insensitive when assigning
- Multiple roles can be assigned to a single user
- Roles are stored in the `user_roles` join table

## 5. Department Assignment

### Assignment Rules
- Users can belong to zero or one department
- Department must exist before assignment
- Setting `departmentId` to `null` removes department assignment
- Department name is automatically resolved in responses

### Department Relationship
- Many-to-One: Multiple users can belong to one department
- Lazy-loaded on the entity level
- Eagerly fetched for DTO responses

## 6. Business Rules

### Username
- Must be unique across all users
- Case-sensitive
- 3-50 characters
- Cannot be changed to an existing username

### Password
- Minimum 8 characters (recommended)
- Stored as BCrypt hash (never plain text)
- Cannot be retrieved (one-way encryption)
- Must be provided for user creation
- Optional for updates (omit to keep existing)

### Active Status
- `true`: User can log in and access the system
- `false`: User is disabled (cannot authenticate)
- Does not delete user data
- Can be toggled for temporary access control

### User Deletion
- Soft delete via `deleted` flag (not currently exposed in API)
- Hard delete not recommended (maintains referential integrity)
- Consider setting `active = false` instead

## 7. Integration with Authentication

The User entity is shared between the Admin module and the Authentication module:

- **Admin Module**: Manages user creation, updates, department/role assignment
- **Auth Module**: Handles login, JWT generation, password verification
- **Shared Repository**: `UserRepository` is used by both modules
- **Password Encoding**: Both modules use the same `BCryptPasswordEncoder` bean

### Workflow Example
1. Admin creates user via `/api/v1/users` (Admin Module)
2. Password is encrypted and stored
3. User can now log in via `/api/v1/auth/login` (Auth Module)
4. JWT token is issued with user's roles and permissions
5. User accesses protected endpoints with JWT
