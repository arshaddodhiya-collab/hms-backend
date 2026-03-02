# User & Role Service — Code Flow Documentation

## Module Overview

| Aspect | Details |
|--------|---------|
| **Package** | `com.hms.HospitalManagementSystem.service.impl` |
| **Classes** | `UserServiceImpl`, `RoleServiceImpl` |
| **Purpose** | Staff user management and role-based access control (RBAC) |
| **Used By** | `UserController`, `RoleController`, `AuthServiceImpl` |
| **Depends On** | `UserRepository`, `RoleRepository`, `DepartmentRepository`, `PermissionRepository`, `PasswordEncoder` |

---

## UserService — Purpose & Responsibility

UserService manages **hospital staff accounts** (not patients). It handles:
- **User CRUD** — creating and updating staff users (doctors, nurses, lab techs, receptionists)
- **Role assignment** — linking users to one or more roles
- **Department assignment** — linking users to hospital departments
- **Role-based queries** — fetching all doctors, all nurses, etc.

### Service Flow Diagrams

#### Create User (`createUser`)
```
Admin POST /api/users
  { username, password?, fullName, active, departmentId, roles[] }
        │
        ▼
┌──────────────────────────────────┐
│ Check username unique?           │ ── Exists → ConflictException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Build User entity:               │
│  - Encode password               │
│    (default: "123" if not set)   │
│  - Link department (if provided) │
│  - Resolve & link roles          │
└─────────┬────────────────────────┘
          ▼
     Save → Return UserDto
```

#### Update User (`updateUser`)
```
Admin PUT /api/users/{id}
  { fullName, active, departmentId, roles[] }
        │
        ▼
┌──────────────────────────────────┐
│ Fetch user                       │ ── Not found → RuntimeException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Update fields:                   │
│  - fullName, active              │
│  - Department (set or clear)     │
│  - Roles (full replacement)      │
└─────────┬────────────────────────┘
          ▼
     Save → Return UserDto
```

#### Get All Users (`getAllUsers`)
```
GET /api/users
        │
        ▼
┌──────────────────────────────────┐
│ Fetch all users                  │
│ Filter OUT users with            │
│   role "PATIENT"                 │
│ (Staff-only endpoint)            │
└─────────┬────────────────────────┘
          ▼
     List<UserDto>
```

> **Important filter**: `getAllUsers()` excludes `PATIENT` role users. This is a staff management endpoint — patients are managed by `PatientService`.

#### Get Users by Role (`getUsersByRole`)
```
GET /api/users?role=DOCTOR
        │
        ▼
  findByRoles_Name(roleName)
  → List<UserDto>
```

> Used by the frontend for doctor dropdowns in appointment booking.

---

## RoleService — Purpose & Responsibility

RoleService manages the **RBAC (Role-Based Access Control)** system:
- **List roles** — get all roles with their permissions
- **List permissions** — get all available permissions
- **Update role permissions** — dynamically assign/revoke permissions per role

### Service Flow Diagrams

#### Update Role Permissions (`updateRolePermissions`)
```
Admin PUT /api/roles/{roleId}/permissions
  { permissionIds: [1, 3, 5, 7] }
        │
        ▼
┌──────────────────────────────────┐
│ Fetch role                       │ ── Not found → ResourceNotFoundException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Fetch all permissions by IDs     │
│ Full replacement:                │
│  role.permissions = new set      │
└─────────┬────────────────────────┘
          ▼
     Save → Return RoleDto
       (with updated permissions)
```

---

## RBAC Data Model

```
User ──many-to-many──► Role ──many-to-many──► Permission
  │                       │                       │
  │                    "DOCTOR"               "patient:read"
  └── "dr_smith"       "NURSE"               "appointment:write"
                       "ADMIN"               "lab:manage"
                       "RECEPTION"
                       "LAB_TECH"
```

### Permission Structure

| Field | Description | Example |
|-------|-------------|---------|
| `code` | Permission identifier | `patient:write` |
| `module` | Feature area | `PATIENT`, `APPOINTMENT`, `LAB` |

### How Permissions Flow to Frontend

```
Login → AuthService.buildAuthResponse()
  │
  ├── user.getRoles()
  │     └── role.getPermissions()
  │           └── permission.getCode()
  │
  └── Returns: { permissions: ["patient:read", "appointment:write", ...] }
```

> The frontend checks these permission codes to show/hide UI elements (via `HasPermissionDirective` in Angular).

---

## Where These Services Are Used

| Consumer | Operations |
|----------|-----------|
| `UserController` | Staff CRUD, role-based filtering |
| `RoleController` | Role listing, permission management |
| `AuthServiceImpl` | Role lookup during registration |
| `AdmissionServiceImpl` | Doctor lookup via `UserRepository` |
| `EncounterServiceImpl` | Doctor validation via `UserRepository` |
| `AppointmentServiceImpl` | Doctor lookup via `UserRepository` |
