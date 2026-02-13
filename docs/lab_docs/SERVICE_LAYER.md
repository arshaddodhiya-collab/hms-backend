# Lab Service Layer

## Overview
The `LabService` encapsulates the business logic for the diagnostic module, ensuring data consistency and enforcing workflow rules.

## Core Responsibilities

1.  **Request Lifecycle Management**
    - Handles creation of requests from Consultations.
    - Manages state transitions (ORDERED -> SAMPLED -> COMPLETED).
2.  **Result Processing**
    - Validates incoming results against business rules.
    - Associates results with the correct request.
    - Updates request timestamps and status.
3.  **Catalog Management**
    - Provides data for valid tests and parameters.

## Key Methods

### `createLabRequest`
- **Input**: `LabRequestCreateRequest`
- **Logic**:
  - Verifies `Patient` and `Encounter` existence.
  - Verifies `LabTest` existence.
  - Sets initial status to `ORDERED`.
  - Saves to repository.

### `addLabResults`
- **Input**: `requestId`, `List<LabResultRequest>`
- **Logic**:
  1.  **Fetch**: Retrieve `LabRequest` by ID.
  2.  **Validate Status**: If status is `COMPLETED` or `CANCELLED`, throw `RuntimeException`.
  3.  **Process Results**:
      - Iterate through result items.
      - Map DTO to `LabResult` entity.
      - Associate with the fetched `LabRequest`.
  4.  **Update State**: Set `LabRequest` status to `COMPLETED`.
  5.  **Save**: Persist changes effectively cascading the results.

### `getLabQueue`
- **Input**: `List<LabRequestStatus>`
- **Logic**:
  - Uses specific repository query `findByStatusIn` to efficiently fetch active tasks.
  - Sorts by `createdAt` descending (newest first).

## Error Handling

| Exception | Scenario | HTTP Status |
|-----------|----------|-------------|
| `ResourceNotFoundException` | Request, Patient, or Test ID not found | 404 Not Found |
| `MethodArgumentNotValidException` | Input Regex validation fails | 400 Bad Request |
| `RuntimeException` (Custom) | Adding results to completed request | 500 (Should be mapped to 409 Conflict) |
| `AccessDeniedException` | User lacks required permission | 403 Forbidden |

## Future Improvements
- **Custom Exceptions**: Replace generic `RuntimeException` with `ConflictException` for status errors.
- **Event Publishing**: Publish `LabResultCompletedEvent` for other modules (e.g., Notification) to consume.
