# API Testing Guide

This document provides comprehensive testing examples for the Patient module API endpoints using cURL, Postman, and automated testing approaches.

## 1. Prerequisites

### Authentication

All Patient API endpoints require JWT authentication. First, obtain a token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "admin",
  "roles": ["ADMIN"]
}
```

**Save the token** for use in subsequent requests:
```bash
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 2. Patient Registration Tests

### 2.1 Successful Registration

```bash
curl -X POST http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "dob": "1990-05-15",
    "gender": "MALE",
    "bloodGroup": "O_POSITIVE",
    "contact": "9876543210",
    "email": "john.doe@example.com",
    "address": "123 Main St, City, State, ZIP",
    "allergies": "Penicillin, Peanuts"
  }'
```

**Expected Response** (201 Created):
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
  "avatar": null
}
```

### 2.2 Duplicate Contact Number

```bash
# Register first patient
curl -X POST http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Smith",
    "dob": "1985-08-22",
    "gender": "FEMALE",
    "contact": "9876543210",
    "email": "jane.smith@example.com"
  }'
```

**Expected Response** (400 Bad Request):
```json
{
  "timestamp": "2026-02-10T12:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Patient with this contact number already exists.",
  "path": "/api/v1/patients"
}
```

### 2.3 Duplicate Email

```bash
curl -X POST http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Robert",
    "lastName": "Johnson",
    "dob": "1978-12-10",
    "gender": "MALE",
    "contact": "9876543299",
    "email": "john.doe@example.com"
  }'
```

**Expected Response** (400 Bad Request):
```json
{
  "timestamp": "2026-02-10T12:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Patient with this email already exists.",
  "path": "/api/v1/patients"
}
```

### 2.4 Composite Duplicate (Name + DOB + Contact)

```bash
curl -X POST http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "dob": "1990-05-15",
    "gender": "MALE",
    "contact": "9876543210"
  }'
```

**Expected Response** (400 Bad Request):
```json
{
  "message": "Duplicate patient record found."
}
```

---

## 3. Patient Search Tests

### 3.1 Search by Name

```bash
# Search for "john"
curl -X GET "http://localhost:8080/api/v1/patients?query=john" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response** (200 OK):
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
      "avatar": null
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true
}
```

### 3.2 Search by Contact

```bash
curl -X GET "http://localhost:8080/api/v1/patients?query=987" \
  -H "Authorization: Bearer $TOKEN"
```

### 3.3 Search by Email

```bash
curl -X GET "http://localhost:8080/api/v1/patients?query=example.com" \
  -H "Authorization: Bearer $TOKEN"
```

### 3.4 Paginated Search

```bash
# Page 0, size 10
curl -X GET "http://localhost:8080/api/v1/patients?query=doe&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# Page 1, size 10
curl -X GET "http://localhost:8080/api/v1/patients?query=doe&page=1&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

### 3.5 Sorted Search

```bash
# Sort by last name ascending
curl -X GET "http://localhost:8080/api/v1/patients?sort=lastName,asc" \
  -H "Authorization: Bearer $TOKEN"

# Sort by creation date descending
curl -X GET "http://localhost:8080/api/v1/patients?sort=createdAt,desc" \
  -H "Authorization: Bearer $TOKEN"

# Multiple sort fields
curl -X GET "http://localhost:8080/api/v1/patients?sort=lastName,asc&sort=firstName,asc" \
  -H "Authorization: Bearer $TOKEN"
```

### 3.6 Get All Patients

```bash
curl -X GET "http://localhost:8080/api/v1/patients" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 4. Patient Details Tests

### 4.1 Get Patient by ID

```bash
curl -X GET http://localhost:8080/api/v1/patients/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response** (200 OK):
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
  "avatar": null,
  "allergies": "Penicillin, Peanuts",
  "medicalHistory": [
    {
      "id": 1,
      "conditionName": "Hypertension",
      "diagnosedDate": "2020-01-15",
      "notes": "Under medication"
    }
  ]
}
```

### 4.2 Patient Not Found

```bash
curl -X GET http://localhost:8080/api/v1/patients/9999 \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response** (404 Not Found):
```json
{
  "timestamp": "2026-02-10T12:30:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Patient not found with ID: 9999",
  "path": "/api/v1/patients/9999"
}
```

---

## 5. Patient Update Tests

### 5.1 Successful Update

```bash
curl -X PUT http://localhost:8080/api/v1/patients/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
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
    "version": 0
  }'
```

**Expected Response** (200 OK):
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
  "avatar": null
}
```

### 5.2 Optimistic Locking Conflict

```bash
# Try to update with old version number
curl -X PUT http://localhost:8080/api/v1/patients/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.conflict@example.com",
    "version": 0
  }'
```

**Expected Response** (409 Conflict):
```json
{
  "timestamp": "2026-02-10T12:30:45.123Z",
  "status": 409,
  "error": "Conflict",
  "message": "Patient record has been modified by another transaction. Please refresh and try again.",
  "path": "/api/v1/patients/1"
}
```

---

## 6. Patient Delete Tests

### 6.1 Successful Delete

```bash
curl -X DELETE http://localhost:8080/api/v1/patients/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response** (204 No Content):
```
(Empty body)
```

### 6.2 Verify Soft Delete

```bash
# Try to get deleted patient
curl -X GET http://localhost:8080/api/v1/patients/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response** (404 Not Found):
```json
{
  "message": "Patient not found with ID: 1"
}
```

---

## 7. Authorization Tests

### 7.1 Missing Token

```bash
curl -X GET http://localhost:8080/api/v1/patients
```

**Expected Response** (401 Unauthorized):
```json
{
  "timestamp": "2026-02-10T12:30:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/patients"
}
```

### 7.2 Invalid Token

```bash
curl -X GET http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer invalid-token"
```

**Expected Response** (401 Unauthorized)

### 7.3 Insufficient Permissions

```bash
# Login as user without patient permissions
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "viewer",
    "password": "viewer123"
  }'

# Try to create patient
curl -X POST http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer $VIEWER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{...}'
```

**Expected Response** (403 Forbidden):
```json
{
  "timestamp": "2026-02-10T12:30:45.123Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/v1/patients"
}
```

---

## 8. Postman Collection

### Collection Structure

```json
{
  "info": {
    "name": "HMS Patient API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Authentication",
      "item": [
        {
          "name": "Login",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/auth/login",
            "body": {
              "mode": "raw",
              "raw": "{\"username\":\"admin\",\"password\":\"admin123\"}"
            }
          }
        }
      ]
    },
    {
      "name": "Patients",
      "item": [
        {
          "name": "Register Patient",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/patients",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{token}}"
              }
            ]
          }
        }
      ]
    }
  ]
}
```

### Environment Variables

```json
{
  "name": "HMS Local",
  "values": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080/api/v1"
    },
    {
      "key": "token",
      "value": ""
    }
  ]
}
```

---

## 9. Integration Test Examples

### Spring Boot Test

```java
@SpringBootTest
@AutoConfigureMockMvc
class PatientControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private String token;
    
    @BeforeEach
    void setUp() throws Exception {
        // Login and get token
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        token = JsonPath.read(response, "$.token");
    }
    
    @Test
    void testRegisterPatient() throws Exception {
        PatientRegisterRequest request = PatientRegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .dob(LocalDate.of(1990, 5, 15))
                .gender(Patient.Gender.MALE)
                .bloodGroup(Patient.BloodGroup.O_POSITIVE)
                .contact("9876543210")
                .email("john.doe@example.com")
                .build();
        
        mockMvc.perform(post("/api/v1/patients")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }
    
    @Test
    void testDuplicateContact() throws Exception {
        // Register first patient
        PatientRegisterRequest request1 = createPatientRequest("9876543210");
        mockMvc.perform(post("/api/v1/patients")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());
        
        // Try to register with same contact
        PatientRegisterRequest request2 = createPatientRequest("9876543210");
        mockMvc.perform(post("/api/v1/patients")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                    .value("Patient with this contact number already exists."));
    }
}
```

---

## 10. Related Documentation

- [Patient Module Overview](./01_PATIENT_MODULE_OVERVIEW.md)
- [Patient Service & Endpoints](./02_PATIENT_SERVICE_AND_ENDPOINTS.md)
- [Security & Permissions](./06_SECURITY_AND_PERMISSIONS.md)
