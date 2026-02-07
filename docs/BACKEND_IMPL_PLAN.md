# IMPLEMENTATION_PLAN.md

## 1️⃣ Project Overview
This project is a **production-grade Hospital Management System (HMS) backend** built with **Spring Boot 3** and **MySQL**. It utilizes a **pure REST API architecture** to support an Angular frontend.

The system is designed for **multi-tenant capability (future-proof)**, **role-based access control (RBAC)**, and **scalable modules** for typical hospital operations (Patients, Appointments, Triage, Consultation, Lab, Billing).

### High-Level Architecture
- **Language**: Java 17+
- **Framework**: Spring Boot 3.2+
- **Database**: MySQL 8.0
- **ORM**: Spring Data JPA (Hibernate)
- **Security**: Spring Security 6 + JWT (Stateless)
- **Documentation**: OpenAPI 3 (Swagger)
- **Migrations**: Flyway
- **Logging**: SLF4J + Logback (JSON format for ease of parsing)

---

## 2️⃣ Feature → API Mapping

| Feature | Frontend Component | Backend API Group | Method | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Auth** | Login Page | `/api/v1/auth` | `POST` | Authenticate user & issue JWT |
| | | `/api/v1/auth` | `GET` | Get current user context (Me) |
| **Dashboard** | Dashboard | `/api/v1/stats` | `GET` | Fetch aggregate stats for dashboard |
| **Patients** | PatientList | `/api/v1/patients` | `GET` | Search/List patients (Paginated) |
| | PatientRegister | `/api/v1/patients` | `POST` | Register new patient |
| | PatientView | `/api/v1/patients/{id}` | `GET` | Get detailed patient info |
| | PatientEdit | `/api/v1/patients/{id}` | `PUT` | Update patient details |
| **Appointments** | ApptList | `/api/v1/appointments` | `GET` | List appointments (Filter by Date/Doc) |
| | ApptCreate | `/api/v1/appointments` | `POST` | Schedule appointment |
| | ApptStatus | `/api/v1/appointments/{id}/status` | `PATCH` | Update status (Confirm/Cancel/Comp) |
| **Triage** | TriageList | `/api/v1/encounters` | `GET` | List active encounters |
| | VitalsForm | `/api/v1/encounters/{id}/vitals` | `POST` | Record vitals for an encounter |
| **Consultation** | ConsultView | `/api/v1/consultations` | `GET` | Get consultation history |
| | Prescription | `/api/v1/consultations/{id}/rx` | `POST` | Add prescription |
| **Lab** | LabList | `/api/v1/lab-requests` | `GET` | View pending lab tests |
| | LabResults | `/api/v1/lab-requests/{id}/results` | `POST` | Upload/Enter lab results |
| **Billing** | InvoiceList | `/api/v1/billing/invoices` | `GET` | List invoices |
| | InvoiceGenerate | `/api/v1/billing/invoices` | `POST` | Create/Auto-generate invoice |
| **IPD** | AdmissionList | `/api/v1/ipd/admissions` | `GET` | List admissions |
| | BedManagement | `/api/v1/ipd/beds` | `GET` | Get bed status |
| | Discharge | `/api/v1/ipd/admissions/{id}/discharge` | `POST` | Discharge patient |
| **Admin** | UserMgmt | `/api/v1/admin/users` | `GET/POST`| Manage internal users |

---

## 3️⃣ API Design

### Authentication
#### Login
- **Endpoint**: `POST /api/v1/auth/login`
- **Request**: `LoginRequest { username, password }`
- **Response**: `AuthResponse { token, refreshToken, type, username, roles[] }`
- **Security**: Public

### Patients
#### Create Patient
- **Endpoint**: `POST /api/v1/patients`
- **Request**: `PatientDto`
  ```json
  {
    "firstName": "John",
    "lastName": "Doe",
    "age": 30,
    "gender": "MALE",
    "contact": "1234567890",
    "email": "john@example.com",
    "allergies": "Peanuts",
    "avatar": "https://example.com/avatar.png"
  }
  ```
- **Response**: `PatientDto` (with ID and audit fields)
- **Roles**: `RECEPTION`, `ADMIN`

#### Get Patient Detail
- **Endpoint**: `GET /api/v1/patients/{id}`
- **Response**: `PatientDetailDto` (Includes basic info + recent visits summary)
- **Roles**: `DOCTOR`, `NURSE`, `RECEPTION`, `ADMIN`

### Appointments
#### Schedule Appointment
- **Endpoint**: `POST /api/v1/appointments`
- **Request**: `AppointmentCreateDto`
  ```json
  {
    "patientId": 1,
    "doctorId": 2,
    "dateTime": "2023-11-01T10:00:00",
    "type": "CONSULTATION",
    "notes": "Patient complains of headache"
  }
  ```
- **Response**: `AppointmentDto`
- **Roles**: `RECEPTION`, `DOCTOR`

### IPD (Inpatient)
#### Admit Patient
- **Endpoint**: `POST /api/v1/ipd/admissions`
- **Request**: `{ patientId, bedId, doctorId, diagnosis }`
- **Response**: `AdmissionDto`

#### Discharge Patient
- **Endpoint**: `POST /api/v1/ipd/admissions/{id}/discharge`
- **Request**: `{ dischargeSummary, diagnosis }`
- **Response**: `AdmissionDto` (Status: DISCHARGED)

### Billing
#### Create Invoice
- **Endpoint**: `POST /api/v1/billing/invoices`
- **Request**: `{ patientId, admissionId?, items: [{desc, amount}] }`
- **Response**: `InvoiceDto`

---

## 4️⃣ Database Design (MySQL)

### Tables

#### `users`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | |
| `username` | VARCHAR(50) | UQ, NN | Login ID |
| `password` | VARCHAR(255) | NN | BCrypt Hash |
| `full_name` | VARCHAR(100)| | Display Name |
| `department_id` | BIGINT | FK | |
| `active` | BOOLEAN | DEF TRUE | Soft disable |
| `created_at` | DATETIME | | Audit |

#### `roles`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | |
| `name` | VARCHAR(50) | UQ, NN | ADMIN, DOCTOR etc |
| `description` | VARCHAR(255)| | |

#### `permissions`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | |
| `code` | VARCHAR(50) | UQ, NN | E.g. MOD_PATIENTS |
| `module` | VARCHAR(50) | NN | |

#### `user_roles` (Join)
| Column | Type | Constraints |
| :--- | :--- | :--- |
| `user_id` | BIGINT | FK |
| `role_id` | BIGINT | FK |

#### `role_permissions` (Join)
| Column | Type | Constraints |
| :--- | :--- | :--- |
| `role_id` | BIGINT | FK |
| `permission_id` | BIGINT | FK |

#### `patients`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | |
| `first_name` | VARCHAR(50) | NN | |
| `last_name` | VARCHAR(50) | NN | |
| `dob` | DATE | NN | Calculated Age on frontend |
| `gender` | ENUM | NN | MALE, FEMALE, OTHER |
| `contact` | VARCHAR(20) | NN | |
| `email` | VARCHAR(100)| | |
| `created_at` | DATETIME | | |

#### `appointments`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | |
| `patient_id` | BIGINT | FK -> patients | |
| `doctor_id` | BIGINT | FK -> users | |
| `start_time` | DATETIME | NN | |
| `status` | ENUM | NN | PENDING, CONFIRMED, CANCELLED |
| `reason` | TEXT | | |

#### `encounters` (Visit/Consultation)
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | |
| `appointment_id`| BIGINT | FK -> appts | Optional (can be walk-in) |
| `patient_id` | BIGINT | FK -> patients | |
| `doctor_id` | BIGINT | FK -> users | |
| `vitals_json` | JSON | | BP, Temp, etc. |
| `diagnosis` | TEXT | | |
| `prescription_json`| JSON | | Simple JSON for V1 |
| `status` | ENUM | | TRIAGE, IN_PROGRESS, FINISHED |

---

## 5️⃣ Entity Layer
- Use `@Entity` with `@Table`.
- Extend a common `BaseEntity` (`id`, `createdAt`, `updatedAt`, `version`).
- Use `FetchType.LAZY` for all `ManyToOne` and `OneToMany`.
- Use `Enums` for Status, Gender, Roles.

## 6️⃣ Repository Layer
- Extend `JpaRepository<T, Long>`.
- Use `JpaSpecificationExecutor` for complex search (Patients search).
- Avoid N+1 queries by using `@EntityGraph` or `JOIN FETCH` in custom queries.

## 7️⃣ Service Layer
- **Transaction Management**: `@Transactional(readOnly = true)` on class level, `@Transactional` on writing methods.
- **Validation**:
    - Input validation (DTO) using Jakarta Validation (`@Valid`).
    - Business validation (checks against DB state) inside service.
- **Mapping**: Use `MapStruct` for Entity <-> DTO conversion for performance.

## 8️⃣ Controller Layer
- `@RestController` and `@RequestMapping("/api/v1/...")`.
- Return `ResponseEntity<T>`.
- Utilize a generic `ApiResponse<T>` wrapper (Optional, but good for consistent meta-data).
- **Versioning**: URI Versioning `/api/v1`.

## 9️⃣ Security Design
- **Core**: Spring Security 6.
- **Mechanism**: JWT (JSON Web Tokens).
- **Flow**:
    1. Public `/auth/login` returns Access (15min) & Refresh (7d) tokens.
    2. `JwtAuthenticationFilter` intercepts requests, validates Token.
    3. `UserDetailsService` loads users from DB.
- **Passwords**: BCrypt encoding (Strength 10).
- **CORS**: Configured to allow Frontend origin.

## 🔟 Exception Handling
- `@ControllerAdvice` global handler.
- **Custom Exceptions**:
    - `ResourceNotFoundException` (404)
    - `BadRequestException` (400)
    - `BusinessRuleViolationException` (409)
- **Response**:
  ```json
  {
    "timestamp": "...",
    "status": 404,
    "error": "Not Found",
    "message": "Patient with ID 5 not found",
    "path": "..."
  }
  ```

## 1️⃣1️⃣ Logging & Monitoring
- **Slf4J** with Logback.
- **Structure**: JSON logs (in Prod) for ELK stack compatibility.
- **Correlation**: Generate `UUID` per request in a filter, add to MDC (Mapped Diagnostic Context).

## 1️⃣2️⃣ Validation Strategy
- **Annotations**: `@NotNull`, `@Size`, `@Email`, `@Past` (for DOB) on DTOs.
- **Custom Validators**: If complex logic (e.g. Appointment overlap check).

## 1️⃣3️⃣ Environment Configuration
- `application.yml` base config.
- `application-dev.yml`: Local DB, debug logs.
- `application-prod.yml`: RDS/Cloud SQL, INFO logs, Secrets from ENV variables.

## 1️⃣4️⃣ Folder Structure
```text
com.hms.backend
├── config          # Security, Swagger, CORS
├── controller      # REST Endpoints
├── dto             # Data Transfer Objects
│   ├── request
│   └── response
├── entity          # JPA Entities
├── exception       # Global Handlers
├── mapper          # MapStruct Mappers
├── repository      # JPA Repositories
├── security        # JWT, UserDetails
├── service         # Business Logic
│   └── impl
└── util            # Helper classes
```

## 1️⃣5️⃣ Development Roadmap
1. **Setup**: Spring Initializr, DB connection, Flyway.
2. **Auth**: User Entity, JWT logic, Login Endpoint.
3. **Patients**: Patient Entity, CRUD APIs, Search.
4. **Appointments**: Booking logic, validations.
5. **Core Clinical**: Encounters, Vitals, Consultation notes.
6. **Support**: Lab & Billing basic implementation.
7. **Hardening**: Security audit, Indexes, Pagination.
8. **Deployment**: Dockerfile, CI/CD pipeline.
