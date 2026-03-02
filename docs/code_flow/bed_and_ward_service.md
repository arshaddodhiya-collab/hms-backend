# Bed & Ward Service — Code Flow Documentation

## Module Overview

| Aspect | Details |
|--------|---------|
| **Package** | `com.hms.HospitalManagementSystem.service.impl` |
| **Classes** | `BedServiceImpl`, `WardServiceImpl` |
| **Purpose** | Hospital infrastructure — managing wards and beds for the IPD module |
| **Used By** | `BedController`, `WardController`, `AdmissionServiceImpl` |
| **Depends On** | `BedRepository`, `WardRepository`, `IpdMapper` |

---

## WardService — Purpose & Responsibility

WardService manages **hospital wards** — logical groupings of beds (e.g., General Ward, ICU, Pediatric Ward). Operations:
- **Create** ward with name, type, capacity, active status
- **List** all wards
- **Get** ward by ID
- **Update** ward properties
- **Delete** ward (with safety check for active staff)

### Ward CRUD Flows

#### Create Ward
```
POST /api/wards { name, type, capacity, active }
        │
        ▼
┌──────────────────────────────┐
│ Check: name already exists?  │ ── Yes → IllegalArgumentException
└─────────┬────────────────────┘
          ▼
  Map request → Ward entity
  Save → Return WardResponse
```

#### Delete Ward
```
DELETE /api/wards/{id}
        │
        ▼
┌──────────────────────────────┐
│ Ward exists?                 │ ── No → ResourceNotFoundException
└─────────┬────────────────────┘
          ▼
  wardRepository.deleteById()
```

> **Note:** Ward deletion does not currently check for active beds/admitted patients — this is a known TODO in the codebase.

---

## BedService — Purpose & Responsibility

BedService manages **individual beds** within wards. Each bed has:
- A ward assignment
- A bed number and type (e.g., GENERAL, ICU, SEMI_PRIVATE)
- Occupied/active flags

### Bed CRUD Flows

#### Create Bed
```
POST /api/beds { wardId, number, type, active }
        │
        ▼
┌──────────────────────────────┐
│ Ward exists?                 │ ── No → ResourceNotFoundException
└─────────┬────────────────────┘
          ▼
  Map request → Bed entity
  Link to ward
  Save → Return BedResponse
```

#### Get Available Beds
```
GET /api/beds/available?wardId={id}&type={type}
        │
        ▼
┌──────────────────────────────┐
│ If type provided:            │
│  → findAvailableBedsByWard   │
│    AndType(wardId, type)     │
│ Else:                        │
│  → findAvailableBedsByWard   │
│    (wardId)                  │
└─────────┬────────────────────┘
          ▼
  List<BedResponse>
  (unoccupied + active beds)
```

#### Update Bed Status
```
PATCH /api/beds/{id}/status { active: true/false }
        │
        ▼
┌──────────────────────────────┐
│ Bed exists?                  │ ── No → ResourceNotFoundException
└─────────┬────────────────────┘
          ▼
  bed.active = isActive
  Save → Return BedResponse
```

---

## How Beds Are Used by AdmissionService

```
AdmissionService.admitPatient()
  │
  ├── bedRepository.findByIdWithLock(bedId)  ← Pessimistic lock
  ├── Validate: not occupied, is active
  ├── bed.occupied = true
  └── Save
  
AdmissionService.dischargePatient()
  │
  ├── bed.occupied = false
  └── Save
  
AdmissionService.transferPatient()
  │
  ├── Old bed: occupied = false
  ├── New bed: findByIdWithLock(), validate, occupied = true
  └── Save both
```

> **Bed locking**: The `findByIdWithLock()` method uses `@Lock(PESSIMISTIC_WRITE)` to prevent race conditions when multiple admissions attempt to claim the same bed simultaneously.

---

## Ward → Bed Relationship

```
Ward (ICU, capacity: 10)
  ├── Bed ICU-101 (GENERAL, active, unoccupied)
  ├── Bed ICU-102 (GENERAL, active, occupied ← Patient #45)
  ├── Bed ICU-103 (ICU, active, unoccupied)
  └── Bed ICU-104 (GENERAL, inactive/maintenance)
```
