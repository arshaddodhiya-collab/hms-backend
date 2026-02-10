# Security & Permissions

This document details the security architecture, permissions, and best practices for the Patient module.

## 1. Security Architecture

### Overview

The Patient module uses **Spring Security** with **JWT-based authentication** and **role-based access control (RBAC)** with fine-grained permissions.

```
┌─────────────────────────────────────────────────────────┐
│                    HTTP Request                          │
│            Authorization: Bearer <JWT>                   │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              JwtAuthenticationFilter                     │
│  1. Extract JWT from Authorization header               │
│  2. Validate JWT signature and expiration               │
│  3. Extract username and authorities                    │
│  4. Set SecurityContext                                 │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              @PreAuthorize Check                         │
│  - Verify user has required permission                  │
│  - Example: hasAuthority('CMP_PATIENT_READ')            │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              Controller Method                           │
│  - Execute business logic                               │
│  - Return response                                      │
└─────────────────────────────────────────────────────────┘
```

---

## 2. Permissions

### Patient Module Permissions

| Permission | Description | Operations |
|------------|-------------|------------|
| `CMP_PATIENT_READ` | View patient information | GET /patients, GET /patients/{id} |
| `CMP_PATIENT_WRITE` | Create and update patients | POST /patients, PUT /patients/{id} |
| `CMP_PATIENT_DELETE` | Delete patients | DELETE /patients/{id} |

### Permission Naming Convention

- **Prefix**: `CMP_` (Component)
- **Module**: `PATIENT`
- **Action**: `READ`, `WRITE`, `DELETE`

---

## 3. Role Assignments

### Default Role-Permission Mapping

| Role | Permissions | Use Case |
|------|-------------|----------|
| `ADMIN` | All patient permissions | System administrators |
| `DOCTOR` | `CMP_PATIENT_READ`, `CMP_PATIENT_WRITE` | Medical staff |
| `NURSE` | `CMP_PATIENT_READ`, `CMP_PATIENT_WRITE` | Nursing staff |
| `RECEPTIONIST` | `CMP_PATIENT_READ`, `CMP_PATIENT_WRITE` | Front desk staff |
| `LAB_TECHNICIAN` | `CMP_PATIENT_READ` | Lab staff (read-only) |
| `PHARMACIST` | `CMP_PATIENT_READ` | Pharmacy staff (read-only) |

### Database Schema

```sql
-- Roles table
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Permissions table
CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Role-Permission mapping
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id),
    FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

-- User-Role mapping
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);
```

---

## 4. Controller Security Annotations

### PatientController

```java
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @PreAuthorize("hasAuthority('CMP_PATIENT_WRITE')")
    public ResponseEntity<PatientResponse> registerPatient(
            @RequestBody PatientRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(patientService.registerPatient(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CMP_PATIENT_READ')")
    public ResponseEntity<Page<PatientResponse>> searchPatients(
            @RequestParam(required = false) String query,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        return ResponseEntity.ok(patientService.searchPatients(query, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CMP_PATIENT_READ')")
    public ResponseEntity<PatientDetailsResponse> getPatientDetails(
            @PathVariable Long id) {
        return ResponseEntity.ok(patientService.getPatientDetails(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CMP_PATIENT_WRITE')")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable Long id,
            @RequestBody PatientUpdateRequest request) {
        return ResponseEntity.ok(patientService.updatePatient(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CMP_PATIENT_DELETE')")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Annotation Breakdown

- `@PreAuthorize("hasAuthority('CMP_PATIENT_READ')")`: Requires user to have `CMP_PATIENT_READ` permission
- `@PreAuthorize("hasAuthority('CMP_PATIENT_WRITE')")`: Requires user to have `CMP_PATIENT_WRITE` permission
- `@PreAuthorize("hasAuthority('CMP_PATIENT_DELETE')")`: Requires user to have `CMP_PATIENT_DELETE` permission

---

## 5. JWT Token Structure

### Token Payload

```json
{
  "sub": "admin",
  "authorities": [
    "CMP_PATIENT_READ",
    "CMP_PATIENT_WRITE",
    "CMP_PATIENT_DELETE",
    "CMP_ADMIN_DEPT_READ",
    "CMP_ADMIN_USER_WRITE"
  ],
  "iat": 1707566400,
  "exp": 1707652800
}
```

### Token Fields

| Field | Description |
|-------|-------------|
| `sub` | Subject (username) |
| `authorities` | List of permissions |
| `iat` | Issued at timestamp |
| `exp` | Expiration timestamp |

### Token Validation

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) {
        String token = extractToken(request);
        
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsername(token);
            List<String> authorities = jwtTokenProvider.getAuthorities(token);
            
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    username, null, 
                    authorities.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList())
                );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## 6. Security Best Practices

### 6.1 Input Validation

**Always validate user input** to prevent injection attacks:

```java
@Service
public class PatientService {
    
    public PatientResponse registerPatient(PatientRegisterRequest request) {
        // Validate required fields
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        
        // Sanitize input
        String sanitizedContact = request.getContact().replaceAll("[^0-9]", "");
        
        // Validate format
        if (!sanitizedContact.matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("Invalid contact number format");
        }
        
        // Continue with registration
    }
}
```

### 6.2 SQL Injection Prevention

**Use JPA/Hibernate** with parameterized queries:

```java
// GOOD: Parameterized query
@Query("SELECT p FROM Patient p WHERE p.firstName = :firstName " +
       "AND p.lastName = :lastName AND p.dob = :dob AND p.contact = :contact")
Optional<Patient> findPotentialDuplicate(
    @Param("firstName") String firstName,
    @Param("lastName") String lastName,
    @Param("dob") LocalDate dob,
    @Param("contact") String contact
);

// BAD: String concatenation (vulnerable to SQL injection)
// String query = "SELECT * FROM patients WHERE name = '" + name + "'";
```

### 6.3 Sensitive Data Protection

**Never expose sensitive data** in responses:

```java
// GOOD: Separate DTOs for request/response
@Data
public class PatientResponse {
    private Long id;
    private String firstName;
    private String lastName;
    // ... other fields
    // NO password or sensitive fields
}

// BAD: Exposing entity directly
// return patientRepository.findById(id); // May expose internal fields
```

### 6.4 Optimistic Locking

**Prevent concurrent update conflicts**:

```java
@Entity
public class Patient extends PublicEntity {
    @Version
    private Long version;
    // ... other fields
}

// Service method
public PatientResponse updatePatient(Long id, PatientUpdateRequest request) {
    Patient patient = patientRepository.findById(id).orElseThrow();
    
    // Check version
    if (request.getVersion() != null && patient.getVersion() != null
            && !request.getVersion().equals(patient.getVersion())) {
        throw new RuntimeException("Patient record has been modified. Please refresh.");
    }
    
    // Update and save
}
```

### 6.5 Soft Delete

**Preserve data for audit purposes**:

```java
public void deletePatient(Long id) {
    Patient patient = patientRepository.findById(id).orElseThrow();
    
    // Soft delete (don't actually remove from database)
    patient.setDeleted(true);
    patient.setActive(false);
    patientRepository.save(patient);
    
    // Hard delete would be: patientRepository.delete(patient);
}
```

### 6.6 Audit Logging

**Track all data modifications**:

```java
@Service
@Slf4j
public class PatientService {
    
    public PatientResponse registerPatient(PatientRegisterRequest request) {
        log.info("Registering new patient: {} {}", 
                 request.getFirstName(), request.getLastName());
        
        Patient patient = patientMapper.toEntity(request);
        Patient savedPatient = patientRepository.save(patient);
        
        log.info("Patient registered successfully with ID: {}", savedPatient.getId());
        
        return patientMapper.toResponse(savedPatient);
    }
    
    public void deletePatient(Long id) {
        log.warn("Deleting patient with ID: {}", id);
        // ... deletion logic
        log.info("Patient deleted successfully: {}", id);
    }
}
```

---

## 7. CORS Configuration

### Allow Frontend Access

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

---

## 8. Error Handling

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("Access Denied")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
```

---

## 9. Production Recommendations

### 9.1 HTTPS Only

```yaml
# application-prod.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

### 9.2 Token Expiration

```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET}
  expiration: 3600000  # 1 hour in milliseconds
```

### 9.3 Rate Limiting

```java
@Configuration
public class RateLimitConfig {
    
    @Bean
    public RateLimiter rateLimiter() {
        return RateLimiter.create(100.0); // 100 requests per second
    }
}
```

### 9.4 Database Encryption

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hms?useSSL=true&requireSSL=true
```

### 9.5 Secrets Management

**Never hardcode secrets**:

```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET}  # From environment variable

spring:
  datasource:
    password: ${DB_PASSWORD}  # From environment variable
```

---

## 10. Security Checklist

- [ ] All endpoints require authentication
- [ ] Appropriate permissions assigned to each endpoint
- [ ] Input validation implemented
- [ ] SQL injection prevention (parameterized queries)
- [ ] Sensitive data not exposed in responses
- [ ] Optimistic locking for concurrent updates
- [ ] Soft delete for data preservation
- [ ] Audit logging for all operations
- [ ] CORS configured for frontend
- [ ] Global exception handling
- [ ] HTTPS enabled in production
- [ ] Token expiration configured
- [ ] Rate limiting implemented
- [ ] Database encryption enabled
- [ ] Secrets managed via environment variables

---

## 11. Related Documentation

- [Patient Module Overview](./01_PATIENT_MODULE_OVERVIEW.md)
- [Patient Service & Endpoints](./02_PATIENT_SERVICE_AND_ENDPOINTS.md)
- [API Testing Guide](./05_API_TESTING_GUIDE.md)
- [Authentication Flow](../auth_docs/02_AUTHENTICATION_FLOW.md)
- [JWT Implementation](../auth_docs/04_JWT_IMPLEMENTATION.md)
- [Backend Best Practices](../BEST_PRACTICES_BACKEND.md)
