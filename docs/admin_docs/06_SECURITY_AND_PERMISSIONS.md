# Security & Permissions

This document details the security model, permissions, and authorization mechanisms for the Admin Module.

## 1. Security Architecture

The Admin Module uses **Spring Security** with **JWT-based authentication** and **role-based access control (RBAC)** with fine-grained permissions.

### Authentication Flow
1. User logs in via `/api/v1/auth/login`
2. Server validates credentials
3. JWT access token is generated with user's roles and permissions
4. Client includes token in `Authorization: Bearer <token>` header
5. `JwtAuthenticationFilter` validates token and sets `SecurityContext`
6. `@PreAuthorize` annotations check permissions before method execution

### Authorization Model
```
User → Roles → Permissions
```
- Users can have multiple roles
- Roles contain multiple permissions
- Permissions are checked at the endpoint level

## 2. Admin Module Permissions

### Department Permissions

| Permission Code | Description | Operations |
|----------------|-------------|------------|
| `CMP_ADMIN_DEPT_READ` | View departments | GET /departments, GET /departments/{id} |
| `CMP_ADMIN_DEPT_WRITE` | Create/Update departments | POST /departments, PUT /departments/{id} |
| `CMP_ADMIN_DEPT_DELETE` | Delete departments | DELETE /departments/{id} |

### User Permissions

| Permission Code | Description | Operations |
|----------------|-------------|------------|
| `CMP_ADMIN_USER_READ` | View users | GET /users, GET /users/{id} |
| `CMP_ADMIN_USER_WRITE` | Create/Update users | POST /users, PUT /users/{id} |

## 3. Permission Assignment

### Default Role Configuration

**ADMIN Role**:
```sql
-- Has all admin permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN' 
AND p.code IN (
    'CMP_ADMIN_DEPT_READ', 
    'CMP_ADMIN_DEPT_WRITE', 
    'CMP_ADMIN_DEPT_DELETE',
    'CMP_ADMIN_USER_READ',
    'CMP_ADMIN_USER_WRITE'
);
```

**Other Roles**:
- `DOCTOR`, `NURSE`, `RECEPTION`: No admin permissions by default
- Can be granted specific permissions as needed

### Custom Permission Assignment

To grant admin permissions to other roles:

```sql
-- Grant department read permission to DOCTOR role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'DOCTOR' AND p.code = 'CMP_ADMIN_DEPT_READ';

-- Grant user read permission to RECEPTION role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'RECEPTION' AND p.code = 'CMP_ADMIN_USER_READ';
```

## 4. Endpoint Security

### Department Controller

```java
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    @GetMapping
    @PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_READ')")
    public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
        // Only users with CMP_ADMIN_DEPT_READ can access
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_WRITE')")
    public ResponseEntity<DepartmentDto> createDepartment(@RequestBody DepartmentDto dto) {
        // Only users with CMP_ADMIN_DEPT_WRITE can access
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_DELETE')")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        // Only users with CMP_ADMIN_DEPT_DELETE can access
    }
}
```

### User Controller

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    @GetMapping
    @PreAuthorize("hasAuthority('CMP_ADMIN_USER_READ')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        // Only users with CMP_ADMIN_USER_READ can access
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CMP_ADMIN_USER_WRITE')")
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto dto) {
        // Only users with CMP_ADMIN_USER_WRITE can access
    }
}
```

## 5. Permission Checking

### How @PreAuthorize Works

1. **Method Invocation**: Client calls protected endpoint
2. **Filter Chain**: Request passes through `JwtAuthenticationFilter`
3. **Token Validation**: JWT is validated and user authorities are loaded
4. **Security Context**: `Authentication` object is set with user's permissions
5. **Authorization Check**: `@PreAuthorize` evaluates expression
6. **Access Decision**: 
   - If user has required authority → Method executes
   - If user lacks authority → `AccessDeniedException` thrown (403 Forbidden)

### Expression Examples

```java
// Single permission
@PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_READ')")

// Multiple permissions (OR)
@PreAuthorize("hasAnyAuthority('CMP_ADMIN_DEPT_READ', 'CMP_ADMIN_DEPT_WRITE')")

// Multiple permissions (AND)
@PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_READ') and hasAuthority('CMP_ADMIN_USER_READ')")

// Role-based (less granular)
@PreAuthorize("hasRole('ADMIN')")

// Complex expressions
@PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_WRITE') or hasRole('ADMIN')")
```

## 6. Security Best Practices

### Password Security
- **Encoding**: BCrypt with strength 10
- **Storage**: Only hashed passwords in database
- **Transmission**: HTTPS recommended for production
- **Never Return**: Passwords never included in API responses

### Token Security
- **Access Token**: Short-lived (15 minutes default)
- **Refresh Token**: Long-lived (7 days default), stored in database
- **Revocation**: Refresh tokens can be revoked via logout
- **Storage**: Client should store tokens securely (httpOnly cookies recommended)

### Permission Granularity
- **Fine-grained**: Use specific permissions (e.g., `CMP_ADMIN_DEPT_READ`)
- **Avoid Wildcards**: Don't use broad permissions like `ADMIN_ALL`
- **Principle of Least Privilege**: Grant minimum required permissions
- **Regular Audits**: Review and update permission assignments

### Data Validation
- **Input Validation**: Validate all request data
- **SQL Injection**: Use parameterized queries (JPA handles this)
- **XSS Prevention**: Sanitize user input (especially descriptions)
- **CSRF**: Disabled for stateless JWT (no session cookies)

## 7. Error Handling

### Global Exception Handler

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Forbidden");
        body.put("message", "Access Denied");
        body.put("path", request.getDescription(false));
        
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", "Database error: " + ex.getMostSpecificCause().getMessage());
        body.put("path", request.getDescription(false));
        
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }
}
```

### Common Error Responses

**401 Unauthorized**: Missing or invalid JWT token
```json
{
  "timestamp": "2026-02-10T12:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required",
  "path": "/api/v1/departments"
}
```

**403 Forbidden**: Valid token but insufficient permissions
```json
{
  "timestamp": "2026-02-10T12:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/v1/departments"
}
```

**409 Conflict**: Data integrity violation
```json
{
  "timestamp": "2026-02-10T12:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Database error: Duplicate entry 'Cardiology' for key 'departments.name'",
  "path": "/api/v1/departments"
}
```

## 8. Security Configuration

### CORS Configuration

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### Session Management

```java
http.sessionManagement()
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
```
- No server-side sessions
- All state in JWT token
- Scalable and stateless

## 9. Audit & Logging

### Recommended Logging

```java
@Service
@Slf4j
public class DepartmentService {
    
    public DepartmentDto createDepartment(DepartmentDto dto) {
        log.info("Creating department: {}", dto.getName());
        // ... creation logic
        log.info("Department created successfully: ID={}", saved.getId());
        return mapToDto(saved);
    }
    
    public void deleteDepartment(Long id) {
        log.warn("Deleting department: ID={}", id);
        // ... deletion logic
        log.info("Department deleted: ID={}", id);
    }
}
```

### Security Events to Log
- Failed authentication attempts
- Permission denied errors (403)
- User creation/updates
- Department creation/deletion
- Role/permission changes
- Unusual access patterns

## 10. Production Recommendations

### Environment-Specific Settings

**Development**:
- Detailed error messages
- Debug logging enabled
- CORS allows localhost

**Production**:
- Generic error messages (don't expose stack traces)
- Info/Warn logging only
- CORS restricted to production domain
- HTTPS enforced
- Secure cookie flags enabled
- Rate limiting on sensitive endpoints
- Database connection pooling
- Regular security audits

### Secrets Management
- Store JWT secret in environment variables
- Use strong, randomly generated secrets (256+ bits)
- Rotate secrets periodically
- Never commit secrets to version control

### Monitoring
- Track failed authentication attempts
- Monitor for unusual permission denial patterns
- Alert on repeated 403 errors from same user
- Log all admin operations for audit trail
