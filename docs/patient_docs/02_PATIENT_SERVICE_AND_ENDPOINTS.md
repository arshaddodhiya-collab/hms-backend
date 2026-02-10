# Patient Service & Endpoints

This document details the **PatientService** business logic and **PatientController** REST API endpoints.

## 1. PatientService

### Overview

`PatientService` handles all patient-related business logic including:
- Patient registration with duplicate detection
- Patient search and pagination
- Patient details retrieval
- Patient updates with optimistic locking
- Soft delete implementation

### Dependencies

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
}
```

## 2. Service Methods

### 2.1 Register Patient

**Method**: `registerPatient(PatientRegisterRequest request)`

**Purpose**: Register a new patient with comprehensive duplicate detection

**Business Logic**:
1. Check if contact number already exists
2. Check if email already exists (if provided)
3. Check for potential duplicate using name + DOB + contact combination
4. Map request DTO to Patient entity
5. Save patient to database
6. Return PatientResponse DTO

**Duplicate Detection**:
```java
// Level 1: Contact uniqueness
if (patientRepository.existsByContact(request.getContact())) {
    throw new IllegalArgumentException("Patient with this contact number already exists.");
}

// Level 2: Email uniqueness
if (request.getEmail() != null && !request.getEmail().isEmpty()
        && patientRepository.existsByEmail(request.getEmail())) {
    throw new IllegalArgumentException("Patient with this email already exists.");
}

// Level 3: Composite uniqueness (Name + DOB + Contact)
patientRepository.findPotentialDuplicate(
        request.getFirstName(),
        request.getLastName(),
        request.getDob(),
        request.getContact()).ifPresent(p -> {
            throw new IllegalArgumentException("Duplicate patient record found.");
        });
```

**Validation Rules**:
- First name is required
- Last name is required
- Date of birth is required
- Gender is required
- Contact is required and must be unique
- Email must be unique (if provided)

**Returns**: `PatientResponse`

**Throws**:
- `IllegalArgumentException`: If duplicate detected or validation fails

---

### 2.2 Search Patients

**Method**: `searchPatients(String query, Pageable pageable)`

**Purpose**: Search patients with pagination and sorting

**Business Logic**:
1. Build JPA Specification from query string
2. Execute paginated query
3. Map results to PatientResponse DTOs

**Search Fields**:
- First name (case-insensitive, partial match)
- Last name (case-insensitive, partial match)
- Contact (case-insensitive, partial match)
- Email (case-insensitive, partial match)

**Query Behavior**:
- If `query` is null or empty: Returns all patients
- If `query` is provided: Returns patients matching any search field

**Pagination**:
- Default sort: `createdAt DESC` (newest first)
- Customizable page size and number
- Returns `Page<PatientResponse>` with metadata

**Returns**: `Page<PatientResponse>`

---

### 2.3 Get Patient Details

**Method**: `getPatientDetails(Long id)`

**Purpose**: Retrieve detailed patient information including medical history

**Business Logic**:
1. Find patient by ID
2. Throw exception if not found
3. Map to PatientDetailsResponse (includes medical history)

**Returns**: `PatientDetailsResponse`

**Throws**:
- `RuntimeException`: If patient not found with ID

---

### 2.4 Update Patient

**Method**: `updatePatient(Long id, PatientUpdateRequest request)`

**Purpose**: Update patient information with optimistic locking

**Business Logic**:
1. Find patient by ID
2. Verify version number (optimistic locking)
3. Update entity from request DTO
4. Save updated patient
5. Return updated PatientResponse

**Optimistic Locking**:
```java
if (request.getVersion() != null && patient.getVersion() != null
        && !request.getVersion().equals(patient.getVersion())) {
    throw new RuntimeException(
            "Patient record has been modified by another transaction. Please refresh and try again.");
}
```

**Returns**: `PatientResponse`

**Throws**:
- `RuntimeException`: If patient not found or version mismatch

---

### 2.5 Delete Patient

**Method**: `deletePatient(Long id)`

**Purpose**: Soft delete patient (preserves data)

**Business Logic**:
1. Find patient by ID
2. Set `deleted = true`
3. Set `active = false`
4. Save patient

**Note**: This is a soft delete. Patient data is preserved for historical records.

**Returns**: `void`

**Throws**:
- `RuntimeException`: If patient not found with ID

---

## 3. PatientController

### Base Path
```
/api/v1/patients
```

### Security
All endpoints require JWT authentication and appropriate permissions.

---

### 3.1 Register Patient

**Endpoint**: `POST /api/v1/patients`

**Permission**: `CMP_PATIENT_WRITE`

**Request Body**:
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "dob": "1990-05-15",
  "gender": "MALE",
  "bloodGroup": "O_POSITIVE",
  "contact": "9876543210",
  "email": "john.doe@example.com",
  "address": "123 Main St, City, State, ZIP",
  "allergies": "Penicillin, Peanuts",
  "avatar": "https://example.com/avatars/john.jpg"
}
```

**Response** (201 Created):
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "dob": "1990-05-15",
  "age": 33,
  "gender": "MALE",
  "bloodGroup": "O_POSITIVE",
  "contact": "9876543210",
  "email": "john.doe@example.com",
  "address": "123 Main St, City, State, ZIP",
  "avatar": "https://example.com/avatars/john.jpg"
}
```

**Error Responses**:
- `400 Bad Request`: Validation error or duplicate detected
- `401 Unauthorized`: Missing or invalid JWT token
- `403 Forbidden`: Insufficient permissions

---

### 3.2 Search Patients

**Endpoint**: `GET /api/v1/patients`

**Permission**: `CMP_PATIENT_READ`

**Query Parameters**:
- `query` (optional): Search term for name, contact, or email
- `page` (optional, default: 0): Page number
- `size` (optional, default: 20): Page size
- `sort` (optional, default: createdAt,desc): Sort field and direction

**Example Request**:
```
GET /api/v1/patients?query=john&page=0&size=10&sort=lastName,asc
```

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 1,
      "firstName": "John",
      "lastName": "Doe",
      "dob": "1990-05-15",
      "age": 33,
      "gender": "MALE",
      "bloodGroup": "O_POSITIVE",
      "contact": "9876543210",
      "email": "john.doe@example.com",
      "address": "123 Main St, City, State, ZIP",
      "avatar": "https://example.com/avatars/john.jpg"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    }
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "numberOfElements": 1,
  "size": 10,
  "number": 0,
  "empty": false
}
```

---

### 3.3 Get Patient Details

**Endpoint**: `GET /api/v1/patients/{id}`

**Permission**: `CMP_PATIENT_READ`

**Path Parameters**:
- `id`: Patient ID

**Example Request**:
```
GET /api/v1/patients/1
```

**Response** (200 OK):
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "dob": "1990-05-15",
  "age": 33,
  "gender": "MALE",
  "bloodGroup": "O_POSITIVE",
  "contact": "9876543210",
  "email": "john.doe@example.com",
  "address": "123 Main St, City, State, ZIP",
  "avatar": "https://example.com/avatars/john.jpg",
  "allergies": "Penicillin, Peanuts",
  "medicalHistory": [
    {
      "id": 1,
      "condition": "Hypertension",
      "diagnosedDate": "2020-01-15",
      "notes": "Under medication"
    }
  ]
}
```

**Error Responses**:
- `404 Not Found`: Patient not found with ID

---

### 3.4 Update Patient

**Endpoint**: `PUT /api/v1/patients/{id}`

**Permission**: `CMP_PATIENT_WRITE`

**Path Parameters**:
- `id`: Patient ID

**Request Body**:
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "dob": "1990-05-15",
  "gender": "MALE",
  "bloodGroup": "O_POSITIVE",
  "contact": "9876543210",
  "email": "john.updated@example.com",
  "address": "456 New St, City, State, ZIP",
  "allergies": "Penicillin, Peanuts, Shellfish",
  "avatar": "https://example.com/avatars/john-new.jpg",
  "version": 0
}
```

**Response** (200 OK):
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "dob": "1990-05-15",
  "age": 33,
  "gender": "MALE",
  "bloodGroup": "O_POSITIVE",
  "contact": "9876543210",
  "email": "john.updated@example.com",
  "address": "456 New St, City, State, ZIP",
  "avatar": "https://example.com/avatars/john-new.jpg"
}
```

**Error Responses**:
- `404 Not Found`: Patient not found with ID
- `409 Conflict`: Version mismatch (concurrent update detected)

---

### 3.5 Delete Patient

**Endpoint**: `DELETE /api/v1/patients/{id}`

**Permission**: `CMP_PATIENT_DELETE`

**Path Parameters**:
- `id`: Patient ID

**Example Request**:
```
DELETE /api/v1/patients/1
```

**Response** (204 No Content):
```
(Empty body)
```

**Error Responses**:
- `404 Not Found`: Patient not found with ID

---

## 4. Error Handling

All endpoints use global exception handling:

### Common Error Response Format
```json
{
  "timestamp": "2026-02-10T12:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Patient with this contact number already exists.",
  "path": "/api/v1/patients"
}
```

### HTTP Status Codes
- `200 OK`: Successful GET/PUT
- `201 Created`: Successful POST
- `204 No Content`: Successful DELETE
- `400 Bad Request`: Validation error
- `401 Unauthorized`: Authentication required
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `409 Conflict`: Optimistic locking conflict

---

## 5. Best Practices

### Client Implementation

1. **Always include JWT token** in Authorization header
2. **Handle version field** for updates to prevent conflicts
3. **Implement retry logic** for 409 Conflict responses
4. **Validate input** on client side before submission
5. **Use pagination** for large result sets
6. **Cache patient details** to reduce API calls

### Performance Tips

1. Use search with pagination for large datasets
2. Specify minimal page size needed
3. Use appropriate sort fields (indexed fields perform better)
4. Cache frequently accessed patient data
5. Implement debouncing for search input

---

## 6. Related Documentation

- [Patient Module Overview](./01_PATIENT_MODULE_OVERVIEW.md)
- [Search & Filtering](./03_SEARCH_AND_FILTERING.md)
- [API Testing Guide](./05_API_TESTING_GUIDE.md)
