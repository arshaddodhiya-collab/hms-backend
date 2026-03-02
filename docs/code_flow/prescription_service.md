# Prescription Service — Code Flow Documentation

## Module Overview

| Aspect | Details |
|--------|---------|
| **Package** | `com.hms.HospitalManagementSystem.service.impl` |
| **Class** | `PrescriptionServiceImpl` |
| **Purpose** | Creating and managing medication prescriptions within encounters |
| **Used By** | `EncounterController` (prescription endpoint) |
| **Depends On** | `PrescriptionRepository`, `EncounterRepository` |

---

## Purpose & Responsibility

PrescriptionService manages **medication prescriptions** attached to clinical encounters. Key behaviors:
- **Create or update** prescriptions within active encounters
- **Replace items** — full replacement of prescription items on each save
- **Doctor ownership** — only the encounter's doctor can prescribe
- **DRAFT → ISSUED** lifecycle — prescriptions start as DRAFT and are issued when the encounter completes

---

## Prescription Status Lifecycle

```
  DRAFT ──────────► ISSUED
 (During             (On encounter
  consultation)       completion)
```

| Status | Set By | Description |
|--------|--------|-------------|
| `DRAFT` | `savePrescription()` | Created during consultation; can be modified |
| `ISSUED` | `EncounterService.completeEncounter()` | Finalized when encounter is completed |

---

## Service Flow Diagrams

### 1. Save Prescription (`savePrescription`)

```
Doctor POST /api/encounters/{encounterId}/prescriptions
  { note, items: [{ medicineName, dosage, frequency, duration }] }
        │
        ▼
┌──────────────────────────────────┐
│ Validate:                        │
│  - Encounter exists?             │ ── No → ResourceNotFoundException
│  - Encounter not COMPLETED?      │ ── Yes → ConflictException
│  - Doctor owns encounter?        │ ── No → AccessDeniedException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Upsert Logic:                    │
│  - Find existing DRAFT           │
│    prescription for encounter    │
│  - If found → update it          │
│  - If not → create new DRAFT     │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Replace All Items:               │
│  1. Clear existing items list    │
│  2. Build new PrescriptionItems  │
│     from request data            │
│  3. Link items → prescription    │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Set prescription note            │
│ Save prescription (cascade items)│
└─────────┬────────────────────────┘
          ▼
     Return Prescription
```

**Key Details:**
- **Full replacement pattern**: Every save clears all existing items and creates new ones. This simplifies the UI — the frontend always sends the complete prescription.
- **One active DRAFT per encounter**: The service finds the existing DRAFT prescription to update, or creates a new one.
- Items are saved via JPA cascade from the parent `Prescription` entity.

### 2. Get Prescription (`getPrescriptionByEncounterId`)

```
Client GET /api/encounters/{encounterId}/prescriptions
        │
        ▼
┌──────────────────────────────────┐
│ Find prescriptions by encounterId│
│ Return first found               │ ── Not found → ResourceNotFoundException
└─────────┬────────────────────────┘
          ▼
     Return Prescription (with items)
```

---

## Prescription Item Structure

Each `PrescriptionItem` contains:

| Field | Description | Example |
|-------|-------------|---------|
| `medicineName` | Name of the medication | "Amoxicillin 500mg" |
| `dosage` | Dosage per intake | "1 tablet" |
| `frequency` | How often | "3 times daily" |
| `duration` | Treatment length | "7 days" |

---

## Where This Service Is Used

| Consumer | Operations |
|----------|-----------|
| `EncounterController` | Save and retrieve prescriptions |
| `EncounterServiceImpl.completeEncounter()` | Issues all DRAFT prescriptions (changes status to ISSUED) |
