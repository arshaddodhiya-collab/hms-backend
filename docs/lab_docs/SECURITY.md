# Lab Module Security & Access Control

## Overview
Access to the Lab Module is governed by Role-Based Access Control (RBAC) using Spring Security. Permissions are granular, separating administrative, clinical, and technical duties.

## Permissions

### 1. Lab Technician (`ROLE_LAB_TECH`)
Primary user of the module.
- **Authorities**: `CMP_LAB_READ`, `CMP_LAB_ENTRY`
- **Capabilities**:
  - View the Lab Queue.
  - Update request status (e.g., to SAMPLED).
  - Enter and Save Lab Results.
  - **Cannot**: Create new Lab Requests (Diagnostic orders are clinical).

### 2. Doctor (`ROLE_DOCTOR`)
Consumer of the module.
- **Authorities**: `CMP_CONSULTATION_READ`, `CMP_CONSULTATION_WRITE`, `CMP_LAB_READ`
- **Capabilities**:
  - Create Lab Requests (Order tests).
  - View Lab Results.
  - **Cannot**: Enter results (Conflict of interest/Scope of practice).

### 3. Administrator (`ROLE_ADMIN`)
Configuration manager.
- **Authorities**: `MOD_ADMIN`, `MOD_LAB`
- **Capabilities**:
  - Create/Update Lab Tests in the Catalog.
  - Manage Prices and Reference Ranges.
  - Define Test Parameters.
  - Full read access.

## Implementation Details

### Method-Level Security
We use `@PreAuthorize` on Controller endpoints:

```java
// Ordering a test (Doctor)
@PreAuthorize("hasAnyAuthority('CMP_LAB_ENTRY', 'CMP_CONSULTATION_WRITE')")
createLabRequest(...)

// Entering results (Tech)
@PreAuthorize("hasAnyAuthority('CMP_LAB_ENTRY')")
addLabResults(...)

// Viewing Catalog (All)
@PreAuthorize("hasAnyAuthority('CMP_LAB_READ', 'CMP_CONSULTATION_READ', 'CMP_LAB_ENTRY')")
getAllLabTests(...)
```

### Data Security
- **Input Sanitization**: strict Regex patterns prevent Injection attacks via text fields.
- **IDOR Protection**: Service layer validates that the `encounterId` and `patientId` exist and are valid.

## Audit Trail
Key actions are implicitly auditable via the `created_at` and `updated_at` timestamps.
- **Future**: Add `created_by` and `updated_by` columns to `lab_requests` and `lab_results` to track exactly which user entered the results.
