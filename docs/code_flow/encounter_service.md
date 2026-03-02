# Encounter Service — Code Flow Documentation

## Module Overview

| Aspect | Details |
|--------|---------|
| **Package** | `com.hms.HospitalManagementSystem.service.impl` |
| **Class** | `EncounterServiceImpl` |
| **Purpose** | Clinical encounter lifecycle — from triage through diagnosis to completion; serves both OPD and IPD workflows |
| **Used By** | `EncounterController`, `AppointmentServiceImpl`, `AdmissionServiceImpl` |
| **Depends On** | `EncounterRepository`, `AppointmentRepository`, `UserRepository`, `RoundRepository`, `VitalsRepository`, `EncounterMapper` |

---

## Purpose & Responsibility

EncounterService is the **core clinical workflow engine**. An Encounter represents a single clinical interaction between a doctor and a patient. It manages:
- **OPD Encounters** — created when a patient checks in for an appointment
- **IPD Encounters** — created when a patient is admitted
- **Triage → Consultation → Completion** flow
- **Clinical Notes** — chief complaint, diagnosis, and doctor notes
- **IPD Rounds** — daily doctor rounds for admitted patients
- **Queue Management** — triage queue, doctor consultation queues, OPD/IPD separation

---

## Encounter Status State Machine

```
     TRIAGE ─────────► IN_PROGRESS ─────────► COMPLETED
   (Nurse records       (Doctor                (Encounter
    vitals)              consults)              finalized)
```

| Status | Who | What Happens |
|--------|-----|-------------|
| `TRIAGE` | Nurse | Created on appointment check-in or admission; nurse records vitals |
| `IN_PROGRESS` | Doctor | Doctor starts clinical notes; transitions automatically |
| `COMPLETED` | Doctor | Diagnosis finalized; prescriptions issued; appointment auto-completed |

---

## Service Flow Diagrams

### 1. Start Encounter (`startEncounter`)

```
Client POST /api/encounters/start
  { appointmentId, patientId, doctorId }
        │
        ▼
┌──────────────────────────────────┐
│ Check: Encounter already exists  │
│  for this appointment?           │
│   YES → return existing          │
│   NO  → create new              │
└─────────┬────────────────────────┘
          ▼ (create path)
┌──────────────────────────────────┐
│ Validate:                        │
│  - Appointment exists?           │ ── No → ResourceNotFoundException
│  - Not CANCELLED/NO_SHOW?        │ ── Yes → ConflictException
│  - PatientId matches appt?       │ ── No → ConflictException
│  - DoctorId matches appt?        │ ── No → ConflictException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Create Encounter:                │
│  status = TRIAGE                 │
│  links: appointment, patient,    │
│    doctor                        │
│  startedAt = now                 │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ If appointment SCHEDULED →       │
│  auto-update to CHECKED_IN       │
└─────────┬────────────────────────┘
          ▼
     EncounterResponse
```

### 2. Update Clinical Notes (`updateClinicalNotes`)

```
Doctor PUT /api/encounters/{id}/notes
  { chiefComplaint, diagnosis, notes }
        │
        ▼
┌──────────────────────────────────┐
│ Validate:                        │
│  - Encounter not COMPLETED?      │ ── Yes → ConflictException
│  - Doctor owns this encounter?   │ ── No → AccessDeniedException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Set clinical fields:             │
│  - chiefComplaint                │
│  - diagnosis                     │
│  - notes                         │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ If status == TRIAGE:             │
│  → auto-transition to            │
│    IN_PROGRESS                   │
│  → update appointment status to  │
│    IN_PROGRESS                   │
└─────────┬────────────────────────┘
          ▼
     EncounterResponse
```

> **Automatic status transition:** When the doctor starts writing notes, the encounter transitions from TRIAGE → IN_PROGRESS automatically. This is the trigger that moves the patient from the nurse's queue to the doctor's active consultation.

### 3. Complete Encounter (`completeEncounter`)

```
Doctor POST /api/encounters/{id}/complete
        │
        ▼
┌──────────────────────────────────┐
│ Validate:                        │
│  - Status is IN_PROGRESS?        │
│  - Doctor owns this encounter?   │ ── No → AccessDeniedException
│  - Diagnosis is provided?        │ ── No → ConflictException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Finalize Encounter:              │
│  status = COMPLETED              │
│  completedAt = now               │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Auto-complete appointment:       │
│  appointment.status = COMPLETED  │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Issue prescriptions:             │
│  All DRAFT prescriptions →       │
│    status = ISSUED               │
│    issuedAt = now                │
└─────────┬────────────────────────┘
          ▼
     EncounterResponse
```

> **Side effects on completion:** Completing an encounter automatically completes the parent appointment and issues all pending prescriptions. This is a critical cascade that ensures data consistency.

### 4. Add IPD Round (`addRound`)

```
Doctor POST /api/encounters/rounds
  { admissionId, notes, vitals... }
        │
        ▼
┌──────────────────────────────────┐
│ Find active encounter for        │
│  this admission                  │ ── Not found → ResourceNotFoundException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Create Round:                    │
│  links: encounter, doctor        │
│  notes from request              │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ If vitals data provided:         │
│  Create Vitals record linked     │
│  to the encounter                │
│  (temperature, BP, pulse, etc.)  │
└─────────┬────────────────────────┘
          ▼
     RoundResponse
```

---

## Queue Management

| Queue | Method | Filter Logic |
|-------|--------|-------------|
| **Triage Queue** | `getTriageQueue()` | All encounters with status `TRIAGE` |
| **Doctor Queue (All)** | `getDoctorQueue(doctorId)` | Doctor's encounters in `TRIAGE` or `IN_PROGRESS` |
| **OPD Doctor Queue** | `getOpdDoctorQueue(doctorId)` | Same as above but `appointment IS NOT NULL` |
| **IPD Doctor Queue** | `getIpdDoctorQueue(doctorId)` | Doctor's encounters with `admission IS NOT NULL` and `IN_PROGRESS` |
| **Patient History** | `getPatientEncounters(patientId)` | All encounters for a patient |

> The OPD vs IPD separation is based on relationships: OPD encounters have an `appointment`; IPD encounters have an `admission`.

---

## Where This Service Is Used

| Consumer | Operations |
|----------|-----------|
| `EncounterController` | All encounter operations, clinical notes, rounds, queues |
| `AppointmentServiceImpl.checkIn()` | Creates encounter directly via `EncounterRepository` |
| `AdmissionServiceImpl.admitPatient()` | Creates IPD encounter directly via `EncounterRepository` |
| `VitalsController` | References encounter for vitals recording |
| `PrescriptionController` | References encounter for prescription saving |
| `LabController` | References encounter for lab requests |

---

## Error Handling

| Error | When |
|-------|------|
| `ResourceNotFoundException` | Encounter, appointment, or doctor not found |
| `ConflictException` | Attempt to modify completed encounter, mismatched patient/doctor, no diagnosis |
| `AccessDeniedException` | Non-owning doctor tries to modify encounter |
