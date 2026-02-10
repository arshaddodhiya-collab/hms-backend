# Department Service & Endpoints

This document details the **Department Service** logic and the exposed **API Endpoints** for department management.

## 1. DepartmentService (`com.hms.HospitalManagementSystem.service.DepartmentService`)

The `DepartmentService` handles all business logic related to department management.

### Key Features

- **Transactional**: All operations are wrapped in `@Transactional` to ensure data integrity.
- **CRUD Operations**: Create, Read, Update, Delete departments
- **Validation**:
    - Unique department name constraint
    - Cannot delete departments with active staff members
- **DTO Mapping**: Converts between `Department` entities and `DepartmentDto` objects

### Methods

#### `getAllDepartments()`
Returns a list of all departments with their details.
- **Returns**: `List<DepartmentDto>`
- **Mapping**: Includes head of department name, staff count, and active status

#### `getDepartmentById(Long id)`
Retrieves a single department by ID.
- **Parameters**: `id` - Department ID
- **Returns**: `DepartmentDto`
- **Throws**: `RuntimeException` if department not found

#### `createDepartment(DepartmentDto dto)`
Creates a new department.
- **Parameters**: `dto` - Department data
- **Validation**: 
    - Checks if department name already exists
    - Validates head of department user exists (if provided)
- **Returns**: `DepartmentDto` of created department
- **Throws**: `RuntimeException` if validation fails

#### `updateDepartment(Long id, DepartmentDto dto)`
Updates an existing department.
- **Parameters**: 
    - `id` - Department ID to update
    - `dto` - Updated department data
- **Validation**:
    - Checks if department exists
    - Validates name uniqueness if changed
    - Validates head of department user exists (if provided)
- **Returns**: `DepartmentDto` of updated department
- **Throws**: `RuntimeException` if validation fails

#### `deleteDepartment(Long id)`
Deletes a department.
- **Parameters**: `id` - Department ID to delete
- **Validation**: Cannot delete departments with active staff members
- **Throws**: `RuntimeException` if department has staff or not found

### DTO Mapping Logic

The `mapToDto()` method converts a `Department` entity to `DepartmentDto`:
```java
private DepartmentDto mapToDto(Department dept) {
    return DepartmentDto.builder()
        .id(dept.getId())
        .name(dept.getName())
        .description(dept.getDescription())
        .headOfDepartmentName(
            dept.getHeadOfDepartment() != null ? 
            dept.getHeadOfDepartment().getFullName() : null)
        .headOfDepartmentId(
            dept.getHeadOfDepartment() != null ? 
            dept.getHeadOfDepartment().getId() : null)
        .staffCount(dept.getUsers().size())
        .active(dept.isActive())
        .build();
}
```

## 2. API Endpoints (`DepartmentController`)

Base URL: `/api/v1/departments`

All endpoints require authentication via JWT token in the `Authorization: Bearer <token>` header.

### GET `/api/v1/departments`
Retrieves all departments.

**Security**: `@PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_READ')")`

**Response**:
```json
[
  {
    "id": 1,
    "name": "Cardiology",
    "description": "Heart and cardiovascular system",
    "headOfDepartmentName": "Dr. John Smith",
    "headOfDepartmentId": 5,
    "staffCount": 12,
    "active": true
  },
  {
    "id": 2,
    "name": "Emergency",
    "description": "Urgent critical care",
    "headOfDepartmentName": null,
    "headOfDepartmentId": null,
    "staffCount": 8,
    "active": true
  }
]
```

### GET `/api/v1/departments/{id}`
Retrieves a single department by ID.

**Security**: `@PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_READ')")`

**Path Parameters**: `id` - Department ID

**Response**: `DepartmentDto` (same structure as above)

**Error Responses**:
- `404 Not Found`: Department does not exist
- `403 Forbidden`: User lacks required permission

### POST `/api/v1/departments`
Creates a new department.

**Security**: `@PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_WRITE')")`

**Request Body**:
```json
{
  "name": "Neurology",
  "description": "Brain and nervous system",
  "headOfDepartmentId": 7,
  "active": true
}
```

**Response**: `DepartmentDto` of created department (201 Created)

**Error Responses**:
- `409 Conflict`: Department name already exists
- `400 Bad Request`: Invalid head of department ID
- `403 Forbidden`: User lacks required permission

### PUT `/api/v1/departments/{id}`
Updates an existing department.

**Security**: `@PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_WRITE')")`

**Path Parameters**: `id` - Department ID

**Request Body**:
```json
{
  "name": "Neurology",
  "description": "Brain and nervous system - Updated",
  "headOfDepartmentId": 8,
  "active": true
}
```

**Response**: `DepartmentDto` of updated department

**Error Responses**:
- `404 Not Found`: Department does not exist
- `409 Conflict`: New name conflicts with existing department
- `400 Bad Request`: Invalid head of department ID
- `403 Forbidden`: User lacks required permission

### DELETE `/api/v1/departments/{id}`
Deletes a department.

**Security**: `@PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_DELETE')")`

**Path Parameters**: `id` - Department ID

**Response**: `204 No Content`

**Error Responses**:
- `404 Not Found`: Department does not exist
- `409 Conflict`: Department has active staff members
- `403 Forbidden`: User lacks required permission

## 3. Standard Departments

The system seeds the following standard departments on initialization:

1. General
2. Cardiology
3. Neurology
4. Orthopedics
5. Pediatrics
6. Gynecology
7. Dermatology
8. Ophthalmology
9. Emergency
10. Radiology
11. Pathology
12. Pharmacy
13. Administration

These are created via the `V2__create_department_module.sql` migration script.

## 4. Business Rules

### Department Name
- Must be unique (case-sensitive)
- Cannot be null or empty
- Maximum length: 100 characters

### Head of Department
- Optional field
- Must reference a valid user ID if provided
- A user can be head of multiple departments (Many-to-One relationship)
- Setting to `null` removes the head of department

### Department Deletion
- Cannot delete a department that has active staff members
- Must first reassign or remove all users from the department
- Soft delete is available via the `deleted` flag (not currently exposed in API)

### Staff Count
- Automatically calculated from the number of users assigned to the department
- Read-only field (cannot be set directly)
- Updated automatically when users are assigned/unassigned
