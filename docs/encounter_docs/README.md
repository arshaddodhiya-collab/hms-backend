# Encounter Module Documentation

## Overview
The Encounter Module is the core medical documentation system for the Hospital Management System. It represents the **medical truth** of a patient's visit, capturing all clinical data, vitals, and prescriptions in a structured, auditable format.

## Purpose
The Encounter module serves as:
- **Central Medical Record**: Single source of truth for patient visit data
- **Workflow Orchestrator**: Manages the patient journey from triage through consultation to completion
- **Clinical Documentation**: Captures chief complaints, diagnoses, and treatment notes
- **Prescription Management**: Links prescriptions to specific encounters
- **Audit Trail**: Maintains complete history of medical interactions

## Key Concepts

### Encounter Lifecycle
```
SCHEDULED → CHECKED_IN → TRIAGE → IN_PROGRESS → COMPLETED
                            ↓           ↓
                        (Vitals)  (Clinical Notes + Prescription)
```

### Medical Truth Principle
- One Encounter per Appointment
- Immutable after completion
- All clinical data linked to Encounter
- Audit fields track all changes

## Documentation Structure

1. **[Architecture](./ARCHITECTURE.md)** - System design and component relationships
2. **[API Reference](./API_REFERENCE.md)** - Complete API endpoint documentation
3. **[Database Schema](./DATABASE_SCHEMA.md)** - Table structures and relationships
4. **[Service Layer](./SERVICE_LAYER.md)** - Business logic implementation
5. **[Security & RBAC](./SECURITY.md)** - Role-based access control
6. **[Integration Guide](./INTEGRATION_GUIDE.md)** - Frontend integration patterns
7. **[Testing Guide](./TESTING.md)** - Unit and integration testing

## Quick Start

### Creating an Encounter
```java
// Start encounter from appointment
POST /api/v1/encounters
{
  "appointmentId": 123,
  "patientId": 456,
  "doctorId": 789
}
```

### Recording Vitals
```java
POST /api/v1/encounters/{encounterId}/vitals
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

### Completing Encounter
```java
PATCH /api/v1/encounters/{id}/complete
// Requires diagnosis to be set
// Automatically issues prescriptions
```

## Key Features

### ✅ Implemented
- Encounter creation and lifecycle management
- Vitals recording with automatic BMI calculation
- Prescription management (draft → issued)
- Queue management (triage queue, doctor queue)
- Role-based access control
- Audit trail (created_at, updated_at, recorded_by)
- Database migration support (Flyway)

### 🚧 Future Enhancements
- Lab request integration
- Document attachments
- Encounter templates
- Clinical decision support
- Encounter search and filtering

## Related Modules
- **Appointment Module**: Triggers encounter creation
- **Patient Module**: Links patient medical history
- **User Module**: Doctor and staff assignments
- **Prescription Module**: Medication management

## Support
For questions or issues, refer to the detailed documentation in this folder or contact the development team.
