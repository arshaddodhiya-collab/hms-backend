# Service Layer Documentation

## Overview
The service layer contains the business logic for the Encounter module. It sits between the controllers (API layer) and repositories (data access layer).

## Design Principles

### 1. Single Responsibility
Each service handles one domain entity and its related operations.

### 2. Transaction Management
All public service methods are annotated with `@Transactional` to ensure:
- Atomic operations
- Automatic rollback on exceptions
- Data consistency

### 3. Validation
Services validate:
- Business rules
- State transitions
- Authorization
- Data integrity

### 4. Separation of Concerns
- Controllers handle HTTP concerns
- Services handle business logic
- Repositories handle data access

---

## EncounterService

**File**: `EncounterService.java`

**Package**: `com.hms.HospitalManagementSystem.service`

### Dependencies
```java
@RequiredArgsConstructor
public class EncounterService {
    private final EncounterRepository encounterRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
}
```

### Methods

#### startEncounter
**Purpose**: Create a new encounter from an appointment (idempotent)

**Signature**:
```java
@Transactional
public Encounter startEncounter(Long appointmentId, Long patientId, Long doctorId)
```

**Logic Flow**:
1. Check if encounter already exists for appointment
2. If exists, return existing encounter (idempotency)
3. If not exists, call `createEncounter`

**Why Idempotent?**
- Prevents duplicate encounters
- Safe to retry on network failures
- Consistent behavior

---

#### createEncounter (private)
**Purpose**: Internal method to create new encounter

**Signature**:
```java
private Encounter createEncounter(Long appointmentId, Long patientId, Long doctorId)
```

**Validation**:
```java
// 1. Appointment must exist
Appointment appointment = appointmentRepository.findById(appointmentId)
    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

// 2. Appointment must not be cancelled or no-show
if (appointment.getStatus() == AppointmentStatus.CANCELLED ||
    appointment.getStatus() == AppointmentStatus.NO_SHOW) {
    throw new ConflictException("Cannot start encounter for cancelled or no-show appointment");
}

// 3. Patient ID must match
if (!appointment.getPatient().getId().equals(patientId)) {
    throw new ConflictException("Patient ID does not match appointment");
}

// 4. Doctor ID must match
if (!appointment.getDoctor().getId().equals(doctorId)) {
    throw new ConflictException("Doctor ID does not match appointment");
}

// 5. Doctor must exist
User doctor = userRepository.findById(doctorId)
    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
```

**Entity Creation**:
```java
Encounter encounter = Encounter.builder()
    .appointment(appointment)
    .patient(appointment.getPatient())
    .doctor(doctor)
    .status(EncounterStatus.TRIAGE)
    .startedAt(LocalDateTime.now())
    .build();
```

**Side Effects**:
```java
// Update appointment status if scheduled
if (appointment.getStatus() == AppointmentStatus.SCHEDULED) {
    appointment.setStatus(AppointmentStatus.CHECKED_IN);
    appointmentRepository.save(appointment);
}
```

**Why This Design?**
- Validates all business rules upfront
- Maintains data consistency
- Synchronizes appointment state
- Provides clear error messages

---

#### updateClinicalNotes
**Purpose**: Update encounter clinical information

**Signature**:
```java
@Transactional
public Encounter updateClinicalNotes(
    Long id,
    String chiefComplaint,
    String diagnosis,
    String notes,
    Long currentUserId
)
```

**Validation**:
```java
// 1. Encounter must exist
Encounter encounter = encounterRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Encounter not found"));

// 2. Cannot update completed encounters
if (encounter.getStatus() == EncounterStatus.COMPLETED) {
    throw new ConflictException("Cannot update clinical notes for completed encounter");
}

// 3. Only assigned doctor can update
if (!encounter.getDoctor().getId().equals(currentUserId)) {
    throw new AccessDeniedException("You can only modify your own encounters");
}
```

**State Transition**:
```java
// Update fields
encounter.setChiefComplaint(chiefComplaint);
encounter.setDiagnosis(diagnosis);
encounter.setNotes(notes);

// Transition from TRIAGE to IN_PROGRESS
if (encounter.getStatus() == EncounterStatus.TRIAGE) {
    encounter.setStatus(EncounterStatus.IN_PROGRESS);
    
    // Sync appointment status
    if (encounter.getAppointment().getStatus() != AppointmentStatus.IN_PROGRESS) {
        encounter.getAppointment().setStatus(AppointmentStatus.IN_PROGRESS);
        appointmentRepository.save(encounter.getAppointment());
    }
}
```

**Why This Design?**
- Enforces ownership (doctor can only update their encounters)
- Prevents modification of completed encounters
- Automatic state transition
- Keeps appointment in sync

---

#### completeEncounter
**Purpose**: Finalize encounter and issue prescriptions

**Signature**:
```java
@Transactional
public Encounter completeEncounter(Long id, Long currentUserId)
```

**Validation**:
```java
// 1. Encounter must exist
Encounter encounter = encounterRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Encounter not found"));

// 2. Must be in IN_PROGRESS status
if (encounter.getStatus() != EncounterStatus.IN_PROGRESS) {
    if (encounter.getStatus() == EncounterStatus.COMPLETED) {
        throw new ConflictException("Encounter is already completed");
    }
}

// 3. Only assigned doctor can complete
if (!encounter.getDoctor().getId().equals(currentUserId)) {
    throw new AccessDeniedException("You can only complete your own encounters");
}

// 4. Diagnosis is required
if (encounter.getDiagnosis() == null || encounter.getDiagnosis().trim().isEmpty()) {
    throw new ConflictException("Diagnosis is required to complete encounter");
}
```

**Completion Logic**:
```java
// Update encounter
encounter.setStatus(EncounterStatus.COMPLETED);
encounter.setCompletedAt(LocalDateTime.now());

// Update appointment
encounter.getAppointment().setStatus(AppointmentStatus.COMPLETED);
appointmentRepository.save(encounter.getAppointment());

// Issue all prescriptions
encounter.getPrescriptions().forEach(prescription -> {
    prescription.setStatus(PrescriptionStatus.ISSUED);
    prescription.setIssuedAt(LocalDateTime.now());
});

return encounterRepository.save(encounter);
```

**Why This Design?**
- Ensures diagnosis is documented
- Atomic completion (all or nothing)
- Automatically issues prescriptions
- Maintains referential integrity

---

#### Queue Methods

**getTriageQueue**:
```java
public List<Encounter> getTriageQueue() {
    return encounterRepository.findByStatus(EncounterStatus.TRIAGE);
}
```

**getDoctorQueue**:
```java
public List<Encounter> getDoctorQueue(Long doctorId) {
    return encounterRepository.findByDoctorIdAndStatus(
        doctorId,
        EncounterStatus.IN_PROGRESS
    );
}
```

**Why Separate Queues?**
- Triage queue: For nurses to record vitals
- Doctor queue: For doctors to see their active consultations
- Clear workflow separation

---

## VitalsService

**File**: `VitalsService.java`

**Package**: `com.hms.HospitalManagementSystem.service`

### Dependencies
```java
@RequiredArgsConstructor
public class VitalsService {
    private final VitalsRepository vitalsRepository;
    private final EncounterRepository encounterRepository;
    private final UserRepository userRepository;
}
```

### Methods

#### saveVitals
**Purpose**: Create or update vitals for an encounter

**Signature**:
```java
@Transactional
public Vitals saveVitals(Long encounterId, Vitals vitalsData, Long userId)
```

**Logic Flow**:
```java
// 1. Verify encounter exists
Encounter encounter = encounterRepository.findById(encounterId)
    .orElseThrow(() -> new ResourceNotFoundException("Encounter not found"));

// 2. Get or create vitals
Vitals vitals = vitalsRepository.findByEncounterId(encounterId)
    .orElse(new Vitals());

// 3. Update fields
vitals.setEncounter(encounter);
vitals.setTemperature(vitalsData.getTemperature());
vitals.setSystolic(vitalsData.getSystolic());
vitals.setDiastolic(vitalsData.getDiastolic());
vitals.setPulse(vitalsData.getPulse());
vitals.setSpo2(vitalsData.getSpo2());
vitals.setWeight(vitalsData.getWeight());
vitals.setHeight(vitalsData.getHeight());

// 4. Calculate BMI
if (vitalsData.getWeight() != null && vitalsData.getHeight() != null) {
    BigDecimal heightInMeters = vitalsData.getHeight().divide(
        new BigDecimal("100"),
        2,
        RoundingMode.HALF_UP
    );
    BigDecimal bmi = vitalsData.getWeight().divide(
        heightInMeters.multiply(heightInMeters),
        2,
        RoundingMode.HALF_UP
    );
    vitals.setBmi(bmi);
}

// 5. Set audit fields
vitals.setRecordedAt(LocalDateTime.now());
User recorder = userRepository.findById(userId)
    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
vitals.setRecordedBy(recorder);

return vitalsRepository.save(vitals);
```

**Why This Design?**
- Automatic BMI calculation (reduces errors)
- Upsert pattern (create or update)
- Audit trail (who recorded, when)
- Validates encounter exists

---

#### getVitalsByEncounterId
**Purpose**: Retrieve vitals for an encounter

**Signature**:
```java
public Vitals getVitalsByEncounterId(Long encounterId)
```

**Logic**:
```java
return vitalsRepository.findByEncounterId(encounterId)
    .orElseThrow(() -> new ResourceNotFoundException(
        "Vitals not found for encounter " + encounterId
    ));
```

---

## PrescriptionService

**File**: `PrescriptionService.java`

**Package**: `com.hms.HospitalManagementSystem.service`

### Dependencies
```java
@RequiredArgsConstructor
public class PrescriptionService {
    private final PrescriptionRepository prescriptionRepository;
    private final EncounterRepository encounterRepository;
}
```

### Methods

#### savePrescription
**Purpose**: Create or update prescription with items

**Signature**:
```java
@Transactional
public Prescription savePrescription(
    Long encounterId,
    String note,
    List<PrescriptionItem> items,
    Long userId
)
```

**Logic Flow**:
```java
// 1. Verify encounter exists
Encounter encounter = encounterRepository.findById(encounterId)
    .orElseThrow(() -> new ResourceNotFoundException("Encounter not found"));

// 2. Get or create prescription
Prescription prescription = prescriptionRepository
    .findByEncounterIdAndStatus(encounterId, PrescriptionStatus.DRAFT)
    .orElse(Prescription.builder()
        .encounter(encounter)
        .status(PrescriptionStatus.DRAFT)
        .build());

// 3. Update note
prescription.setNote(note);

// 4. Replace items (clear and add new)
prescription.getItems().clear();
items.forEach(item -> {
    item.setPrescription(prescription);
    prescription.getItems().add(item);
});

return prescriptionRepository.save(prescription);
```

**Why Replace Items?**
- Simplifies client logic (send full list)
- Avoids orphaned items
- Clear ownership (cascade delete)

---

#### getPrescriptionByEncounterId
**Purpose**: Retrieve prescription for encounter

**Signature**:
```java
public Prescription getPrescriptionByEncounterId(Long encounterId)
```

**Logic**:
```java
return prescriptionRepository.findByEncounterId(encounterId)
    .stream()
    .findFirst()
    .orElseThrow(() -> new ResourceNotFoundException(
        "Prescription not found for encounter " + encounterId
    ));
```

---

## Common Patterns

### 1. Exception Handling
```java
// Not Found
.orElseThrow(() -> new ResourceNotFoundException("Entity not found"))

// Business Rule Violation
if (invalidCondition) {
    throw new ConflictException("Business rule violated");
}

// Authorization
if (!hasPermission) {
    throw new AccessDeniedException("Insufficient permissions");
}
```

### 2. Audit Trail
```java
entity.setCreatedAt(LocalDateTime.now());
entity.setUpdatedAt(LocalDateTime.now());
entity.setRecordedBy(currentUser);
```

### 3. State Validation
```java
if (entity.getStatus() != ExpectedStatus) {
    throw new ConflictException("Invalid state transition");
}
```

### 4. Ownership Validation
```java
if (!entity.getOwner().getId().equals(currentUserId)) {
    throw new AccessDeniedException("Not authorized");
}
```

---

## Testing Strategy

### Unit Tests
Mock all dependencies:
```java
@Mock
private EncounterRepository encounterRepository;

@Mock
private AppointmentRepository appointmentRepository;

@InjectMocks
private EncounterService encounterService;
```

Test scenarios:
- Happy path
- Validation failures
- State transitions
- Authorization checks

### Integration Tests
Test with real database:
- Transaction rollback
- Cascade operations
- Foreign key constraints

---

## Performance Considerations

### 1. Lazy Loading
```java
@ManyToOne(fetch = FetchType.LAZY)
private Encounter encounter;
```
- Avoids loading entire object graph
- Fetch explicitly when needed

### 2. Batch Operations
```java
// Save multiple items in one transaction
prescription.getItems().addAll(items);
prescriptionRepository.save(prescription);
```

### 3. Query Optimization
```java
// Use indexed fields
findByStatus(status)  // Uses idx_encounter_status
findByDoctorId(id)    // Uses idx_encounter_doctor
```

---

## Error Handling Best Practices

### 1. Specific Exceptions
```java
throw new ResourceNotFoundException("Encounter not found with ID: " + id);
```
- Clear error messages
- Include context (ID, entity type)

### 2. Early Validation
```java
// Validate at start of method
if (diagnosis == null || diagnosis.trim().isEmpty()) {
    throw new ConflictException("Diagnosis required");
}
```

### 3. Transactional Rollback
```java
@Transactional
public Encounter completeEncounter(...) {
    // Any exception rolls back entire transaction
}
```

---

## Future Enhancements

### 1. Event Publishing
```java
@Transactional
public Encounter completeEncounter(...) {
    // ... existing logic ...
    eventPublisher.publishEvent(new EncounterCompletedEvent(encounter));
    return encounter;
}
```

### 2. Caching
```java
@Cacheable("encounters")
public Encounter getEncounterById(Long id) {
    return encounterRepository.findById(id)
        .orElseThrow(...);
}
```

### 3. Async Processing
```java
@Async
public CompletableFuture<Void> sendPrescriptionNotification(Long encounterId) {
    // Send email/SMS asynchronously
}
```
