# Security & RBAC Documentation

## Overview
The Encounter module implements comprehensive security through:
- JWT-based authentication
- Role-Based Access Control (RBAC)
- Permission-based authorization
- Ownership validation
- Audit trails

---

## Authentication

### JWT Token Structure
```json
{
  "sub": "doctor@hospital.com",
  "roles": ["ROLE_DOCTOR"],
  "permissions": ["CMP_CONSULTATION_WRITE", "CMP_CONSULTATION_READ"],
  "userId": 789,
  "iat": 1707734400,
  "exp": 1707820800
}
```

### Token Validation
- Signature verification using secret key
- Expiration check
- Role and permission extraction

### Getting Current User
```java
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String username = authentication.getName();
User user = userService.getUserByUsername(username);
Long userId = user.getId();
```

---

## Authorization Model

### Permission Hierarchy

```
Module Permissions:
├── CMP_VITALS_READ
├── CMP_VITALS_WRITE
├── CMP_CONSULTATION_READ
├── CMP_CONSULTATION_WRITE
├── CMP_PRESCRIPTION_READ
└── CMP_PRESCRIPTION_WRITE
```

### Role-Permission Mapping

| Role | Permissions |
|------|-------------|
| ROLE_NURSE | CMP_VITALS_READ, CMP_VITALS_WRITE |
| ROLE_DOCTOR | CMP_CONSULTATION_READ, CMP_CONSULTATION_WRITE, CMP_PRESCRIPTION_READ, CMP_PRESCRIPTION_WRITE, CMP_VITALS_READ |
| ROLE_ADMIN | ALL |
| ROLE_RECEPTIONIST | CMP_VITALS_READ, CMP_CONSULTATION_READ |

---

## Controller-Level Security

### @PreAuthorize Annotations

**EncounterController**:
```java
@PostMapping
@PreAuthorize("hasAnyAuthority('CMP_VITALS_WRITE', 'CMP_CONSULTATION_WRITE')")
public ResponseEntity<EncounterResponse> startEncounter(@RequestBody EncounterCreateRequest request)

@PatchMapping("/{id}/clinical-notes")
@PreAuthorize("hasAuthority('CMP_CONSULTATION_WRITE')")
public ResponseEntity<EncounterResponse> updateClinicalNotes(...)

@PatchMapping("/{id}/complete")
@PreAuthorize("hasAuthority('CMP_CONSULTATION_WRITE')")
public ResponseEntity<EncounterResponse> completeEncounter(...)

@GetMapping("/{id}")
@PreAuthorize("hasAuthority('CMP_CONSULTATION_READ')")
public ResponseEntity<EncounterResponse> getEncounterById(...)

@GetMapping("/queue/triage")
@PreAuthorize("hasAuthority('CMP_VITALS_READ')")
public ResponseEntity<List<EncounterResponse>> getTriageQueue()

@GetMapping("/queue/doctor/{doctorId}")
@PreAuthorize("hasAuthority('CMP_CONSULTATION_READ')")
public ResponseEntity<List<EncounterResponse>> getDoctorQueue(...)
```

**VitalsController**:
```java
@PostMapping("/{encounterId}/vitals")
@PreAuthorize("hasAnyAuthority('CMP_VITALS_WRITE', 'CMP_CONSULTATION_WRITE')")
public ResponseEntity<VitalsResponse> saveVitals(...)

@GetMapping("/{encounterId}/vitals")
@PreAuthorize("hasAnyAuthority('CMP_VITALS_READ', 'CMP_CONSULTATION_READ')")
public ResponseEntity<VitalsResponse> getVitals(...)
```

**PrescriptionController**:
```java
@PostMapping("/{encounterId}/prescriptions")
@PreAuthorize("hasAuthority('CMP_PRESCRIPTION_WRITE')")
public ResponseEntity<PrescriptionResponse> savePrescription(...)

@GetMapping("/{encounterId}/prescriptions")
@PreAuthorize("hasAnyAuthority('CMP_PRESCRIPTION_READ', 'CMP_CONSULTATION_READ')")
public ResponseEntity<PrescriptionResponse> getPrescription(...)
```

### Why hasAnyAuthority?
Allows multiple roles to access the same endpoint:
- Nurses can record vitals (`CMP_VITALS_WRITE`)
- Doctors can also record vitals (`CMP_CONSULTATION_WRITE`)

---

## Service-Level Security

### Ownership Validation

**Example from EncounterService**:
```java
@Transactional
public Encounter updateClinicalNotes(Long id, String chiefComplaint, 
                                     String diagnosis, String notes, 
                                     Long currentUserId) {
    Encounter encounter = encounterRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Encounter not found"));
    
    // Ownership check
    if (!encounter.getDoctor().getId().equals(currentUserId)) {
        throw new AccessDeniedException("You can only modify your own encounters");
    }
    
    // ... rest of logic
}
```

**Why Service-Level Checks?**
- Controller checks permissions (CAN user do this?)
- Service checks ownership (SHOULD user do this?)
- Defense in depth

### Authorization Flow

```
1. Request arrives with JWT token
   ↓
2. Spring Security validates token
   ↓
3. Extracts roles and permissions
   ↓
4. @PreAuthorize checks permissions
   ↓
5. Controller calls service
   ↓
6. Service validates ownership
   ↓
7. Operation proceeds or throws AccessDeniedException
```

---

## Security Scenarios

### Scenario 1: Nurse Recording Vitals

**Request**:
```http
POST /api/v1/encounters/50/vitals
Authorization: Bearer <nurse_token>
```

**Token Claims**:
```json
{
  "roles": ["ROLE_NURSE"],
  "permissions": ["CMP_VITALS_WRITE"],
  "userId": 123
}
```

**Authorization Check**:
```java
@PreAuthorize("hasAnyAuthority('CMP_VITALS_WRITE', 'CMP_CONSULTATION_WRITE')")
// ✅ PASS - Nurse has CMP_VITALS_WRITE
```

**Service Logic**:
```java
vitals.setRecordedBy(currentUser); // Audit: Nurse 123 recorded vitals
```

---

### Scenario 2: Doctor Updating Clinical Notes

**Request**:
```http
PATCH /api/v1/encounters/50/clinical-notes
Authorization: Bearer <doctor_token>
```

**Token Claims**:
```json
{
  "roles": ["ROLE_DOCTOR"],
  "permissions": ["CMP_CONSULTATION_WRITE"],
  "userId": 789
}
```

**Authorization Check**:
```java
@PreAuthorize("hasAuthority('CMP_CONSULTATION_WRITE')")
// ✅ PASS - Doctor has CMP_CONSULTATION_WRITE
```

**Ownership Validation**:
```java
if (!encounter.getDoctor().getId().equals(789)) {
    throw new AccessDeniedException(...);
}
// ✅ PASS - Doctor 789 is assigned to this encounter
```

---

### Scenario 3: Doctor Accessing Another Doctor's Encounter

**Request**:
```http
PATCH /api/v1/encounters/50/clinical-notes
Authorization: Bearer <doctor_token>
```

**Token Claims**:
```json
{
  "roles": ["ROLE_DOCTOR"],
  "permissions": ["CMP_CONSULTATION_WRITE"],
  "userId": 999  // Different doctor
}
```

**Authorization Check**:
```java
@PreAuthorize("hasAuthority('CMP_CONSULTATION_WRITE')")
// ✅ PASS - Has permission
```

**Ownership Validation**:
```java
if (!encounter.getDoctor().getId().equals(999)) {
    throw new AccessDeniedException(...);
}
// ❌ FAIL - Doctor 999 is NOT assigned to encounter (assigned to 789)
```

**Response**: `403 Forbidden`

---

### Scenario 4: Receptionist Viewing Queue

**Request**:
```http
GET /api/v1/encounters/queue/triage
Authorization: Bearer <receptionist_token>
```

**Token Claims**:
```json
{
  "roles": ["ROLE_RECEPTIONIST"],
  "permissions": ["CMP_VITALS_READ"],
  "userId": 456
}
```

**Authorization Check**:
```java
@PreAuthorize("hasAuthority('CMP_VITALS_READ')")
// ✅ PASS - Receptionist has CMP_VITALS_READ
```

**Service Logic**:
```java
return encounterRepository.findByStatus(EncounterStatus.TRIAGE);
// ✅ Returns all triage encounters (no ownership check for queue views)
```

---

## Audit Trail

### Database Audit Fields

**encounters table**:
- `created_at`: When encounter was created
- `updated_at`: Last modification time
- `started_at`: When encounter began
- `completed_at`: When encounter finished

**vitals table**:
- `recorded_at`: When vitals were taken
- `recorded_by`: User ID who recorded vitals
- `created_at`: Record creation
- `updated_at`: Last modification

**prescriptions table**:
- `issued_at`: When prescription was issued
- `created_at`: Record creation
- `updated_at`: Last modification

### Audit Query Examples

**Who recorded vitals?**
```sql
SELECT v.*, u.full_name as recorded_by_name
FROM vitals v
JOIN users u ON u.id = v.recorded_by
WHERE v.encounter_id = 50;
```

**When was encounter completed?**
```sql
SELECT e.id, e.completed_at, u.full_name as doctor_name
FROM encounters e
JOIN users u ON u.id = e.doctor_id
WHERE e.id = 50;
```

**Prescription issuance history**:
```sql
SELECT p.*, e.completed_at, u.full_name as doctor_name
FROM prescriptions p
JOIN encounters e ON e.id = p.encounter_id
JOIN users u ON u.id = e.doctor_id
WHERE p.status = 'ISSUED'
ORDER BY p.issued_at DESC;
```

---

## Security Best Practices

### 1. Never Trust Client Input
```java
// ❌ BAD - Using userId from request body
public void updateEncounter(Long id, Long userId) {
    // Attacker could send any userId
}

// ✅ GOOD - Get userId from authenticated context
public void updateEncounter(Long id) {
    Long userId = getCurrentUserId(); // From JWT token
}
```

### 2. Validate at Multiple Layers
```java
// Layer 1: Controller - Permission check
@PreAuthorize("hasAuthority('CMP_CONSULTATION_WRITE')")

// Layer 2: Service - Ownership check
if (!encounter.getDoctor().getId().equals(currentUserId)) {
    throw new AccessDeniedException(...);
}

// Layer 3: Database - Foreign key constraints
```

### 3. Fail Securely
```java
// ❌ BAD - Reveals information
if (encounter == null) {
    throw new ResourceNotFoundException("Encounter 50 does not exist");
}

// ✅ GOOD - Generic message for unauthorized access
if (encounter == null || !encounter.getDoctor().getId().equals(userId)) {
    throw new ResourceNotFoundException("Encounter not found");
}
```

### 4. Use Parameterized Queries
```java
// ✅ Spring Data JPA uses parameterized queries by default
encounterRepository.findById(id);  // Safe from SQL injection
```

---

## Common Security Vulnerabilities & Mitigations

### 1. Insecure Direct Object Reference (IDOR)

**Vulnerability**:
```java
// User can access any encounter by changing ID in URL
GET /api/v1/encounters/999
```

**Mitigation**:
```java
// Service validates ownership
if (!encounter.getDoctor().getId().equals(currentUserId)) {
    throw new AccessDeniedException(...);
}
```

### 2. Privilege Escalation

**Vulnerability**:
```java
// Nurse tries to complete encounter (doctor-only action)
PATCH /api/v1/encounters/50/complete
```

**Mitigation**:
```java
@PreAuthorize("hasAuthority('CMP_CONSULTATION_WRITE')")
// Only doctors have this permission
```

### 3. Mass Assignment

**Vulnerability**:
```java
// Attacker sends extra fields in request
{
  "diagnosis": "Flu",
  "status": "COMPLETED"  // Trying to bypass workflow
}
```

**Mitigation**:
```java
// Use DTOs with only allowed fields
public class EncounterUpdateRequest {
    private String chiefComplaint;
    private String diagnosis;
    private String notes;
    // status NOT included - controlled by service logic
}
```

---

## Security Configuration

### Spring Security Config (Reference)
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### Method Security Enabled
```java
@EnableMethodSecurity(prePostEnabled = true)
```
- Enables `@PreAuthorize` annotations
- Evaluates SpEL expressions
- Throws `AccessDeniedException` on failure

---

## Testing Security

### Unit Tests
```java
@Test
void updateClinicalNotes_Fails_WhenNotOwner() {
    when(encounterRepository.findById(50L)).thenReturn(Optional.of(encounter));
    
    assertThrows(AccessDeniedException.class,
        () -> encounterService.updateClinicalNotes(50L, "complaint", "diagnosis", "notes", 999L)
    );
}
```

### Integration Tests
```java
@Test
@WithMockUser(authorities = "CMP_VITALS_READ")
void getTriageQueue_Success_WithVitalsReadPermission() {
    mockMvc.perform(get("/api/v1/encounters/queue/triage"))
        .andExpect(status().isOk());
}

@Test
@WithMockUser(authorities = "ROLE_PATIENT")
void getTriageQueue_Forbidden_WithoutPermission() {
    mockMvc.perform(get("/api/v1/encounters/queue/triage"))
        .andExpect(status().isForbidden());
}
```

---

## Compliance Considerations

### HIPAA Requirements
- **Access Control**: Implemented via RBAC
- **Audit Trails**: All modifications logged
- **Encryption**: HTTPS in transit, database encryption at rest
- **Minimum Necessary**: Users only access data needed for their role

### Data Privacy
- Patient data only accessible to:
  - Assigned doctor
  - Nurses in triage workflow
  - Admins (for system management)
- No bulk export without authorization
- Audit log of all access

---

## Future Security Enhancements

### 1. Row-Level Security
```sql
CREATE POLICY encounter_access ON encounters
FOR SELECT
USING (
    doctor_id = current_user_id() OR
    current_user_has_role('ADMIN')
);
```

### 2. Rate Limiting
```java
@RateLimiter(name = "encounterApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> startEncounter(...) {
    // ...
}
```

### 3. Encryption at Rest
```java
@Convert(converter = EncryptedStringConverter.class)
private String diagnosis;
```

### 4. Multi-Factor Authentication
- Require MFA for sensitive operations
- Complete encounter
- Issue prescriptions

### 5. IP Whitelisting
- Restrict API access to hospital network
- VPN required for remote access
