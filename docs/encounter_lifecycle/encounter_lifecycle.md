# Encounter Lifecycle — HMS

> **Encounter** = A single clinical interaction between a patient and a healthcare provider.
> It is the **central record** connecting vitals, diagnosis, prescriptions, lab requests, and billing.

---

## Table of Contents

1. [What is an Encounter?](#what-is-an-encounter)
2. [Entity Relationship Diagram](#entity-relationship-diagram)
3. [Encounter Statuses](#encounter-statuses)
4. [OPD (Outpatient) Flow](#opd-outpatient-flow)
5. [IPD (Inpatient) Flow](#ipd-inpatient-flow)
6. [Child Entities — What Lives Inside an Encounter](#child-entities)
7. [API Reference](#api-reference)
8. [Database Schema](#database-schema)
9. [Business Rules & Constraints](#business-rules--constraints)
10. [Frontend Module Mapping](#frontend-module-mapping)

---

## What is an Encounter?

An `Encounter` is the **core clinical record** for every patient visit — whether OPD or IPD. It captures:

| Aspect         | Description                                                  |
| -------------- | ------------------------------------------------------------ |
| **Who**        | Patient + Doctor                                             |
| **When**       | `startedAt` → `completedAt`                                  |
| **Why**        | Chief complaint, diagnosis                                   |
| **What happened** | Vitals recorded, clinical notes, prescriptions, lab orders |
| **Where**      | Linked to an OPD `Appointment` **or** an IPD `Admission`    |

### Key Rules

- An **OPD encounter** is linked to exactly **one Appointment** (`appointment_id`).
- An **IPD encounter** is linked to exactly **one Admission** (`admission_id`). This is a single, long-lived encounter for the entire stay.
- An encounter **cannot** be linked to both an Appointment and an Admission simultaneously.

---

## Entity Relationship Diagram

```
┌──────────────────┐
│   Appointment    │       OPD Path
│ (One-to-One)     │◄──────────────────┐
└──────────────────┘                   │
                                       │
                                ┌──────┴──────────┐
                                │                  │
       IPD Path                 │    ENCOUNTER     │
┌──────────────────┐            │                  │
│    Admission     │◄───────────│  patient_id (FK) │
│ (Many-to-One)    │            │  doctor_id  (FK) │
└──────────────────┘            │  status           │
                                │  chiefComplaint   │
                                │  diagnosis        │
                                │  notes            │
                                │  startedAt        │
                                │  completedAt      │
                                └──────────────────┘
                                   │    │    │
                     ┌─────────────┤    │    ├─────────────┐
                     ▼             ▼    │    ▼             ▼
               ┌──────────┐ ┌──────────┐│ ┌──────────┐ ┌──────────┐
               │  Vitals  │ │  Round   ││ │Prescri-  │ │ Lab      │
               │ (1..N)   │ │ (1..N)   ││ │ption     │ │ Request  │
               │          │ │ IPD only ││ │ (1..N)   │ │ (1..N)   │
               └──────────┘ └──────────┘│ └──────────┘ └──────────┘
                                        │                    │
                                        │               ┌────┴─────┐
                                        │               │LabResult │
                                        │               │  (1..N)  │
                                        │               └──────────┘
                                        ▼
                                  (Billing via
                                   Admission
                                   discharge)
```

---

## Encounter Statuses

```java
public enum EncounterStatus {
    TRIAGE,        // Nurse is recording vitals
    IN_PROGRESS,   // Doctor is consulting / IPD patient is under care
    COMPLETED,     // Consultation/Stay finished
    CANCELLED      // Encounter cancelled
}
```

### Status Transition Rules

```
OPD:  TRIAGE → IN_PROGRESS → COMPLETED
IPD:  IN_PROGRESS → COMPLETED (created directly as IN_PROGRESS)

Any state → CANCELLED (manual)
```

| Transition             | Trigger                          | Who       |
| ---------------------- | -------------------------------- | --------- |
| → `TRIAGE`             | Encounter created for OPD        | System    |
| `TRIAGE` → `IN_PROGRESS` | Doctor saves clinical notes    | Doctor    |
| → `IN_PROGRESS`        | IPD Encounter created at admission | System  |
| `IN_PROGRESS` → `COMPLETED` | Doctor finishes consultation / discharge | Doctor |
| Any → `CANCELLED`      | Manual cancellation              | Doctor    |

---

## OPD (Outpatient) Flow

This is the standard walk-in or appointment-based visit.

### Step-by-Step

```
┌─────────┐     ┌─────────┐     ┌───────────────┐     ┌──────────────┐     ┌───────────┐
│ Schedule │────▶│ Check-In│────▶│ Triage        │────▶│ Consultation │────▶│ Complete  │
│Appointment│    │         │     │ (Vitals)      │     │ (Diagnosis,  │     │           │
│          │     │         │     │               │     │  Rx, Lab)    │     │           │
└─────────┘     └─────────┘     └───────────────┘     └──────────────┘     └───────────┘
  SCHEDULED      CHECKED_IN        TRIAGE               IN_PROGRESS          COMPLETED
 (Appt Status)  (Appt Status)   (Enc Status)          (Enc Status)        (Enc Status)
```

### 1. Appointment Scheduled

- **Who**: Front desk / Patient self-registration
- **What**: Creates an `Appointment` record with status `SCHEDULED`
- **Encounter**: Not yet created

### 2. Encounter Created (Check-In / Triage Start)

- **Trigger**: Nurse or system calls `POST /api/v1/encounters`
- **Input**: `{ appointmentId, patientId, doctorId }`
- **What happens**:
  - Validates appointment is not `CANCELLED` or `NO_SHOW`
  - Validates patient ID matches appointment
  - Validates doctor ID matches appointment
  - Creates `Encounter` with status `TRIAGE`
  - Updates `Appointment` status: `SCHEDULED → CHECKED_IN`
- **Idempotent**: If encounter already exists for this appointment, returns the existing one

### 3. Vitals Recorded (Triage)

- **Who**: Nurse (via Triage module)
- **What**: Records vitals (temperature, BP, pulse, SpO2, weight, height, BMI)
- Creates a `Vitals` entity linked to the encounter
- Encounter remains in `TRIAGE` status

### 4. Doctor Consultation

- **Who**: Doctor (via Consultation module)
- **What**: Doctor opens the encounter, writes clinical notes
- **Trigger**: `PATCH /api/v1/encounters/{id}/clinical-notes`
- **Status change**: `TRIAGE → IN_PROGRESS`
- **Appointment status**: `CHECKED_IN → IN_PROGRESS`
- **During consultation, doctor can**:
  - Update chief complaint, diagnosis, and notes
  - Add prescriptions (saved as `DRAFT`)
  - Order lab tests (`POST /api/v1/lab-requests`)

### 5. Encounter Completed

- **Trigger**: `PATCH /api/v1/encounters/{id}/complete`
- **Validation**: Diagnosis must not be empty
- **What happens**:
  - Encounter status → `COMPLETED`
  - `completedAt` timestamp set
  - All `Prescription` items → status `ISSUED`, `issuedAt` set
  - Appointment status → `COMPLETED`

---

## IPD (Inpatient) Flow

This is for patients who need to be admitted and stay in the hospital.

### Step-by-Step

```
┌──────────┐    ┌───────────────┐    ┌───────────┐    ┌───────────┐    ┌───────────┐
│  Admit   │───▶│ Encounter     │───▶│  Rounds   │───▶│ Discharge │───▶│  Billing  │
│ Patient  │    │ Created       │    │ (Daily)   │    │           │    │ Generated │
│          │    │ (IN_PROGRESS) │    │ + Vitals  │    │           │    │           │
└──────────┘    └───────────────┘    └───────────┘    └───────────┘    └───────────┘
  ADMITTED        IN_PROGRESS         IN_PROGRESS       DISCHARGED      (Automatic)
 (Adm Status)    (Enc Status)       (Enc Status)      (Adm Status)
```

### 1. Patient Admission

- **Trigger**: `POST /api/v1/ipd/admissions`
- **Input**: `{ patientId, doctorId, bedId, diagnosis }`
- **What happens**:
  1. Validates patient is not already admitted
  2. Validates bed is available and active
  3. Closes any active OPD appointments for this patient
  4. Marks bed as occupied
  5. Creates `Admission` record (status `ADMITTED`)
  6. **Automatically creates an `Encounter`** with status `IN_PROGRESS` and `admission_id` set
- **Important**: IPD encounters skip `TRIAGE` and start directly as `IN_PROGRESS`

### 2. Doctor Rounds (Daily Visits)

- **Trigger**: `POST /api/v1/encounters/rounds`
- **Input**: `{ admissionId, notes, temperature, systolic, diastolic, pulse, spo2 }`
- **What happens**:
  - Finds the active encounter for this admission
  - Creates a `Round` record with doctor's notes
  - If vitals are provided, creates a new `Vitals` entry (added to `vitalsHistory`)
- **Key Design**: IPD has **one long-lived encounter** per admission. Rounds are sub-records, not new encounters.

### 3. Lab Tests & Prescriptions

- Same mechanism as OPD: lab requests and prescriptions are linked to the encounter

### 4. Patient Discharge

- **Trigger**: `POST /api/v1/ipd/admissions/{id}/discharge`
- **Input**: `{ dischargeSummary, diagnosis, advice, dischargeDate }`
- **What happens**:
  1. Updates admission: status → `DISCHARGED`, sets discharge date, summary, advice
  2. Releases bed (marks as unoccupied)
  3. **Auto-generates billing** via `BillingService.generateBill()`
  4. Closes any remaining active appointments for the patient

### 5. Bed Transfer (During Stay)

- **Trigger**: `PUT /api/v1/ipd/admissions/{id}/transfer`
- Releases old bed, occupies new bed, updates admission record

---

## Child Entities

### Vitals

| Field        | Type         | Description        |
| ------------ | ------------ | ------------------ |
| temperature  | BigDecimal   | °C                 |
| systolic     | Integer      | mmHg (BP upper)    |
| diastolic    | Integer      | mmHg (BP lower)    |
| pulse        | Integer      | bpm                |
| spo2         | Integer      | % oxygen           |
| weight       | BigDecimal   | kg                 |
| height       | BigDecimal   | cm                 |
| bmi          | BigDecimal   | Calculated         |
| recordedBy   | User (FK)    | Nurse who recorded |
| recordedAt   | LocalDateTime| When recorded      |

- **OPD**: Usually 1 vitals entry during triage
- **IPD**: Multiple vitals entries (one per round), forming a `vitalsHistory`
- Helper method `encounter.getVitals()` returns the **latest** vitals entry

### Round (IPD Only)

| Field     | Type          | Description                     |
| --------- | ------------- | ------------------------------- |
| encounter | Encounter(FK) | Parent encounter                |
| doctor    | User (FK)     | Doctor performing the round     |
| notes     | TEXT          | Round observations              |
| createdAt | LocalDateTime | When the round was performed    |

### Prescription

| Field   | Type               | Description                              |
| ------- | ------------------ | ---------------------------------------- |
| encounter | Encounter (FK)   | Parent encounter                         |
| note    | TEXT               | General prescription note                |
| status  | `DRAFT` / `ISSUED` | Draft while editing, Issued on completion |
| items   | List\<PrescriptionItem\> | Individual medicine entries         |
| issuedAt | LocalDateTime     | Set when encounter is completed          |

- Prescriptions start as `DRAFT` when created during consultation
- They are automatically set to `ISSUED` when the encounter is completed

### Lab Request

| Field        | Type              | Description                       |
| ------------ | ----------------- | --------------------------------- |
| encounter    | Encounter (FK)    | Ordering encounter                |
| patient      | Patient (FK)      | Patient being tested              |
| labTest      | LabTestCatalog(FK)| Test from the catalog             |
| testName     | String            | Snapshot of test name at creation |
| status       | Enum              | `ORDERED → SAMPLED → COMPLETED` or `CANCELLED` |
| labResults   | List\<LabResult\> | Parameter-level results           |

- Doctor orders tests during consultation
- Lab technician enters results → status becomes `COMPLETED`
- Doctor can view results in the encounter's Lab tab

---

## API Reference

### Encounter Endpoints

| Method  | Endpoint                                    | Description                        | Permission           |
| ------- | ------------------------------------------- | ---------------------------------- | -------------------- |
| POST    | `/api/v1/encounters`                        | Start/create an encounter          | VITALS_WRITE, CONSULTATION_WRITE |
| GET     | `/api/v1/encounters/{id}`                   | Get encounter by ID                | CONSULTATION_READ    |
| PATCH   | `/api/v1/encounters/{id}/clinical-notes`    | Update diagnosis, notes            | CONSULTATION_WRITE   |
| PATCH   | `/api/v1/encounters/{id}/complete`          | Complete encounter                 | CONSULTATION_WRITE   |
| GET     | `/api/v1/encounters/{id}/clinical-notes`    | Get clinical notes                 | CONSULTATION_READ    |
| GET     | `/api/v1/encounters/by-appointment/{id}`    | Get encounter by appointment       | CONSULTATION_READ    |
| GET     | `/api/v1/encounters/patient/{patientId}`    | Get all patient encounters         | CONSULTATION_READ, PATIENT_VIEW |
| GET     | `/api/v1/encounters/queue/triage`           | Triage queue (TRIAGE status)       | VITALS_READ          |
| GET     | `/api/v1/encounters/queue/doctor/{id}`      | All encounters for a doctor        | CONSULTATION_READ    |
| GET     | `/api/v1/encounters/queue/opd/doctor/{id}`  | OPD-only queue for a doctor        | CONSULTATION_READ    |
| GET     | `/api/v1/encounters/queue/ipd/doctor/{id}`  | IPD-only queue for a doctor        | CONSULTATION_READ    |
| POST    | `/api/v1/encounters/rounds`                 | Add IPD round                      | CONSULTATION_WRITE   |

### Related Endpoints

| Method | Endpoint                              | Description               |
| ------ | ------------------------------------- | ------------------------- |
| POST   | `/api/v1/lab-requests`                | Order a lab test          |
| GET    | `/api/v1/lab-requests?encounterId=X`  | Get lab requests for encounter |
| POST   | `/api/v1/lab-requests/{id}/results`   | Enter lab results         |
| POST   | `/api/v1/ipd/admissions`              | Admit patient (creates encounter) |
| POST   | `/api/v1/ipd/admissions/{id}/discharge` | Discharge patient       |

---

## Database Schema

### `encounters` Table

```sql
CREATE TABLE encounters (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id  BIGINT UNIQUE,         -- FK → appointments (OPD, nullable)
    admission_id    BIGINT,                -- FK → admissions   (IPD, nullable)
    patient_id      BIGINT NOT NULL,       -- FK → patients
    doctor_id       BIGINT NOT NULL,       -- FK → users
    chief_complaint TEXT,
    diagnosis       TEXT,
    notes           TEXT,
    status          VARCHAR(50) NOT NULL,  -- TRIAGE, IN_PROGRESS, COMPLETED, CANCELLED
    started_at      DATETIME NOT NULL,
    completed_at    DATETIME,
    is_deleted      BOOLEAN DEFAULT FALSE,

    INDEX idx_encounter_appointment (appointment_id),
    INDEX idx_encounter_patient     (patient_id),
    INDEX idx_encounter_doctor      (doctor_id),
    INDEX idx_encounter_status      (status)
);
```

### Related Tables

```sql
-- Vitals (1:N with Encounter)
CREATE TABLE vitals (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    encounter_id  BIGINT NOT NULL,       -- FK → encounters
    temperature   DECIMAL(5,2),
    systolic      INT,
    diastolic     INT,
    pulse         INT,
    spo2          INT,
    weight        DECIMAL(5,2),
    height        DECIMAL(5,2),
    bmi           DECIMAL(5,2),
    recorded_at   DATETIME NOT NULL,
    recorded_by   BIGINT NOT NULL,       -- FK → users
    created_at    DATETIME,
    updated_at    DATETIME
);

-- Rounds (1:N with Encounter, IPD only)
CREATE TABLE rounds (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    encounter_id  BIGINT NOT NULL,       -- FK → encounters
    doctor_id     BIGINT NOT NULL,       -- FK → users
    notes         TEXT,
    created_at    DATETIME,
    updated_at    DATETIME
);

-- Prescriptions (1:N with Encounter)
CREATE TABLE prescriptions (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    encounter_id  BIGINT NOT NULL,       -- FK → encounters
    note          TEXT,
    status        VARCHAR(20) NOT NULL,  -- DRAFT, ISSUED
    issued_at     DATETIME,
    created_at    DATETIME,
    updated_at    DATETIME
);

-- Lab Requests (1:N with Encounter)
CREATE TABLE lab_requests (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    encounter_id     BIGINT NOT NULL,    -- FK → encounters
    patient_id       BIGINT NOT NULL,    -- FK → patients
    lab_test_id      BIGINT,             -- FK → lab_test_catalog
    test_name        VARCHAR(255) NOT NULL,
    status           VARCHAR(20) NOT NULL, -- ORDERED, SAMPLED, COMPLETED, CANCELLED
    technician_notes TEXT,
    created_at       DATETIME,
    updated_at       DATETIME
);
```

---

## Business Rules & Constraints

### OPD Rules

1. **One encounter per appointment** — enforced by unique constraint on `appointment_id`
2. Encounter cannot be created for `CANCELLED` or `NO_SHOW` appointments
3. Patient ID and Doctor ID must match the appointment
4. **Diagnosis is required** to complete an encounter
5. Only the **assigned doctor** can modify or complete their own encounters
6. Clinical notes cannot be updated after encounter is `COMPLETED`

### IPD Rules

1. **One encounter per admission** — a single long-lived encounter
2. Encounter is created automatically during admission (status `IN_PROGRESS`)
3. Multiple **rounds** can be added to the same encounter
4. Each round can optionally include new vitals (building `vitalsHistory`)
5. When patient is **discharged**, the bed is freed and billing is auto-generated
6. Active OPD appointments are automatically closed when a patient is admitted

### Prescription Rules

1. Created as `DRAFT` during consultation
2. Automatically set to `ISSUED` when encounter is completed
3. `issuedAt` timestamp is set at that moment

### Lab Request Rules

1. Linked to an encounter at creation time
2. Lab technician enters results → status becomes `COMPLETED`
3. Results cannot be added to `COMPLETED` or `CANCELLED` requests

---

## Frontend Module Mapping

| Module         | Angular Route                    | Encounter Role                        |
| -------------- | -------------------------------- | ------------------------------------- |
| **Triage**     | `/triage`                        | Creates encounter, records vitals     |
| **Consultation** | `/consultation/:encounterId`   | Doctor writes notes, Rx, lab orders   |
| **Lab**        | `/lab/entry/:requestId`          | Technician enters results             |
| **Lab Report** | `/lab/view/:requestId`           | View completed results                |
| **IPD Admissions** | `/ipd/admissions`            | Lists admitted patients               |
| **IPD Discharge** | `/ipd/discharge/:admissionId` | Discharge form and summary            |

### OPD Frontend Flow

```
Appointments List → Start Encounter (Triage Module)
  → Record Vitals → Save
  → Consultation Queue shows patient
  → Doctor opens Consultation (by encounterId)
    → Tab: Vitals (read-only, from triage)
    → Tab: Diagnosis & Notes (editable)
    → Tab: Prescription (add medicines)
    → Tab: Lab Requests (order tests, view results)
  → Click "Finish Consultation" → Encounter COMPLETED
```

### IPD Frontend Flow

```
IPD → Admit Patient (select patient, doctor, bed)
  → Encounter auto-created (IN_PROGRESS)
  → IPD Queue shows patient
  → Doctor adds Rounds (notes + optional vitals)
  → Doctor can order labs, prescriptions (same as OPD)
  → Discharge page → Fill summary → Confirm
    → Admission DISCHARGED, Bed released, Bill generated
```

---

## Summary

The **Encounter** is the single most important clinical entity in the HMS. It bridges the gap between administrative processes (appointments, admissions) and clinical data (vitals, diagnosis, prescriptions, lab results).

- **OPD**: Short-lived, tied to one appointment, flows through triage → consultation → completion
- **IPD**: Long-lived, tied to one admission, accumulates rounds and vitals over the stay

Every clinical action in the system — recording vitals, writing a diagnosis, prescribing medicine, ordering a lab test — is anchored to an Encounter.
