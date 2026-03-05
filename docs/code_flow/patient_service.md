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

---
---

# Deep Dive: `@Version` & Optimistic Locking

## What Is It?

The `Patient` entity has a `@Version` field that enables **optimistic locking** — a concurrency control strategy that prevents two users from silently overwriting each other's changes.

```java
// Patient.java (entity)
@Version
private Long version;  // Starts at 0, auto-incremented by Hibernate on every UPDATE
```

This adds a `version` column to the `patients` table:

```sql
CREATE TABLE patients (
    id         BIGINT,
    first_name VARCHAR,
    last_name  VARCHAR,
    ...
    version    BIGINT DEFAULT 0   ← auto-managed by Hibernate
);
```

---

## How Hibernate Uses It Automatically

Every time Hibernate saves an update, it **automatically adds `version` to the WHERE clause**:

```sql
-- What you'd expect:
UPDATE patients SET first_name = 'John' WHERE id = 5;

-- What ACTUALLY runs with @Version:
UPDATE patients
  SET first_name = 'John', version = 4
  WHERE id = 5 AND version = 3;
--                ^^^^^^^^^^^^^^^
--                "only update if version hasn't changed"
```

- If the `WHERE` matches → update succeeds, version increments to `4`
- If someone else already changed it (`version` is now `4`, not `3`) → `WHERE` matches **zero rows** → Hibernate throws `OptimisticLockException`

**You never set the version manually.** Hibernate manages it entirely.

---

## How It's Used in `updatePatient()` — Manual Check

On top of Hibernate's automatic protection, the service adds a **manual check** for a friendlier error message:

```java
// PatientServiceImpl.updatePatient()
public PatientProjection updatePatient(Long id, PatientUpdateRequest request) {
    Patient patient = patientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

    // ★ MANUAL VERSION CHECK — before Hibernate even tries to save
    if (request.getVersion() != null && patient.getVersion() != null
            && !request.getVersion().equals(patient.getVersion())) {
        throw new ConflictException(
            "Patient record has been modified by another transaction. "
            + "Please refresh and try again."
        );
    }

    // ... merge changes and save
}
```

**Why both manual AND automatic?**

| Layer | Error Type | User Experience |
|-------|-----------|-----------------|
| Manual check (service code) | `ConflictException` → 409 | Clean JSON error: `"Please refresh and try again"` |
| Hibernate automatic (if manual missed) | `OptimisticLockException` → 500 | Generic server error — bad UX |

The manual check catches the problem early and returns a **user-friendly response**. The Hibernate check is the safety net.

---

## Real-World Scenario

```
Time    Receptionist A                     Receptionist B
────────────────────────────────────────────────────────────
 T1     Opens patient #5                   Opens patient #5
        Sees: { phone: "111", ver: 3 }     Sees: { phone: "111", ver: 3 }

 T2     Changes phone → "222"
        PUT /patients/5 { phone: "222", version: 3 }
        Backend: request.ver(3) == db.ver(3) ✅
        UPDATE ... SET phone='222', version=4 WHERE id=5 AND version=3
        ✅ Success! DB version is now 4

 T3                                        Changes email → "new@email.com"
                                           PUT /patients/5 { email: "new@email.com", version: 3 }
                                           Backend: request.ver(3) != db.ver(4) ❌
                                           → ConflictException:
                                             "Record modified. Please refresh."
```

**Without `@Version`:** Receptionist B's save would succeed and **silently overwrite** A's phone change back (the phone would disappear from the update).

**With `@Version`:** B is told to refresh, sees A's phone change, then adds the email — **both changes are preserved**.

---

## The Frontend's Role

For this to work, the frontend must:

1. **Receive** the `version` when loading patient data
2. **Send it back** in the update request

```json
// GET /api/patients/5 response:
{
  "id": 5,
  "firstName": "Rahul",
  "contact": "9876543210",
  "version": 3            ← frontend stores this
}

// PUT /api/patients/5 request:
{
  "contact": "1111111111",
  "version": 3            ← frontend sends it back
}
```

If the versions match → save proceeds. If not → 409 error → frontend shows "Record was modified, please refresh."

---

## Where `@Version` Is Used in HMS

| Entity | Purpose |
|--------|---------|
| `Patient` | Prevents concurrent patient profile edits |
| `Charge` | Prevents concurrent billing charge modifications |
