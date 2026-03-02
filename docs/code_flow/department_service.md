# Department Service — Code Flow Documentation

## Module Overview

| Aspect | Details |
|--------|---------|
| **Package** | `com.hms.HospitalManagementSystem.service.impl` |
| **Class** | `DepartmentServiceImpl` |
| **Purpose** | Hospital department management — organizing staff into organizational units |
| **Used By** | `DepartmentController`, `AuthServiceImpl`, `UserServiceImpl` |
| **Depends On** | `DepartmentRepository`, `UserRepository` |

---

## Purpose & Responsibility

DepartmentService manages **hospital departments** such as Cardiology, Emergency, Radiology, etc. Each department:
- Has a **name** and **description**
- Can have a **Head of Department** (a User/doctor)
- Tracks associated **staff members**
- Has an **active/inactive** status

---

## Service Flow Diagrams

### 1. Create Department (`createDepartment`)

```
Admin POST /api/departments
  { name, description, headOfDepartmentId? }
        │
        ▼
┌──────────────────────────────────┐
│ Check name uniqueness            │ ── Exists → ConflictException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Build Department entity          │
│  - If HOD ID provided:           │
│    → Lookup user, set as HOD     │
└─────────┬────────────────────────┘
          ▼
     Save → Return DepartmentDto
```

### 2. Update Department (`updateDepartment`)

```
Admin PUT /api/departments/{id}
  { name, description, headOfDepartmentId?, active }
        │
        ▼
┌──────────────────────────────────┐
│ Fetch department                 │ ── Not found → RuntimeException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ If name changed:                 │
│  Check new name unique?          │ ── Exists → ConflictException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Update fields:                   │
│  - name, description, active     │
│  - Set or clear HOD              │
└─────────┬────────────────────────┘
          ▼
     Return DepartmentDto
```

### 3. Delete Department (`deleteDepartment`)

```
Admin DELETE /api/departments/{id}
        │
        ▼
┌──────────────────────────────────┐
│ Fetch department                 │ ── Not found → RuntimeException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Safety check:                    │
│  Has active staff members?       │ ── Yes → ConflictException
│  (dept.getUsers() not empty)     │    "Cannot delete with active staff"
└─────────┬────────────────────────┘
          ▼
     departmentRepository.delete()
```

> **Safety guard**: A department cannot be deleted if it still has staff assigned. Staff must be reassigned first.

---

## DepartmentDto Response Shape

| Field | Source |
|-------|--------|
| `id` | Department.id |
| `name` | Department.name |
| `description` | Department.description |
| `headOfDepartmentName` | HOD user's fullName (or null) |
| `headOfDepartmentId` | HOD user's ID (or null) |
| `staffCount` | Count of associated users |
| `active` | Department.active flag |

---

## Where This Service Is Used

| Consumer | Operations |
|----------|-----------|
| `DepartmentController` | Full CRUD for departments |
| `AuthServiceImpl.register()` | Department lookup by name during registration |
| `UserServiceImpl.createUser()` | Department lookup by ID when assigning users |
