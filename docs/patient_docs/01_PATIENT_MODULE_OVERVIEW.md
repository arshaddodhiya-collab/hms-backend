# Patient Module Overview

This document provides an overview of the **Patient Module** in the Hospital Management System (HMS) backend, which handles **Patient Registration**, **Patient Management**, and **Medical History Tracking**.

## 1. Core Components

The Patient module is built on Spring Boot with Spring Security for role-based access control (RBAC).

### A. Entities (`com.hms.HospitalManagementSystem.entity`)

The patient module manages two primary entities:

1. **Patient (`patients` table)**:
   - Represents a hospital patient with demographic and contact information
   - Contains `firstName`, `lastName`, `dob`, `gender`, `bloodGroup`
   - Contact fields: `contact` (unique), `email` (unique), `address`
   - Medical fields: `allergies`, `avatar`
   - Has a `One-to-Many` relationship with **MedicalHistory**
   - Extends `PublicEntity` (provides `id`, `active`, `deleted`, `createdAt`, `updatedAt`)
   - Uses `@Version` for optimistic locking

2. **MedicalHistory (`medical_history` table)**:
   - Represents a patient's medical history record
   - Has a `Many-to-One` relationship with **Patient**
   - Cascade delete when patient is removed

### B. Enums

**Gender**:
- `MALE`
- `FEMALE`
- `OTHER`

**BloodGroup**:
- `A_POSITIVE`, `A_NEGATIVE`
- `B_POSITIVE`, `B_NEGATIVE`
- `O_POSITIVE`, `O_NEGATIVE`
- `AB_POSITIVE`, `AB_NEGATIVE`

### C. DTOs (`com.hms.HospitalManagementSystem.dto`)

Data Transfer Objects for API communication:

1. **PatientRegisterRequest** (Request):
   - Fields: `firstName`, `lastName`, `dob`, `gender`, `bloodGroup`
   - Contact: `contact`, `email`, `address`
   - Medical: `allergies`, `avatar`
   - Used for patient registration

2. **PatientUpdateRequest** (Request):
   - Same fields as `PatientRegisterRequest`
   - Additional: `id`, `version` (for optimistic locking)
   - Used for patient updates

3. **PatientResponse** (Response):
   - Fields: `id`, `firstName`, `lastName`, `dob`, `age` (calculated)
   - Contact: `gender`, `bloodGroup`, `contact`, `email`, `address`
   - Display: `avatar`
   - Used for list views and basic patient info

4. **PatientDetailsResponse** (Response):
   - Extends `PatientResponse`
   - Additional: `allergies`, `medicalHistory` (List<MedicalHistoryDto>)
   - Used for detailed patient view

### D. Mapper (`com.hms.HospitalManagementSystem.mapper`)

**PatientMapper** (MapStruct):
- `toEntity(PatientRegisterRequest)`: Convert request to entity
- `toResponse(Patient)`: Convert entity to response
- `toDetailsResponse(Patient)`: Convert entity to detailed response
- `updateEntityFromRequest(PatientUpdateRequest, Patient)`: Update entity from request

### E. Services (`com.hms.HospitalManagementSystem.service`)

**PatientService**:
- Patient registration with duplicate detection
- Search and pagination using Specifications
- Patient details retrieval
- Patient updates with optimistic locking
- Soft delete implementation
- Validation and business logic

### F. Controllers (`com.hms.HospitalManagementSystem.controller`)

**PatientController** (`/api/v1/patients`):
- Secured with `@PreAuthorize` annotations
- Required permissions: `CMP_PATIENT_READ`, `CMP_PATIENT_WRITE`, `CMP_PATIENT_DELETE`
- RESTful endpoints for CRUD operations

### G. Repository (`com.hms.HospitalManagementSystem.repository`)

**PatientRepository**:
- Extends `JpaRepository<Patient, Long>`
- Implements `JpaSpecificationExecutor<Patient>` for dynamic queries
- Custom methods:
  - `findByContact(String)`: Find by contact number
  - `findByEmail(String)`: Find by email
  - `findPotentialDuplicate(...)`: Detect duplicates by name+DOB+contact
  - `existsByContact(String)`: Check contact uniqueness
  - `existsByEmail(String)`: Check email uniqueness

### H. Specification (`com.hms.HospitalManagementSystem.specification`)

**PatientSpecification**:
- `search(String query)`: Search by firstName, lastName, contact, or email
- `hasContact(String contact)`: Filter by contact number
- Uses JPA Criteria API for dynamic queries

## 2. Directory Structure

```
com.hms.HospitalManagementSystem
├── controller
│   └── PatientController.java          # Patient REST API
├── dto
│   ├── request
│   │   ├── PatientRegisterRequest.java # Registration DTO
│   │   └── PatientUpdateRequest.java   # Update DTO
│   ├── response
│   │   ├── PatientResponse.java        # Basic response DTO
│   │   └── PatientDetailsResponse.java # Detailed response DTO
│   └── MedicalHistoryDto.java          # Medical history DTO
├── entity
│   ├── Patient.java                    # Patient Entity
│   └── MedicalHistory.java             # Medical History Entity
├── repository
│   └── PatientRepository.java          # Patient Data Access
├── service
│   └── PatientService.java             # Patient Business Logic
├── mapper
│   └── PatientMapper.java              # MapStruct Mapper
└── specification
    └── PatientSpecification.java       # JPA Specifications
```

## 3. Security Model

### Permissions

The Patient module uses fine-grained permissions:

- **Patient Permissions**:
  - `CMP_PATIENT_READ`: View patients
  - `CMP_PATIENT_WRITE`: Create/Update patients
  - `CMP_PATIENT_DELETE`: Delete patients

### Role Assignment

By default, these permissions are assigned to:
- `ADMIN`: All permissions
- `DOCTOR`: Read and Write
- `NURSE`: Read and Write
- `RECEPTIONIST`: Read and Write

## 4. Key Features

### Patient Registration
- Comprehensive patient information capture
- Multi-level duplicate detection:
  1. Contact number uniqueness check
  2. Email uniqueness check
  3. Name + DOB + Contact combination check
- Automatic age calculation from DOB
- Blood group and gender validation
- Avatar/photo support

### Patient Search
- Search by first name, last name, contact, or email
- Case-insensitive partial matching
- Pagination and sorting support
- Default sort by creation date (newest first)

### Patient Details
- Full patient information
- Medical history records
- Allergies information
- Calculated age field

### Patient Updates
- Optimistic locking with version field
- Prevents concurrent update conflicts
- Partial updates supported
- Contact and email uniqueness validation

### Soft Delete
- Preserves patient data
- Sets `deleted = true` and `active = false`
- Maintains data integrity for historical records

## 5. Database Schema

### patients Table
```sql
CREATE TABLE patients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    dob DATE NOT NULL,
    gender VARCHAR(10) NOT NULL,
    blood_group VARCHAR(15),
    contact VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(100) UNIQUE,
    address TEXT,
    allergies TEXT,
    avatar VARCHAR(255),
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    deleted BOOLEAN DEFAULT FALSE,
    INDEX idx_patient_name (first_name, last_name),
    INDEX idx_patient_contact (contact)
);
```

### medical_history Table
```sql
CREATE TABLE medical_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    -- Additional medical history fields
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);
```

## 6. Business Logic

### Duplicate Detection

The system implements three levels of duplicate detection:

1. **Contact Uniqueness**: No two patients can have the same contact number
2. **Email Uniqueness**: No two patients can have the same email (if provided)
3. **Composite Uniqueness**: Combination of firstName + lastName + DOB + contact

### Optimistic Locking

- Uses `@Version` annotation on Patient entity
- Prevents lost updates in concurrent scenarios
- Client must provide current version number for updates
- Throws exception if version mismatch detected

### Age Calculation

- Age is calculated dynamically from DOB
- Not stored in database (derived field)
- Calculated in mapper during entity-to-DTO conversion

## 7. Related Documentation

- [Patient Service & Endpoints](./02_PATIENT_SERVICE_AND_ENDPOINTS.md)
- [Search & Filtering](./03_SEARCH_AND_FILTERING.md)
- [Database Schema](./04_DATABASE_SCHEMA.md)
- [API Testing Guide](./05_API_TESTING_GUIDE.md)
- [Security & Permissions](./06_SECURITY_AND_PERMISSIONS.md)
