# Lab Module Testing Strategy

## Overview
The Lab Module requires rigorous testing due to the medical nature of the data. Incorrect results or swapped parameters can have serious clinical implications.

## Testing Layers

### 1. Unit Testing (Backend)
Focus on `LabService` business logic.
- **Tools**: JUnit 5, Mockito.
- **Scenarios**:
  - `createLabRequest`: Verify status is set to ORDERED.
  - `addLabResults`: Verify status changes to COMPLETED.
  - `addLabResults`: Verify `RuntimeException` is thrown if status is CANCELLED.
  - `createLabTest`: Verify catalog creation.

### 2. Integration Testing (Backend)
Focus on Controller-Service-Database flow.
- **Tools**: Spring Boot Test, MockMvc, H2 Database.
- **Scenarios**:
  - `POST /lab-requests`: Verify DTO validation (400 Bad Request for invalid IDs).
  - `POST /results`: Verify Regex pattern validation rejects "garbage" input.
  - **Security**: Verify a User without `CMP_LAB_ENTRY` cannot post results.

### 3. Frontend Testing
Focus on Component logic and Form validation.
- **Tools**: Jasmine, Karma.
- **Scenarios**:
  - **LabTestEntry**:
    - Verify form is invalid if "Value" is empty.
    - Verify "Save" button is disabled if validation fails.
    - Verify duplicate parameter check shows error message.
    - Verify auto-fill logic correctly populates rows from `parameters`.

## Manual Verification Checklist for QA

### T1: Normal Flow
1. Login as Doctor. Order "CBC" for Patient X.
2. Login as Lab Tech. See order in queue.
3. Open Order. Verify "Hemoglobin", "WBC" rows are pre-filled.
4. Enter valid values (`14.5`, `5000`). Mark one as Abnormal.
5. Save. Verify success message.
6. Check Queue. Status should be COMPLETED.
7. Click View. Verify values match input.

### T2: Validation Flow
1. Open active request.
2. Enter `invalid@text` in Value field. Verify red error border.
3. Try to Save. Verify button disabled.
4. Add a new row. Enter "Hemoglobin" (duplicate). Verify error "Duplicate parameters".

### T3: Security Flow
1. Login as Doctor.
2. Try to access `/lab/entry/{id}` directly via URL.
3. Should be redirected or denied (based on Route Guards).

## Future Testing Improvements
- **End-to-End (E2E)**: Implement Cypress tests for the full Doctor -> Lab Tech -> Report flow.
- **Performance**: Load test the Queue endpoint with 10k+ records.
