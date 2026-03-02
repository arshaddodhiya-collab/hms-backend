# Appointment Service — Code Flow Documentation

## Module Overview

| Aspect | Details |
|--------|---------|
| **Package** | `com.hms.HospitalManagementSystem.service.impl` |
| **Class** | `AppointmentServiceImpl` |
| **Purpose** | OPD appointment lifecycle — booking, status transitions, doctor schedule management |
| **Used By** | `AppointmentController`, `AdmissionServiceImpl` |
| **Depends On** | `AppointmentRepository`, `PatientRepository`, `UserRepository`, `EncounterRepository` |

---

## Purpose & Responsibility

AppointmentService manages the **full lifecycle of outpatient (OPD) appointments** from booking through completion. It enforces:
- Time slot validation and overlap prevention
- State machine transitions for appointment status
- Integration with the Encounter system (creating encounters on check-in)
- Soft delete/restore capability
- Bulk completion for IPD admission transitions

---

## Appointment Status State Machine

```
  SCHEDULED ──────► CHECKED_IN ──────► IN_PROGRESS ──────► COMPLETED
      │                                     │
      │                                     │
      ▼                                     ▼
  CANCELLED                            COMPLETED
      
  Any (except COMPLETED) ──────► NO_SHOW
```

| Transition | Trigger Method | Business Rule |
|-----------|----------------|---------------|
| → `SCHEDULED` | `bookAppointment()` | Initial state on creation |
| `SCHEDULED` → `CHECKED_IN` | `checkIn()` | Creates an Encounter (TRIAGE status) |
| `CHECKED_IN` → `IN_PROGRESS` | `startConsultation()` | Only from CHECKED_IN |
| `IN_PROGRESS` → `COMPLETED` | `completeAppointment()` | Encounter must be completed first |
| Any → `CANCELLED` | `cancelAppointment()` | Cannot cancel if encounter active or already completed |
| Any (except COMPLETED) → `NO_SHOW` | `markNoShow()` | Cannot mark completed as no-show |

---

## Service Flow Diagrams

### 1. Book Appointment (`bookAppointment`)

```
Client POST /api/appointments
        │
        ▼
┌──────────────────────────────────┐
│ Validate Input:                  │
│  - Start time must be future     │
│  - End must be after start       │
│  - Duration: 15-120 minutes      │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Convert Instant → LocalDateTime  │
│  (using system timezone)         │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Validate entities:               │
│  - Patient exists?               │ ── No → ResourceNotFoundException
│  - Doctor exists?                │ ── No → ResourceNotFoundException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Check doctor time overlap        │ ── Overlap → ConflictException
│  (existsOverlappingAppointment)  │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Create Appointment entity        │
│  status = SCHEDULED              │
│  type = from request (enum)      │
└─────────┬────────────────────────┘
          ▼
     Save → Return Appointment
```

**Key Details:**
- Time is received as `Instant`, converted to `LocalDateTime` using system timezone for JPA storage
- The overlap check uses a custom repository query to detect conflicting doctor schedules
- Duration constraints: minimum 15 min, maximum 2 hours

### 2. Check-In Flow (`checkIn`) — **Critical Encounter Integration**

```
Client PATCH /api/appointments/{id}/check-in
        │
        ▼
┌──────────────────────────────────┐
│ Validate:                        │
│  status must be SCHEDULED        │ ── Not? → ConflictException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Update status → CHECKED_IN       │
│ Save appointment                 │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ ★ Create Encounter entity        │
│   - Status: TRIAGE               │
│   - Links: appointment, patient, │
│     doctor                       │
│   - Start time: now              │
└─────────┬────────────────────────┘
          ▼
     Return Appointment
```

> **This is the bridge from Appointment → Encounter.** When a patient checks in, the system creates an Encounter in TRIAGE status, which enters the nurse's triage queue.

### 3. Complete Active Appointments for Patient (`completeActiveAppointmentsForPatient`)

```
Called internally by AdmissionService
        │
        ▼
┌──────────────────────────────────┐
│ Fetch all patient appointments   │
│ Filter:                          │
│  - Not deleted                   │
│  - Status: SCHEDULED/CHECKED_IN/ │
│    IN_PROGRESS                   │
│  - Today or past appointments    │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Bulk update all → COMPLETED      │
│ (Transition from OPD to IPD)     │
└──────────────────────────────────┘
```

> **This is called when a patient is admitted (IPD).** All active OPD appointments are auto-completed to avoid conflicts between OPD and IPD workflows.

### 4. Get Upcoming Appointments for Doctor (`getUpcomingAppointmentsForDoctor`)

```
Client GET /api/appointments/doctor/{id}/upcoming
        │
        ▼
┌──────────────────────────────────┐
│ Query: doctor's today appts      │
│ Filter: SCHEDULED, CHECKED_IN,   │
│   or IN_PROGRESS only            │
│ Sort: by start time ascending    │
└─────────┬────────────────────────┘
          ▼
     List<Appointment>
  (Used for doctor dashboard)
```

---

## Where This Service Is Used

| Consumer | Operations |
|----------|-----------|
| `AppointmentController` | All booking/scheduling CRUD |
| `AdmissionServiceImpl` | `completeActiveAppointmentsForPatient()` during admission and discharge |
| `EncounterServiceImpl` | Reads appointment during encounter creation |
| `DashboardServiceImpl` | `appointmentRepository` for today's appointment counts |

---

## Error Handling

| Error | When |
|-------|------|
| `ValidationException` | Past date, end before start, invalid duration |
| `ConflictException` | Doctor time overlap, invalid status transition, active encounter conflict |
| `ResourceNotFoundException` | Patient or doctor not found |
