# Encounter Module API Reference

## Base URL
```
/api/v1/encounters
```

## Authentication
All endpoints require JWT authentication via `Authorization: Bearer <token>` header.

---

## Encounter Endpoints

### 1. Start Encounter
Creates a new encounter from an appointment. **Idempotent** - returns existing encounter if already created.

**Endpoint**: `POST /api/v1/encounters`

**Permissions**: `CMP_VITALS_WRITE` OR `CMP_CONSULTATION_WRITE`

**Request Body**:
```json
{
  "appointmentId": 123,
  "patientId": 456,
  "doctorId": 789
}
```

**Response**: `200 OK`
```json
{
  "id": 50,
  "appointmentId": 123,
  "patientId": 456,
  "doctorId": 789,
  "status": "TRIAGE",
  "chiefComplaint": null,
  "diagnosis": null,
  "notes": null,
  "startedAt": "2026-02-12T10:30:00",
  "completedAt": null
}
```

**Business Rules**:
- Appointment must exist and not be CANCELLED or NO_SHOW
- Patient and Doctor IDs must match appointment
- Creates encounter with status TRIAGE
- Updates appointment status to CHECKED_IN if SCHEDULED

**Error Responses**:
- `404 Not Found`: Appointment not found
- `409 Conflict`: Invalid appointment status or ID mismatch

---

### 2. Update Clinical Notes
Updates chief complaint, diagnosis, and notes. Transitions from TRIAGE to IN_PROGRESS.

**Endpoint**: `PATCH /api/v1/encounters/{id}/clinical-notes`

**Permissions**: `CMP_CONSULTATION_WRITE`

**Request Body**:
```json
{
  "chiefComplaint": "Fever and headache for 3 days",
  "diagnosis": "Viral fever",
  "notes": "Prescribed rest and fluids. Follow up in 3 days if symptoms persist."
}
```

**Response**: `200 OK`
```json
{
  "id": 50,
  "appointmentId": 123,
  "patientId": 456,
  "doctorId": 789,
  "status": "IN_PROGRESS",
  "chiefComplaint": "Fever and headache for 3 days",
  "diagnosis": "Viral fever",
  "notes": "Prescribed rest and fluids. Follow up in 3 days if symptoms persist.",
  "startedAt": "2026-02-12T10:30:00",
  "completedAt": null
}
```

**Business Rules**:
- Only the assigned doctor can update
- Cannot update completed encounters
- Status transitions: TRIAGE → IN_PROGRESS
- Updates appointment status to IN_PROGRESS

**Error Responses**:
- `404 Not Found`: Encounter not found
- `403 Forbidden`: Not the assigned doctor
- `409 Conflict`: Encounter already completed

---

### 3. Complete Encounter
Finalizes the encounter, issues prescriptions, and updates appointment.

**Endpoint**: `PATCH /api/v1/encounters/{id}/complete`

**Permissions**: `CMP_CONSULTATION_WRITE`

**Request Body**: None

**Response**: `200 OK`
```json
{
  "id": 50,
  "appointmentId": 123,
  "patientId": 456,
  "doctorId": 789,
  "status": "COMPLETED",
  "chiefComplaint": "Fever and headache for 3 days",
  "diagnosis": "Viral fever",
  "notes": "Prescribed rest and fluids. Follow up in 3 days if symptoms persist.",
  "startedAt": "2026-02-12T10:30:00",
  "completedAt": "2026-02-12T11:15:00"
}
```

**Business Rules**:
- Only the assigned doctor can complete
- Diagnosis must be present
- Status must be IN_PROGRESS
- Sets all prescriptions to ISSUED
- Updates appointment to COMPLETED
- Sets completedAt timestamp

**Error Responses**:
- `404 Not Found`: Encounter not found
- `403 Forbidden`: Not the assigned doctor
- `409 Conflict`: Missing diagnosis or already completed

---

### 4. Get Encounter by ID
Retrieves a specific encounter.

**Endpoint**: `GET /api/v1/encounters/{id}`

**Permissions**: `CMP_CONSULTATION_READ`

**Response**: `200 OK`
```json
{
  "id": 50,
  "appointmentId": 123,
  "patientId": 456,
  "doctorId": 789,
  "status": "IN_PROGRESS",
  "chiefComplaint": "Fever and headache",
  "diagnosis": "Viral fever",
  "notes": "Rest and fluids",
  "startedAt": "2026-02-12T10:30:00",
  "completedAt": null
}
```

**Error Responses**:
- `404 Not Found`: Encounter not found

---

### 5. Get Encounter by Appointment ID
Retrieves encounter for a specific appointment.

**Endpoint**: `GET /api/v1/encounters/by-appointment/{appointmentId}`

**Permissions**: `CMP_CONSULTATION_READ`

**Response**: `200 OK` (same as Get by ID)

**Error Responses**:
- `404 Not Found`: No encounter for this appointment

---

### 6. Get Triage Queue
Retrieves all encounters in TRIAGE status.

**Endpoint**: `GET /api/v1/encounters/queue/triage`

**Permissions**: `CMP_VITALS_READ`

**Response**: `200 OK`
```json
[
  {
    "id": 50,
    "appointmentId": 123,
    "patientId": 456,
    "doctorId": 789,
    "status": "TRIAGE",
    "chiefComplaint": null,
    "diagnosis": null,
    "notes": null,
    "startedAt": "2026-02-12T10:30:00",
    "completedAt": null
  },
  {
    "id": 51,
    "appointmentId": 124,
    "patientId": 457,
    "doctorId": 789,
    "status": "TRIAGE",
    "chiefComplaint": null,
    "diagnosis": null,
    "notes": null,
    "startedAt": "2026-02-12T10:45:00",
    "completedAt": null
  }
]
```

---

### 7. Get Doctor Queue
Retrieves all IN_PROGRESS encounters for a specific doctor.

**Endpoint**: `GET /api/v1/encounters/queue/doctor/{doctorId}`

**Permissions**: `CMP_CONSULTATION_READ`

**Response**: `200 OK`
```json
[
  {
    "id": 52,
    "appointmentId": 125,
    "patientId": 458,
    "doctorId": 789,
    "status": "IN_PROGRESS",
    "chiefComplaint": "Cough",
    "diagnosis": null,
    "notes": null,
    "startedAt": "2026-02-12T11:00:00",
    "completedAt": null
  }
]
```

---

## Vitals Endpoints

### 1. Save Vitals
Creates or updates vitals for an encounter. Automatically calculates BMI.

**Endpoint**: `POST /api/v1/encounters/{encounterId}/vitals`

**Permissions**: `CMP_VITALS_WRITE` OR `CMP_CONSULTATION_WRITE`

**Request Body**:
```json
{
  "temperature": 98.6,
  "systolic": 120,
  "diastolic": 80,
  "pulse": 72,
  "spo2": 98,
  "weight": 70.5,
  "height": 175.0
}
```

**Response**: `200 OK`
```json
{
  "id": 10,
  "encounterId": 50,
  "temperature": 98.6,
  "systolic": 120,
  "diastolic": 80,
  "pulse": 72,
  "spo2": 98,
  "weight": 70.5,
  "height": 175.0,
  "bmi": 23.02,
  "recordedAt": "2026-02-12T10:35:00",
  "recordedBy": "Nurse Jane"
}
```

**Business Rules**:
- BMI calculated as: weight(kg) / (height(m))²
- Records current user as recordedBy
- Updates existing vitals if already present

**Error Responses**:
- `404 Not Found`: Encounter not found

---

### 2. Get Vitals
Retrieves vitals for an encounter.

**Endpoint**: `GET /api/v1/encounters/{encounterId}/vitals`

**Permissions**: `CMP_VITALS_READ` OR `CMP_CONSULTATION_READ`

**Response**: `200 OK` (same as Save Vitals)

**Error Responses**:
- `404 Not Found`: Vitals not found for this encounter

---

## Prescription Endpoints

### 1. Save Prescription
Creates or updates a prescription with items.

**Endpoint**: `POST /api/v1/encounters/{encounterId}/prescriptions`

**Permissions**: `CMP_PRESCRIPTION_WRITE`

**Request Body**:
```json
{
  "note": "Take with food. Complete full course.",
  "items": [
    {
      "medicineName": "Paracetamol 500mg",
      "dosage": "1 tablet",
      "frequency": "Three times daily",
      "duration": "5 days"
    },
    {
      "medicineName": "Vitamin C",
      "dosage": "1 tablet",
      "frequency": "Once daily",
      "duration": "7 days"
    }
  ]
}
```

**Response**: `200 OK`
```json
{
  "id": 20,
  "encounterId": 50,
  "note": "Take with food. Complete full course.",
  "status": "DRAFT",
  "issuedAt": null,
  "items": [
    {
      "id": 30,
      "medicineName": "Paracetamol 500mg",
      "dosage": "1 tablet",
      "frequency": "Three times daily",
      "duration": "5 days"
    },
    {
      "id": 31,
      "medicineName": "Vitamin C",
      "dosage": "1 tablet",
      "frequency": "Once daily",
      "duration": "7 days"
    }
  ]
}
```

**Business Rules**:
- Creates prescription with DRAFT status
- Replaces all existing items
- Status changes to ISSUED when encounter is completed

**Error Responses**:
- `404 Not Found`: Encounter not found

---

### 2. Get Prescription
Retrieves prescription for an encounter.

**Endpoint**: `GET /api/v1/encounters/{encounterId}/prescriptions`

**Permissions**: `CMP_PRESCRIPTION_READ` OR `CMP_CONSULTATION_READ`

**Response**: `200 OK` (same as Save Prescription)

**Error Responses**:
- `404 Not Found`: Prescription not found for this encounter

---

## Status Codes

| Code | Meaning | Usage |
|------|---------|-------|
| 200 | OK | Successful request |
| 404 | Not Found | Resource doesn't exist |
| 403 | Forbidden | Insufficient permissions or ownership violation |
| 409 | Conflict | Business rule violation |
| 401 | Unauthorized | Missing or invalid JWT token |
| 500 | Internal Server Error | Unexpected server error |

---

## Common Error Response Format

```json
{
  "timestamp": "2026-02-12T12:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Encounter not found",
  "path": "/api/v1/encounters/999"
}
```

---

## Rate Limiting
Currently not implemented. Future consideration for production deployment.

---

## Versioning
API version is included in the URL path (`/api/v1/`). Breaking changes will increment the version number.

---

## Testing with cURL

### Start Encounter
```bash
curl -X POST http://localhost:8080/api/v1/encounters \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "appointmentId": 123,
    "patientId": 456,
    "doctorId": 789
  }'
```

### Save Vitals
```bash
curl -X POST http://localhost:8080/api/v1/encounters/50/vitals \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "temperature": 98.6,
    "systolic": 120,
    "diastolic": 80,
    "pulse": 72,
    "spo2": 98,
    "weight": 70.5,
    "height": 175.0
  }'
```

### Complete Encounter
```bash
curl -X PATCH http://localhost:8080/api/v1/encounters/50/complete \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```
