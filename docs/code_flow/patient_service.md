# Patient Service — Code Flow Documentation

## Module Overview

| Aspect | Details |
|--------|---------|
| **Package** | `com.hms.HospitalManagementSystem.service.impl` |
| **Class** | `PatientServiceImpl` |
| **Purpose** | Patient registration, search, profile management, and soft-delete |
| **Used By** | `PatientController` |
| **Depends On** | `PatientRepository`, `PatientMapper`, `PatientSpecification` |

---

## Purpose & Responsibility

PatientService is the **patient master data management** module. It handles the full patient lifecycle:
- **Registration** — creating new patient records with duplicate detection
- **Search** — full-text search across patient fields with pagination (using `Slice`)
- **Details Retrieval** — fetching full patient profile via projections
- **Update** — editing patient details with optimistic locking
- **Soft Delete** — marking patients as inactive/deleted without physical removal

---

## Service Flow Diagrams

### 1. Register Patient (`registerPatient`)

```
Client POST /api/patients
        │
        ▼
┌──────────────────────────────────┐
│ Duplicate Detection (3 checks):  │
│  1. Contact number exists?       │ ── Yes → ConflictException
│  2. Email exists?                │ ── Yes → ConflictException
│  3. Name+DOB+Contact match?     │ ── Yes → ConflictException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Map DTO → Patient Entity         │
│   (via PatientMapper)            │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Save & Flush to DB               │
│   (Generates ID + MRN)           │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Re-fetch as PatientProjection    │
│   (returns clean DTO shape)      │
└─────────┬────────────────────────┘
          ▼
     PatientProjection
```

**Key Details:**
- Triple-layer duplicate detection ensures no accidental duplicates
- `saveAndFlush()` is used to immediately persist and generate the ID
- The return value is re-fetched as a **Spring Data Projection** (not the raw entity)

### 2. Search Patients (`searchPatients`)

```
Client GET /api/patients?query=john&page=0&size=10
        │
        ▼
┌──────────────────────────────────┐
│ Build JPA Specification          │
│   via PatientSpecification       │
│   (searches name, contact, MRN)  │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Execute query with Slice paging  │
│   findBy(spec, projection, page) │
└─────────┬────────────────────────┘
          ▼
     Slice<PatientProjection>
  (hasNext flag, no total count)
```

**Key Details:**
- Uses **JPA Specifications** for dynamic search criteria
- Returns a `Slice` (not `Page`) — avoids costly `COUNT(*)` query for better performance
- The `PatientSpecification.search(query)` handles multi-field matching

### 3. Get Patient Details (`getPatientDetails`)

```
Client GET /api/patients/{id}
        │
        ▼
┌──────────────────────────────────┐
│ findById(id, ProjectionClass)    │ ── Not found → ResourceNotFoundException
└─────────┬────────────────────────┘
          ▼
     PatientDetailsProjection
  (full details including related data)
```

**Key Details:**
- Uses `PatientDetailsProjection` — a richer projection than `PatientProjection`
- Projection-based queries only fetch needed columns

### 4. Update Patient (`updatePatient`)

```
Client PUT /api/patients/{id}
        │
        ▼
┌──────────────────────────────────┐
│ Fetch Patient entity             │ ── Not found → ResourceNotFoundException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Optimistic Locking Check         │
│  request.version != entity.ver?  │ ── Mismatch → ConflictException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Merge changes via mapper         │
│  PatientMapper.updateEntity()    │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Save & re-fetch as projection    │
└─────────┬────────────────────────┘
          ▼
     PatientProjection
```

**Key Details:**
- **Optimistic locking** prevents concurrent modification conflicts
- Uses `PatientMapper.updateEntityFromRequest()` to selectively merge non-null fields
- Returns a fresh projection after save

### 5. Delete Patient (`deletePatient`)

```
Client DELETE /api/patients/{id}
        │
        ▼
┌──────────────────────────────────┐
│ Fetch Patient entity             │ ── Not found → ResourceNotFoundException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Soft Delete:                     │
│   patient.deleted = true         │
│   patient.active = false         │
└─────────┬────────────────────────┘
          ▼
     Save (no physical deletion)
```

---

## Where This Service Is Used

| Consumer | Operations |
|----------|-----------|
| `PatientController` | All CRUD operations |
| `AdmissionServiceImpl` | Looks up patient during admission (via `PatientRepository` directly) |
| `BillingServiceImpl` | Validates patient existence for invoices |
| `LabServiceImpl` | Resolves patient for lab requests |
| `DashboardServiceImpl` | Counts total patients for stats |

---

## Key Design Patterns

| Pattern | Details |
|---------|---------|
| **Spring Data Projections** | `PatientProjection` and `PatientDetailsProjection` control response shape |
| **JPA Specification** | Dynamic search queries via `PatientSpecification` |
| **Slice Pagination** | Uses `Slice` instead of `Page` for performant pagination |
| **Soft Delete** | Logical deletion via `deleted` flag — data preserved for auditing |
| **Optimistic Locking** | `@Version` field prevents lost updates in concurrent scenarios |

---

## Error Handling

| Error | When |
|-------|------|
| `ConflictException` | Duplicate contact, email, or name+DOB+contact found |
| `ConflictException` | Version mismatch during update (optimistic locking) |
| `ResourceNotFoundException` | Patient not found by ID |
