# Encounter Module Architecture

## System Overview

The Encounter Module follows a layered architecture pattern with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│                    Controllers                          │
│  (REST API Layer - Request/Response DTOs)               │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    Services                             │
│  (Business Logic - Validation, State Management)        │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                  Repositories                           │
│  (Data Access Layer - JPA)                              │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    Database                             │
│  (MySQL - Flyway Migrations)                            │
└─────────────────────────────────────────────────────────┘
```

## Component Architecture

### 1. Entity Layer

#### Encounter Entity
**Purpose**: Core entity representing a patient visit

**Key Relationships**:
- `@OneToOne` with Appointment (unique constraint)
- `@ManyToOne` with Patient
- `@ManyToOne` with User (Doctor)
- `@OneToOne` with Vitals (cascade)
- `@OneToMany` with Prescriptions (cascade)

**State Machine**:
```
TRIAGE → IN_PROGRESS → COMPLETED
```

**Audit Fields**:
- `createdAt`: Timestamp of encounter creation
- `updatedAt`: Last modification timestamp
- `deleted`: Soft delete flag

#### Vitals Entity
**Purpose**: Store patient vital signs

**Relationships**:
- `@OneToOne` with Encounter
- `@ManyToOne` with User (recordedBy)

**Calculated Fields**:
- BMI = weight(kg) / (height(m))²

#### Prescription Entity
**Purpose**: Medication prescriptions

**Relationships**:
- `@ManyToOne` with Encounter
- `@OneToMany` with PrescriptionItems (cascade)

**Status Flow**:
```
DRAFT → ISSUED (on encounter completion)
```

#### PrescriptionItem Entity
**Purpose**: Individual medication entries

**Fields**:
- Medicine name, dosage, frequency, duration

### 2. Repository Layer

```java
// Spring Data JPA Repositories
EncounterRepository extends JpaRepository<Encounter, Long>
VitalsRepository extends JpaRepository<Vitals, Long>
PrescriptionRepository extends JpaRepository<Prescription, Long>
```

**Custom Queries**:
- `findByAppointmentId`: Idempotent encounter lookup
- `findByStatus`: Queue management
- `findByDoctorIdAndStatus`: Doctor-specific queues
- `findByEncounterId`: Related data retrieval

### 3. Service Layer

#### EncounterService
**Responsibilities**:
- Encounter lifecycle management
- State transition validation
- Appointment status synchronization
- Queue management
- Authorization checks

**Key Methods**:
```java
startEncounter(appointmentId, patientId, doctorId)
  → Creates encounter (idempotent)
  → Updates appointment to CHECKED_IN
  → Sets status to TRIAGE

updateClinicalNotes(id, complaint, diagnosis, notes, userId)
  → Validates ownership
  → Transitions TRIAGE → IN_PROGRESS
  → Updates appointment status

completeEncounter(id, userId)
  → Validates diagnosis present
  → Sets status to COMPLETED
  → Issues prescriptions (DRAFT → ISSUED)
  → Updates appointment to COMPLETED
```

#### VitalsService
**Responsibilities**:
- Vitals recording and retrieval
- BMI calculation
- Audit trail management

**Key Methods**:
```java
saveVitals(encounterId, vitalsData, userId)
  → Creates or updates vitals
  → Calculates BMI
  → Records user who captured vitals

getVitalsByEncounterId(encounterId)
  → Retrieves vitals for display
```

#### PrescriptionService
**Responsibilities**:
- Prescription CRUD operations
- Item management
- Status transitions

**Key Methods**:
```java
savePrescription(encounterId, note, items, userId)
  → Creates/updates prescription
  → Replaces prescription items
  → Maintains DRAFT status

getPrescriptionByEncounterId(encounterId)
  → Retrieves prescription with items
```

### 4. Controller Layer

#### EncounterController
**Endpoints**:
- `POST /api/v1/encounters` - Start encounter
- `PATCH /api/v1/encounters/{id}/clinical-notes` - Update notes
- `PATCH /api/v1/encounters/{id}/complete` - Complete encounter
- `GET /api/v1/encounters/{id}` - Get by ID
- `GET /api/v1/encounters/by-appointment/{appointmentId}` - Get by appointment
- `GET /api/v1/encounters/queue/triage` - Triage queue
- `GET /api/v1/encounters/queue/doctor/{doctorId}` - Doctor queue

**Security**:
- `@PreAuthorize` annotations for RBAC
- User context from Spring Security

#### VitalsController
**Endpoints**:
- `POST /api/v1/encounters/{encounterId}/vitals` - Save vitals
- `GET /api/v1/encounters/{encounterId}/vitals` - Get vitals

#### PrescriptionController
**Endpoints**:
- `POST /api/v1/encounters/{encounterId}/prescriptions` - Save prescription
- `GET /api/v1/encounters/{encounterId}/prescriptions` - Get prescription

## Design Patterns

### 1. Repository Pattern
- Abstracts data access logic
- Enables testing with mocks
- Provides clean separation

### 2. Service Layer Pattern
- Encapsulates business logic
- Manages transactions
- Coordinates multiple repositories

### 3. DTO Pattern
- Decouples API from domain model
- Enables API versioning
- Reduces over-fetching

### 4. Builder Pattern
- Used in entities for clean object construction
- Improves readability

### 5. State Machine Pattern
- Encounter status transitions
- Prescription status flow
- Appointment synchronization

## Data Flow

### Creating an Encounter
```
1. Controller receives POST /api/v1/encounters
2. Maps DTO to service parameters
3. Service checks if encounter exists (idempotent)
4. If not exists:
   a. Validates appointment exists
   b. Validates patient/doctor match
   c. Creates encounter entity
   d. Updates appointment status
   e. Saves to database
5. Returns encounter entity
6. Controller maps to EncounterResponse DTO
```

### Completing an Encounter
```
1. Controller receives PATCH /api/v1/encounters/{id}/complete
2. Service retrieves encounter
3. Validates:
   - Encounter exists
   - User is the assigned doctor
   - Diagnosis is present
   - Status is IN_PROGRESS
4. Updates:
   - Encounter status → COMPLETED
   - Encounter completedAt timestamp
   - Appointment status → COMPLETED
   - All prescriptions status → ISSUED
   - All prescriptions issuedAt timestamp
5. Saves all changes (transactional)
6. Returns updated encounter
```

## Transaction Management

All service methods are annotated with `@Transactional`:
- Ensures atomic operations
- Automatic rollback on exceptions
- Maintains data consistency

## Error Handling

### Custom Exceptions
- `ResourceNotFoundException`: Entity not found
- `ConflictException`: Business rule violation
- `AccessDeniedException`: Authorization failure

### Exception Flow
```
Service throws exception
    ↓
GlobalExceptionHandler catches
    ↓
Returns appropriate HTTP status
    ↓
Client receives error response
```

## Security Architecture

### Authentication
- JWT-based authentication
- User context from SecurityContextHolder

### Authorization
- Method-level security with `@PreAuthorize`
- Permission-based access control
- Ownership validation in service layer

### Permissions
- `CMP_VITALS_READ/WRITE`: Vitals access
- `CMP_CONSULTATION_READ/WRITE`: Encounter access
- `CMP_PRESCRIPTION_READ/WRITE`: Prescription access

## Database Design

### Normalization
- 3NF compliance
- No redundant data
- Foreign key constraints

### Indexing Strategy
```sql
-- Encounter indexes
idx_encounter_appointment (appointment_id)
idx_encounter_patient (patient_id)
idx_encounter_doctor (doctor_id)
idx_encounter_status (status)

-- Vitals indexes
idx_vitals_encounter (encounter_id)

-- Prescription indexes
idx_prescription_encounter (encounter_id)
idx_item_prescription (prescription_id)
```

### Cascade Rules
- `CascadeType.ALL` for owned relationships
- `orphanRemoval = true` for dependent entities
- Maintains referential integrity

## Performance Considerations

### Lazy Loading
- `@ManyToOne` and `@OneToOne` use `FetchType.LAZY`
- Prevents N+1 query problems
- Explicit fetching when needed

### Query Optimization
- Indexed foreign keys
- Efficient status queries
- Batch operations where applicable

## Scalability

### Horizontal Scaling
- Stateless service design
- Database connection pooling
- Transaction isolation

### Future Enhancements
- Read replicas for queries
- Caching layer (Redis)
- Event-driven architecture for notifications

## Testing Strategy

### Unit Tests
- Service layer with mocked repositories
- Business logic validation
- State transition testing

### Integration Tests
- Controller layer with MockMvc
- End-to-end API testing
- Database integration

## Monitoring & Observability

### Logging
- Service method entry/exit
- Exception logging
- Audit trail in database

### Metrics (Future)
- Encounter creation rate
- Average completion time
- Queue lengths
