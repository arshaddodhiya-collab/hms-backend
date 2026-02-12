# Testing Guide

## Overview
This guide covers testing strategies for the Encounter module, including unit tests, integration tests, and end-to-end testing.

---

## Testing Pyramid

```
        /\
       /  \
      / E2E \
     /______\
    /        \
   /Integration\
  /____________\
 /              \
/   Unit Tests   \
/________________\
```

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test API endpoints with database
- **E2E Tests**: Test complete user workflows

---

## Unit Testing

### Service Layer Tests

**File**: `EncounterServiceTest.java`

```java
package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.entity.*;
import com.hms.HospitalManagementSystem.enums.*;
import com.hms.HospitalManagementSystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncounterServiceTest {

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EncounterService encounterService;

    private User doctor;
    private Patient patient;
    private Appointment appointment;
    private Encounter encounter;

    @BeforeEach
    void setUp() {
        // Setup test data
        doctor = new User();
        doctor.setId(1L);
        doctor.setFullName("Dr. Smith");

        patient = new Patient();
        patient.setId(100L);
        patient.setFirstName("John Doe");

        appointment = new Appointment();
        appointment.setId(10L);
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        encounter = new Encounter();
        encounter.setId(50L);
        encounter.setAppointment(appointment);
        encounter.setPatient(patient);
        encounter.setDoctor(doctor);
        encounter.setStatus(EncounterStatus.IN_PROGRESS);
        encounter.setDiagnosis("Flu");
    }

    @Test
    void startEncounter_CreatesNew_WhenNotExists() {
        // Arrange
        when(encounterRepository.findByAppointmentId(10L)).thenReturn(Optional.empty());
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(invocation -> {
            Encounter saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        // Act
        Encounter result = encounterService.startEncounter(10L, 100L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(50L, result.getId());
        verify(encounterRepository).save(any(Encounter.class));
    }

    @Test
    void startEncounter_ReturnsExisting_WhenExists() {
        // Arrange
        when(encounterRepository.findByAppointmentId(10L)).thenReturn(Optional.of(encounter));

        // Act
        Encounter result = encounterService.startEncounter(10L, 100L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(50L, result.getId());
        verify(encounterRepository, never()).save(any(Encounter.class));
    }

    @Test
    void completeEncounter_Success() {
        // Arrange
        when(encounterRepository.findById(50L)).thenReturn(Optional.of(encounter));
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

        // Act
        Encounter result = encounterService.completeEncounter(50L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(EncounterStatus.COMPLETED, result.getStatus());
        assertEquals(AppointmentStatus.COMPLETED, result.getAppointment().getStatus());
        assertNotNull(result.getCompletedAt());
        verify(encounterRepository).save(encounter);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void completeEncounter_Fails_WhenNotOwner() {
        // Arrange
        when(encounterRepository.findById(50L)).thenReturn(Optional.of(encounter));

        // Act & Assert
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> encounterService.completeEncounter(50L, 2L)); // Different user
    }

    @Test
    void completeEncounter_Fails_WhenNoDiagnosis() {
        // Arrange
        encounter.setDiagnosis(null);
        when(encounterRepository.findById(50L)).thenReturn(Optional.of(encounter));

        // Act & Assert
        assertThrows(com.hms.HospitalManagementSystem.exception.ConflictException.class,
                () -> encounterService.completeEncounter(50L, 1L));
    }
}
```

### Running Unit Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=EncounterServiceTest

# Run specific test method
./mvnw test -Dtest=EncounterServiceTest#completeEncounter_Success

# Run with coverage
./mvnw test jacoco:report
```

---

## Integration Testing

### Controller Tests

**File**: `EncounterControllerIntegrationTest.java`

```java
package com.hms.HospitalManagementSystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.HospitalManagementSystem.dto.request.EncounterCreateRequest;
import com.hms.HospitalManagementSystem.entity.*;
import com.hms.HospitalManagementSystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EncounterControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    private Appointment appointment;
    private Patient patient;
    private User doctor;

    @BeforeEach
    void setUp() {
        // Create test data
        patient = new Patient();
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient = patientRepository.save(patient);

        doctor = new User();
        doctor.setUsername("doctor@test.com");
        doctor.setFullName("Dr. Smith");
        doctor = userRepository.save(doctor);

        appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment = appointmentRepository.save(appointment);
    }

    @Test
    @WithMockUser(authorities = "CMP_CONSULTATION_WRITE")
    void startEncounter_Success() throws Exception {
        EncounterCreateRequest request = new EncounterCreateRequest();
        request.setAppointmentId(appointment.getId());
        request.setPatientId(patient.getId());
        request.setDoctorId(doctor.getId());

        mockMvc.perform(post("/api/v1/encounters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("TRIAGE"))
                .andExpect(jsonPath("$.appointmentId").value(appointment.getId()));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PATIENT")
    void startEncounter_Forbidden_WithoutPermission() throws Exception {
        EncounterCreateRequest request = new EncounterCreateRequest();
        request.setAppointmentId(appointment.getId());
        request.setPatientId(patient.getId());
        request.setDoctorId(doctor.getId());

        mockMvc.perform(post("/api/v1/encounters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CMP_VITALS_READ")
    void getTriageQueue_Success() throws Exception {
        mockMvc.perform(get("/api/v1/encounters/queue/triage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
```

### Database Tests

**Test Configuration**: `application-test.properties`

```properties
# Use H2 in-memory database for tests
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# Disable Flyway for tests
spring.flyway.enabled=false
```

### Running Integration Tests

```bash
# Run integration tests
./mvnw verify

# Run with specific profile
./mvnw test -Dspring.profiles.active=test
```

---

## Test Data Builders

### EncounterTestBuilder

```java
public class EncounterTestBuilder {
    private Long id = 1L;
    private Appointment appointment;
    private Patient patient;
    private User doctor;
    private EncounterStatus status = EncounterStatus.TRIAGE;
    private String diagnosis;

    public EncounterTestBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public EncounterTestBuilder withAppointment(Appointment appointment) {
        this.appointment = appointment;
        return this;
    }

    public EncounterTestBuilder withStatus(EncounterStatus status) {
        this.status = status;
        return this;
    }

    public EncounterTestBuilder withDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
        return this;
    }

    public Encounter build() {
        Encounter encounter = new Encounter();
        encounter.setId(id);
        encounter.setAppointment(appointment);
        encounter.setPatient(patient);
        encounter.setDoctor(doctor);
        encounter.setStatus(status);
        encounter.setDiagnosis(diagnosis);
        return encounter;
    }
}

// Usage
Encounter encounter = new EncounterTestBuilder()
    .withId(50L)
    .withStatus(EncounterStatus.IN_PROGRESS)
    .withDiagnosis("Flu")
    .build();
```

---

## Mocking Strategies

### 1. Mock External Dependencies

```java
@Mock
private EncounterRepository encounterRepository;

@Mock
private AppointmentRepository appointmentRepository;

@InjectMocks
private EncounterService encounterService;
```

### 2. Stub Method Calls

```java
when(encounterRepository.findById(50L)).thenReturn(Optional.of(encounter));
```

### 3. Verify Interactions

```java
verify(encounterRepository).save(any(Encounter.class));
verify(encounterRepository, times(1)).save(encounter);
verify(encounterRepository, never()).delete(any());
```

### 4. Argument Captors

```java
@Captor
private ArgumentCaptor<Encounter> encounterCaptor;

// In test
verify(encounterRepository).save(encounterCaptor.capture());
Encounter savedEncounter = encounterCaptor.getValue();
assertEquals(EncounterStatus.COMPLETED, savedEncounter.getStatus());
```

---

## Test Coverage

### Coverage Goals
- **Unit Tests**: 80%+ code coverage
- **Integration Tests**: All API endpoints
- **E2E Tests**: Critical user workflows

### Measuring Coverage

```bash
# Generate coverage report
./mvnw clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

### Coverage Report Example

```
Package                          Coverage
com.hms.service                  85%
com.hms.controller               78%
com.hms.repository               100% (interfaces)
Overall                          82%
```

---

## Testing Best Practices

### 1. AAA Pattern (Arrange-Act-Assert)

```java
@Test
void completeEncounter_Success() {
    // Arrange
    when(encounterRepository.findById(50L)).thenReturn(Optional.of(encounter));
    
    // Act
    Encounter result = encounterService.completeEncounter(50L, 1L);
    
    // Assert
    assertEquals(EncounterStatus.COMPLETED, result.getStatus());
}
```

### 2. Test One Thing

```java
// ❌ BAD - Tests multiple things
@Test
void encounterWorkflow() {
    encounterService.startEncounter(...);
    encounterService.updateClinicalNotes(...);
    encounterService.completeEncounter(...);
}

// ✅ GOOD - Separate tests
@Test
void startEncounter_Success() { ... }

@Test
void updateClinicalNotes_Success() { ... }

@Test
void completeEncounter_Success() { ... }
```

### 3. Descriptive Test Names

```java
// ❌ BAD
@Test
void test1() { ... }

// ✅ GOOD
@Test
void completeEncounter_Fails_WhenNoDiagnosis() { ... }
```

### 4. Use Test Fixtures

```java
@BeforeEach
void setUp() {
    // Common test data setup
    doctor = createTestDoctor();
    patient = createTestPatient();
    encounter = createTestEncounter();
}
```

### 5. Clean Up After Tests

```java
@AfterEach
void tearDown() {
    // Clean up resources if needed
}
```

---

## Continuous Integration

### GitHub Actions Example

```yaml
name: Backend Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up JDK 17
      uses: actions/setup-java@v2
      with:
        java-version: '17'
        distribution: 'adopt'
    
    - name: Run tests
      run: ./mvnw clean test
    
    - name: Generate coverage report
      run: ./mvnw jacoco:report
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v2
```

---

## Performance Testing

### Load Testing with JMeter

**Test Plan**: `encounter-load-test.jmx`

- **Thread Group**: 100 concurrent users
- **Ramp-up**: 10 seconds
- **Loop Count**: 10

**Endpoints to Test**:
1. Start Encounter
2. Save Vitals
3. Update Clinical Notes
4. Complete Encounter

**Metrics**:
- Response time (avg, p95, p99)
- Throughput (requests/sec)
- Error rate

### Running Load Tests

```bash
jmeter -n -t encounter-load-test.jmx -l results.jtl -e -o report/
```

---

## Test Documentation

### Test Case Template

```markdown
**Test Case ID**: TC_ENC_001
**Test Name**: Complete Encounter with Valid Diagnosis
**Preconditions**: 
- Encounter exists with status IN_PROGRESS
- User is the assigned doctor
- Diagnosis is provided

**Steps**:
1. Call completeEncounter(encounterId, doctorId)

**Expected Result**:
- Encounter status changes to COMPLETED
- completedAt timestamp is set
- Appointment status changes to COMPLETED
- All prescriptions status changes to ISSUED

**Actual Result**: PASS
```

---

## Debugging Tests

### Enable Debug Logging

```properties
# application-test.properties
logging.level.com.hms=DEBUG
logging.level.org.springframework.test=DEBUG
```

### Print Test Data

```java
@Test
void debugTest() {
    System.out.println("Encounter: " + encounter);
    System.out.println("Doctor ID: " + doctor.getId());
    
    // Run test
    Encounter result = encounterService.completeEncounter(50L, 1L);
    
    System.out.println("Result: " + result);
}
```

### Use Breakpoints

- Set breakpoints in service methods
- Run tests in debug mode
- Inspect variables and call stack

---

## Summary

### Test Checklist

- [ ] Unit tests for all service methods
- [ ] Integration tests for all API endpoints
- [ ] Test happy paths
- [ ] Test error scenarios
- [ ] Test authorization checks
- [ ] Test data validation
- [ ] Test state transitions
- [ ] Achieve 80%+ code coverage
- [ ] All tests pass in CI/CD pipeline
