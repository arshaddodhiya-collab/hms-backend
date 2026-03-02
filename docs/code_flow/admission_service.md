# Admission Service (IPD) — Code Flow Documentation

## Module Overview

| Aspect | Details |
|--------|---------|
| **Package** | `com.hms.HospitalManagementSystem.service.impl` |
| **Class** | `AdmissionServiceImpl` |
| **Purpose** | Inpatient (IPD) admission lifecycle — admit, discharge, transfer patients |
| **Used By** | `AdmissionController` |
| **Depends On** | `AdmissionRepository`, `BedRepository`, `PatientRepository`, `UserRepository`, `BillingService`, `EncounterRepository`, `AppointmentService`, `IpdMapper` |

---

## Purpose & Responsibility

AdmissionService manages the **inpatient department (IPD)** workflow. When a patient needs to stay in the hospital, this service handles:
- **Admission** — assigning a patient to a bed with a doctor
- **Discharge** — releasing the patient, generating bills
- **Transfer** — moving a patient between beds
- **Querying** — listing active admissions

---

## Admission Status Lifecycle

```
  ADMITTED ──────────────────► DISCHARGED
      │
      │ (transfer = same status,
      │  different bed)
      ▼
  ADMITTED (new bed)
```

---

## Service Flow Diagrams

### 1. Admit Patient (`admitPatient`) — **Most Complex Operation**

```
Client POST /api/admissions
  { patientId, doctorId, bedId, diagnosis }
        │
        ▼
┌──────────────────────────────────┐
│ 1. Validate Patient              │ ── Not found → ResourceNotFoundException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ 2. Close active OPD appointments │
│  → appointmentService            │
│    .completeActiveAppointments   │
│    ForPatient()                  │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ 3. Check duplicate admission     │
│  Patient already admitted?       │ ── Yes → BusinessRuleViolationException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ 4. Validate Doctor               │ ── Not found → ResourceNotFoundException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ 5. Lock & Validate Bed           │
│  - findByIdWithLock() (pessim.)  │ ── Not found → ResourceNotFoundException
│  - Is occupied?                  │ ── Yes → BusinessRuleViolationException
│  - Is active?                    │ ── No → BusinessRuleViolationException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ 6. Update bed → occupied = true  │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ 7. Create Admission entity       │
│  status = ADMITTED               │
│  admissionDate = now             │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ 8. Create IPD Encounter          │
│  status = IN_PROGRESS            │
│  links: admission, patient,      │
│    doctor                        │
└─────────┬────────────────────────┘
          ▼
     AdmissionResponse
```

**Key Details:**
- **Pessimistic locking** on bed via `findByIdWithLock()` prevents race conditions where two admissions could claim the same bed
- **OPD → IPD transition**: All active outpatient appointments are auto-completed
- **Encounter creation**: An IPD encounter is created directly (not via EncounterService), with status `IN_PROGRESS` and linked to the admission

### 2. Discharge Patient (`dischargePatient`)

```
Client POST /api/admissions/{id}/discharge
  { dischargeDate, dischargeSummary, diagnosis, advice }
        │
        ▼
┌──────────────────────────────────┐
│ Validate:                        │
│  - Admission exists?             │ ── No → ResourceNotFoundException
│  - Not already discharged?       │ ── Yes → BusinessRuleViolationException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ 1. Update Admission:             │
│  - dischargeDate (from req or    │
│    now)                          │
│  - status = DISCHARGED           │
│  - dischargeSummary, diagnosis,  │
│    advice                        │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ 2. Release Bed:                  │
│  bed.occupied = false            │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ 3. Complete IPD Encounter:       │
│  status = COMPLETED              │
│  completedAt = now               │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ 4. Generate Bill:                │
│  → billingService.generateBill() │
│  (creates invoice from unbilled  │
│   charges)                       │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ 5. Clean up stray appointments:  │
│  → completeActiveAppointments    │
└─────────┬────────────────────────┘
          ▼
     AdmissionResponse
```

> **Discharge is a cascade operation**: it releases the bed, completes the encounter, triggers billing, and cleans up any remaining active appointments.

### 3. Transfer Patient (`transferPatient`)

```
Client POST /api/admissions/{id}/transfer
  { newBedId }
        │
        ▼
┌──────────────────────────────────┐
│ Validate:                        │
│  - Admission exists + ADMITTED?  │ ── No → BusinessRuleViolationException
│  - New bed exists (with lock)?   │ ── No → ResourceNotFoundException
│  - Not the same bed?             │ ── Same → BusinessRuleViolationException
│  - New bed available + active?   │ ── No → BusinessRuleViolationException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ 1. Release old bed (occupied=F)  │
│ 2. Occupy new bed (occupied=T)   │
│ 3. Update admission → new bed    │
└─────────┬────────────────────────┘
          ▼
     AdmissionResponse
```

---

## Cross-Service Integration

```
                    AdmissionService
                    ┌───────────────┐
 AppointmentService │               │ BillingService
  (close OPD)  ◄────┤   ADMIT       │
                    │               ├────► (generate bill on discharge)
                    │   DISCHARGE   │
 EncounterRepo  ◄────┤               │
  (create/close     │   TRANSFER    │
   IPD encounter)   └───────────────┘
                    BedRepository
                    (lock, occupy, release)
```

---

## Where This Service Is Used

| Consumer | Operations |
|----------|-----------|
| `AdmissionController` | Admit, discharge, transfer, list active admissions |
| `DashboardServiceImpl` | `admissionRepository.count()` for critical patient count |
