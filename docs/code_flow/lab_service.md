# Lab Service — Code Flow Documentation

## Module Overview

| Aspect | Details |
|--------|---------|
| **Package** | `com.hms.HospitalManagementSystem.service.impl` |
| **Class** | `LabServiceImpl` |
| **Purpose** | Lab test catalog management, lab request ordering, result entry, and queue management |
| **Used By** | `LabController`, `ReportServiceImpl` |
| **Depends On** | `LabTestCatalogRepository`, `LabRequestRepository`, `LabResultRepository`, `EncounterRepository`, `PatientRepository` |

---

## Purpose & Responsibility

LabService manages the **complete laboratory workflow** in three areas:

1. **Catalog Management** — maintaining the master list of available lab tests
2. **Request Management** — doctors ordering tests and lab queue operations
3. **Result Management** — lab technicians entering test results

---

## Lab Request Status State Machine

```
  ORDERED ──────► IN_PROGRESS ──────► COMPLETED
                                         │
                                     (results added)
  
  Any ──────► CANCELLED
```

| Status | Who Sets | Description |
|--------|----------|-------------|
| `ORDERED` | Doctor (via create request) | Test has been ordered |
| `IN_PROGRESS` | Lab Tech (via updateStatus) | Sample collected, processing |
| `COMPLETED` | System (via addLabResults) | Auto-set when results are entered |
| `CANCELLED` | Lab Tech (via updateStatus) | Test cancelled |

---

## Service Flow Diagrams

### 1. Create Lab Test Catalog Entry (`createLabTest`)

```
Admin POST /api/lab/tests
  { name, code, price, referenceRange, active }
        │
        ▼
┌──────────────────────────────────┐
│ Check: Code already exists?      │ ── Yes → ConflictException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Build LabTestCatalog entity      │
│ Save to DB                       │
└─────────┬────────────────────────┘
          ▼
     Return LabTestCatalog
```

### 2. Create Lab Request (`createLabRequest`)

```
Doctor POST /api/lab/requests
  { encounterId, patientId, labTestId, notes }
        │
        ▼
┌──────────────────────────────────┐
│ Validate:                        │
│  - Encounter exists?             │ ── No → ResourceNotFoundException
│  - Patient exists?               │ ── No → RuntimeException
│  - Lab Test exists in catalog?   │ ── No → ResourceNotFoundException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Create LabRequest:               │
│  - Link: encounter, patient,     │
│    labTest                       │
│  - testName: snapshot from       │
│    catalog                       │
│  - status = ORDERED              │
│  - technicianNotes from request  │
└─────────┬────────────────────────┘
          ▼
     Save → Return LabRequest
```

**Key Details:**
- The `testName` is **snapshotted** from the catalog at order time — protects against catalog changes affecting historical records
- Lab requests are linked to encounters, establishing clinical context

### 3. Add Lab Results (`addLabResults`)

```
Lab Tech POST /api/lab/requests/{id}/results
  [ { parameterName, resultValue, unit, referenceRange, isAbnormal } ]
        │
        ▼
┌──────────────────────────────────┐
│ Validate:                        │
│  - Request exists?               │ ── No → ResourceNotFoundException
│  - Not COMPLETED/CANCELLED?      │ ── Yes → RuntimeException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ For each result entry:           │
│  Create LabResult entity         │
│  Link to LabRequest              │
│  Save individually               │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Auto-complete:                   │
│  request.status = COMPLETED      │
└─────────┬────────────────────────┘
          ▼
     Return updated LabRequest
```

**Key Details:**
- Results are **appended** (not replaced) — supports multiple parameter entries per test
- Adding results auto-marks the request as `COMPLETED`
- Each result entry can be flagged as `abnormal` for clinical attention

### 4. Lab Queue (`getLabQueue`)

```
Lab Tech GET /api/lab/queue?status=ORDERED,IN_PROGRESS
        │
        ▼
┌──────────────────────────────────┐
│ findByStatusIn(statuses)         │
│ Returns filtered lab requests    │
└─────────┬────────────────────────┘
          ▼
     List<LabRequest>
```

> The lab queue is filtered by status — typically showing `ORDERED` and `IN_PROGRESS` requests that need attention.

---

## Complete Lab Workflow

```
Doctor orders test ─────► LabRequest (ORDERED)
                                │
                                ▼
              Lab Tech picks from queue
              Updates status → IN_PROGRESS
                                │
                                ▼
              Lab Tech enters results
              addLabResults() → COMPLETED
                                │
                                ▼
              Doctor views results in encounter
              ReportService can generate PDF
```

---

## Where This Service Is Used

| Consumer | Operations |
|----------|-----------|
| `LabController` | CRUD for catalog, ordering requests, entering results, queue retrieval |
| `ReportServiceImpl` | `getLabRequestById()` to fetch data for PDF generation |
| `DashboardServiceImpl` | `labRequestRepository` for pending lab counts |
