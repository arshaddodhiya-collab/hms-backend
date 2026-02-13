# Lab Module Documentation

## Overview
The Lab Module is a critical component of the Hospital Management System, designed to manage the entire lifecycle of diagnostic testing. It handles test catalogs, patient lab requests, sample collection, result entry, and report generation, integrating seamlessly with the **Encounter** and **Consultation** modules.

## Purpose
The Lab module serves as:
- **Diagnostic Hub**: Central repository for all lab tests and results.
- **Workflow Manager**: Tracks status from `ORDERED` to `COMPLETED`.
- **Quality Control**: Enforces validation on result parameters and reference ranges.
- **Integration Point**: Provides diagnostic data to doctors during consultations.

## Key Concepts

### Lab Lifecycle
```
ORDERED → SAMPLED → COMPLETED
   ↓         ↓          ↓
(Doctor)  (Lab Tech) (Lab Tech)
```

### Request-Result Model
- **Lab Request**: The order for a specific test (e.g., CBC) for a patient.
- **Lab Test Catalog**: Definitions of available tests and their prices.
- **Lab Test Parameters**: Defines the detailed parameters (e.g., Hb, WBC) expected for a test.
- **Lab Result**: The actual value recorded for a specific parameter.

## Documentation Structure
1. **[Architecture](./ARCHITECTURE.md)** - System design and component relationships
2. **[API Reference](./API_REFERENCE.md)** - Complete API endpoint documentation
3. **[Database Schema](./DATABASE_SCHEMA.md)** - Table structures and relationships
4. **[Service Layer](./SERVICE_LAYER.md)** - Business logic implementation
5. **[Security & RBAC](./SECURITY.md)** - Role-based access control
6. **[Integration Guide](./INTEGRATION_GUIDE.md)** - Frontend integration patterns
7. **[Testing Guide](./TESTING.md)** - Unit and integration testing

## Quick Start

### Creating a Lab Request
```java
// Doctor orders a test
POST /api/v1/lab-requests
{
  "encounterId": 101,
  "patientId": 500,
  "labTestId": 5,
  "notes": "Routine checkup"
}
```

### Adding Results
```java
// Lab Tech enters results
POST /api/v1/lab-requests/{id}/results
[
  {
    "parameterName": "Hemoglobin",
    "resultValue": "14.5",
    "unit": "g/dL",
    "referenceRange": "13.0-17.0"
  }
]
```

## Key Features

### ✅ Implemented
- **Test Catalog Management**: Define tests, prices, and reference ranges.
- **Request Workflow**: Status tracking (ORDERED, SAMPLED, COMPLETED, CANCELLED).
- **Result Entry**: Dynamic result forms based on test parameters.
- **Strict Validation**: Regex-based validation for medical data formats.
- **Role-Based Access**: Distinct roles for Doctors (Order) and Lab Techs (Process).
- **Auto-fill Parameters**: Automatically populates expected parameters for standard tests.

### 🚧 Future Enhancements
- **Bulk Sample Collection**: Process multiple samples at once.
- **Report PDF Generation**: Printable lab reports.
- **External Device Integration**: HL7 integration for auto-analyzers.
- **Billing Integration**: Auto-charge patients upon request creation.

## Related Modules
- **Encounter Module**: Source of lab requests.
- **Patient Module**: Patient identity and history.
- **User Module**: Auth and permissions.
