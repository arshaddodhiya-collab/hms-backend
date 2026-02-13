# Lab Module Integration Guide

## Frontend Integration

The Lab Module interacts with two main areas of the frontend application: the **Lab Management** area (`/lab`) and the **Consultation** area (`/consultation`).

### 1. Lab Management (`features/lab`)

#### `lab-request-list.component`
- **Purpose**: dashboard for Lab Technicians.
- **API**: `GET /api/v1/lab-requests`
- **Behavior**:
  - Filters requests by status (ORDERED, SAMPLED).
  - Provides navigation to the **Result Entry** page.
  - Shows "View Results" button for COMPLETED requests.

#### `lab-test-entry.component`
- **Purpose**: Form for entering result values.
- **API**:
  - `GET /api/v1/lab-requests/{id}`: Fetches active request.
  - `POST /api/v1/lab-requests/{id}/results`: Submits data.
- **Auto-fill Logic**:
  - Checks if `request.results` exists (Edit mode).
  - If not, checks `request.parameters` to pre-fill rows (Creation mode).
- **Validation**:
  - UI enforces `Validators.required` and regex patterns.
  - Visual feedback for invalid inputs.

#### `lab-report-view.component`
- **Purpose**: Read-only display of completed results.
- **API**: `GET /api/v1/lab-requests/{id}`
- **Display**: Highlights abnormal results with red badges/text.

### 2. Consultation (`features/consultation`)

#### `consultation-detail.component`
- **Purpose**: Doctor's view of a patient visit.
- **Integration**:
  - Displays a "Lab Requests" tab.
  - **Orders**: Uses `LabService.createRequest` to order new tests.
  - **Views**: Lists requests linked to the current `encounterId`.
  - **Results**: Clicking a completed request opens the report view.

## Cross-Module Dependencies

### With Encounter Module
- **Linkage**: Every Lab Request is optionally linked to an `Encounter`.
- **Workflow**: Completing a Lab Request does **not** automatically complete the Encounter. The Doctor must review results first.

### With Patient Module
- **Data**: Lab Request requires a valid `patientId`.
- **Display**: Patient Name and ID are fetched and displayed on all Lab headers.

## Edge Cases & Safeguards

- **Duplicate Entry**: The frontend prevents adding two parameters with the same name.
- **Concurrent Editing**: Currently, last write wins. Future: Optimistic locking with `@Version`.
- **Status Conflicts**: If a request is cancelled by a Doctor while a Tech is entering results, the backend rejects the save with a status error.

## Checklist for New Features
When adding a new feature to Lab:
1.  Update `LabRequestResponse` DTO if data shape changes.
2.  Ensure `LabService` handles the logic.
3.  Update `lab.models.ts` in frontend to match DTO.
4.  Add appropriate `@PreAuthorize` tags.
