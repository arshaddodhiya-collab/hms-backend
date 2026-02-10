# Patient Module Documentation

Comprehensive documentation for the Hospital Management System (HMS) Patient Module, covering patient registration, management, and medical history tracking.

## 📚 Documentation Index

1. **[Patient Module Overview](./01_PATIENT_MODULE_OVERVIEW.md)**
   - Core components and architecture
   - Entities, DTOs, Services, and Controllers
   - Directory structure
   - Key features overview

2. **[Patient Service & Endpoints](./02_PATIENT_SERVICE_AND_ENDPOINTS.md)**
   - PatientService business logic
   - CRUD operations and validation
   - REST API endpoints
   - Request/response examples
   - Duplicate detection logic

3. **[Search & Filtering](./03_SEARCH_AND_FILTERING.md)**
   - PatientSpecification implementation
   - Search capabilities
   - Pagination and sorting
   - Query examples

4. **[Database Schema](./04_DATABASE_SCHEMA.md)**
   - Table structures and relationships
   - Entity relationships diagram
   - Common SQL queries
   - JPA/Hibernate queries
   - Migration scripts
   - Indexing strategy

5. **[API Testing Guide](./05_API_TESTING_GUIDE.md)**
   - Authentication prerequisites
   - Patient API test cases
   - Integration test scenarios
   - Error response examples
   - Postman collection structure

6. **[Security & Permissions](./06_SECURITY_AND_PERMISSIONS.md)**
   - Security architecture
   - Permission definitions
   - Role-based access control
   - Endpoint security annotations
   - Best practices

## 🚀 Quick Start

### Prerequisites
- Java 17+
- MySQL 8.0+
- Maven 3.6+
- Valid JWT token (obtain via `/api/v1/auth/login`)

### Base URL
```
http://localhost:8080/api/v1
```

### Authentication
All Patient endpoints require authentication:
```bash
Authorization: Bearer <your-jwt-token>
```

### Required Permissions
- **Patient Management**: `CMP_PATIENT_READ`, `CMP_PATIENT_WRITE`, `CMP_PATIENT_DELETE`

## 📋 API Endpoints Summary

### Patients
| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/patients` | `CMP_PATIENT_WRITE` | Register new patient |
| GET | `/patients` | `CMP_PATIENT_READ` | Search/list patients (paginated) |
| GET | `/patients/{id}` | `CMP_PATIENT_READ` | Get patient details with medical history |
| PUT | `/patients/{id}` | `CMP_PATIENT_WRITE` | Update patient information |
| DELETE | `/patients/{id}` | `CMP_PATIENT_DELETE` | Soft delete patient |

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Client (Angular)                      │
│                  JWT Token in Header                     │
└────────────────────┬────────────────────────────────────┘
                     │ HTTPS
                     ▼
┌─────────────────────────────────────────────────────────┐
│              Spring Security Filter Chain                │
│  ┌──────────────────────────────────────────────────┐  │
│  │        JwtAuthenticationFilter                    │  │
│  │  - Validates JWT                                  │  │
│  │  - Sets SecurityContext                           │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                  PatientController                       │
│              @PreAuthorize Annotations                   │
│  - POST   /patients                                      │
│  - GET    /patients                                      │
│  - GET    /patients/{id}                                 │
│  - PUT    /patients/{id}                                 │
│  - DELETE /patients/{id}                                 │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                   PatientService                         │
│  - Duplicate detection (contact, email, name+DOB)       │
│  - Validation and business logic                        │
│  - Optimistic locking (version control)                 │
│  - Soft delete implementation                           │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              PatientRepository (JPA)                     │
│  - JpaSpecificationExecutor for dynamic queries         │
│  - Custom queries for duplicate detection               │
│  - Indexed searches (name, contact)                     │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                  MySQL Database                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │              patients table                       │  │
│  │  - id (PK)                                        │  │
│  │  - first_name, last_name (indexed)                │  │
│  │  - dob, gender, blood_group                       │  │
│  │  - contact (unique, indexed)                      │  │
│  │  - email (unique)                                 │  │
│  │  - address, allergies, avatar                     │  │
│  │  - version (optimistic locking)                   │  │
│  │  - active, deleted, created_at, updated_at        │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │          medical_history table                    │  │
│  │  - patient_id (FK) → patients.id                  │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## 🔑 Key Features

### Patient Registration
- ✅ Comprehensive patient information capture
- ✅ Multi-level duplicate detection
  - Contact number uniqueness
  - Email uniqueness
  - Name + DOB + Contact combination
- ✅ Automatic age calculation from DOB
- ✅ Blood group and gender enums
- ✅ Avatar/photo support

### Patient Management
- ✅ Full CRUD operations
- ✅ Advanced search (name, contact, email)
- ✅ Pagination and sorting
- ✅ Optimistic locking for concurrent updates
- ✅ Soft delete (preserves data)
- ✅ Medical history tracking

### Data Integrity
- ✅ Unique constraints (contact, email)
- ✅ Indexed searches for performance
- ✅ Version control for concurrent updates
- ✅ Cascade operations for medical history
- ✅ Audit fields (created_at, updated_at)

### Security
- ✅ JWT-based authentication
- ✅ Fine-grained permissions
- ✅ Role-based access control
- ✅ Method-level security with @PreAuthorize
- ✅ Global exception handling

## 📦 Database Schema

### Core Tables
- `patients`: Patient demographic and contact information
- `medical_history`: Patient medical history records (one-to-many)

### Relationships
- Patient → MedicalHistory (One-to-Many): Medical records cascade

### Indexes
- `idx_patient_name`: Composite index on (first_name, last_name)
- `idx_patient_contact`: Index on contact for fast lookups

## 🧪 Testing

### Quick Test with cURL

**Login**:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

**Register Patient**:
```bash
curl -X POST http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "dob": "1990-05-15",
    "gender": "MALE",
    "bloodGroup": "O_POSITIVE",
    "contact": "9876543210",
    "email": "john.doe@example.com",
    "address": "123 Main St, City",
    "allergies": "Penicillin"
  }'
```

**Search Patients**:
```bash
curl -X GET "http://localhost:8080/api/v1/patients?query=john&page=0&size=10" \
  -H "Authorization: Bearer <your-token>"
```

**Get Patient Details**:
```bash
curl -X GET http://localhost:8080/api/v1/patients/1 \
  -H "Authorization: Bearer <your-token>"
```

For comprehensive testing examples, see [API Testing Guide](./05_API_TESTING_GUIDE.md).

## 🛠️ Development

### Project Structure
```
src/main/java/com/hms/HospitalManagementSystem/
├── controller/
│   └── PatientController.java
├── dto/
│   ├── request/
│   │   ├── PatientRegisterRequest.java
│   │   └── PatientUpdateRequest.java
│   └── response/
│       ├── PatientResponse.java
│       └── PatientDetailsResponse.java
├── entity/
│   ├── Patient.java
│   └── MedicalHistory.java
├── repository/
│   └── PatientRepository.java
├── service/
│   └── PatientService.java
├── mapper/
│   └── PatientMapper.java
├── specification/
│   └── PatientSpecification.java
└── exception/
    └── GlobalExceptionHandler.java
```

### Running the Application
```bash
# Start MySQL
mysql -u root -p

# Run Spring Boot
./mvnw spring-boot:run

# Application runs on http://localhost:8080
```

## 📖 Related Documentation

- [Authentication Flow](../auth_docs/02_AUTHENTICATION_FLOW.md)
- [JWT Implementation](../auth_docs/04_JWT_IMPLEMENTATION.md)
- [Database Migration Guide](../DATABASE_MIGRATION_GUIDE.md)
- [Backend Best Practices](../BEST_PRACTICES_BACKEND.md)
- [Database Implementation Plan](../database_implementation_plan.md)

## 🤝 Contributing

When extending the Patient module:
1. Follow existing patterns for DTOs, Services, and Controllers
2. Add appropriate `@PreAuthorize` annotations
3. Update documentation
4. Add test cases
5. Update migration scripts if schema changes
6. Maintain duplicate detection logic

## 📝 License

Part of the Hospital Management System (HMS) project.

---

**Last Updated**: February 2026  
**Version**: 1.0  
**Maintained By**: HMS Development Team
