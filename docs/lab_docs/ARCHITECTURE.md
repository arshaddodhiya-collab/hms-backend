# Lab Module Architecture

## System Overview

The Lab Module follows the standard layered architecture of the application, designed for scalability and maintainability.

```
┌─────────────────────────────────────────────────────────┐
│                    Controllers                          │
│  (LabController - REST API)                             │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    Services                             │
│  (LabService - Workflow & Validation)                   │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                  Repositories                           │
│  (JPA - LabRequestRepository, LabTestRepository...)     │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    Database                             │
│  (Tables: lab_requests, lab_results, lab_test_catalog)  │
└─────────────────────────────────────────────────────────┘
```

## Component Architecture

### 1. Entity Layer

#### LabTestCatalog
**Purpose**: Defines the menu of available tests.
- **Fields**: Name, Code, Price, Reference Range (general).
- **Relationships**: `@OneToMany` with `LabTestParameter`.

#### LabTestParameter
**Purpose**: Defines the specific parameters for a test (e.g., "Hemoglobin" for "CBC").
- **Usage**: Used to auto-fill result entry forms.

#### LabRequest
**Purpose**: Represents a specific order for a patient.
- **Key Relationships**:
  - `@ManyToOne` with Encounter (optional, for direct orders).
  - `@ManyToOne` with Patient.
  - `@ManyToOne` with LabTestCatalog.
  - `@OneToMany` with LabResult.
- **Status Enum**: `ORDERED`, `SAMPLED`, `COMPLETED`, `CANCELLED`.

#### LabResult
**Purpose**: Stores the actual outcome of a test parameter.
- **Fields**: Parameter Name, Value, Unit, Reference Range, Is Abnormal.
- **Constraints**: Strict link to `LabRequest`.

### 2. Service Layer

#### LabService
**Responsibilities**:
- **Workflow Management**: transitions status (ORDERED -> SAMPLED -> COMPLETED).
- **Validation**: Ensures results are only added to active requests.
- **Catalog Management**: CRUD operations for tests and parameters.
- **Integration**: Fetches requests by Encounter or Patient.

**Key Logic**:
```java
addLabResults(requestId, results)
  → Validates status (must not be COMPLETED/CANCELLED)
  → Saves results associated with request
  → Auto-updates status to COMPLETED
  → Updates `updatedAt` timestamp
```

### 3. Controller Layer

#### LabController
**Endpoints**:
- `POST /api/v1/lab-requests`: Create order.
- `GET /api/v1/lab-requests`: Queue view (filtering by status).
- `POST /api/v1/lab-requests/{id}/results`: Enter results.
- `GET /api/v1/lab-tests`: Fetch catalog.

**Security**:
- `CMP_LAB_ENTRY`: Required for result entry.
- `CMP_CONSULTATION_WRITE`: Required for ordering tests.

## Design Patterns

### 1. DTO Pattern
- `LabRequestResponse` encapsulates the request details plus the list of results or expected parameters.
- `LabResultRequest` encapsulates payload validation (Regex patterns).

### 2. Status Machine (Implicit)
- The service layer enforces valid transitions.
- **Rule**: Results cannot be modified after `COMPLETED` status (unless admin override - future).

## Transaction Management
- `@Transactional` is used on all write operations in `LabService` to ensure `LabRequest` status updates and `LabResult` insertions happen atomically.

## Scalability
- **Database**: Indexed columns for `status`, `encounter_id`, and `patient_id` ensure fast queue retrieval.
- **Optimization**: `LabRequestResponse` mapping uses efficient stream processing.
