# Vitals Service — Code Flow Documentation

## Module Overview

| Aspect | Details |
|--------|---------|
| **Package** | `com.hms.HospitalManagementSystem.service.impl` |
| **Class** | `VitalsServiceImpl` |
| **Purpose** | Recording and retrieving patient vital signs during encounters (triage step) |
| **Used By** | `EncounterController` (vitals endpoint) |
| **Depends On** | `VitalsRepository`, `EncounterRepository`, `UserRepository` |

---

## Purpose & Responsibility

VitalsService handles **recording patient vital signs** during the triage phase of an encounter. This is typically performed by a **nurse** before the doctor consultation begins. Supported measurements:
- Temperature, Blood Pressure (systolic/diastolic), Pulse, SpO2
- Weight, Height, and auto-calculated **BMI**

---

## Service Flow Diagrams

### 1. Save Vitals (`saveVitals`)

```
Nurse POST /api/encounters/{encounterId}/vitals
  { temperature, systolic, diastolic, pulse, spo2, weight, height }
        │
        ▼
┌──────────────────────────────────┐
│ Validate:                        │
│  - Encounter exists?             │ ── No → ResourceNotFoundException
│  - User (nurse) exists?          │ ── No → ResourceNotFoundException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Upsert Logic:                    │
│  - Find existing vitals for      │
│    this encounter                │
│  - If found → update fields      │
│  - If not → create new record    │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Set all vital fields:            │
│  temperature, systolic,          │
│  diastolic, pulse, spo2,        │
│  weight, height                  │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Auto-calculate BMI:              │
│  if weight & height provided:    │
│  BMI = weight / (height/100)²    │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Set metadata:                    │
│  recordedBy = current user       │
│  recordedAt = now                │
└─────────┬────────────────────────┘
          ▼
     Save → Return Vitals
```

**Key Details:**
- **Upsert pattern**: If vitals already exist for the encounter, they are updated (not duplicated)
- **BMI auto-calculation**: Height is assumed in **centimeters**, converted to meters for the formula
- **One vitals record per encounter** (for OPD). IPD rounds may create additional records via `EncounterService.addRound()`

### 2. Get Vitals (`getVitalsByEncounterId`)

```
Client GET /api/encounters/{encounterId}/vitals
        │
        ▼
┌──────────────────────────────────┐
│ findByEncounterId(id)            │ ── Not found → ResourceNotFoundException
└─────────┬────────────────────────┘
          ▼
     Return Vitals entity
```

---

## Where This Service Is Used

| Consumer | Operations |
|----------|-----------|
| `EncounterController` | Save and retrieve vitals for encounters |
| `EncounterServiceImpl.addRound()` | Directly saves vitals via `VitalsRepository` during IPD rounds (bypasses this service) |

---

## Data Flow in Clinical Context

```
Patient checks in → Encounter created (TRIAGE)
        │
        ▼
Nurse records vitals → VitalsService.saveVitals()
        │
        ▼
Doctor sees patient in queue → Views vitals → Starts consultation
```

> Vitals recording is the **key triage activity** that prepares the encounter for doctor consultation. The encounter remains in TRIAGE status until the doctor begins writing clinical notes.
