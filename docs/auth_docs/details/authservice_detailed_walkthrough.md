# AuthService - Detailed Code Walkthrough

This document provides a line-by-line explanation of the [`AuthService.java`](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/AuthService.java) file.

## Class Overview

```java
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthService {
```

**Annotations:**
- `@Service` - Marks this as a Spring service component
- `@Transactional` - All methods run in database transactions
- `@RequiredArgsConstructor` - Lombok generates constructor for `final` fields
- `@Slf4j` - Lombok provides logging via `log` variable

---

## Dependencies (Lines 38-46)

```java
private final UserRepository userRepository;
private final RoleRepository roleRepository;
private final DepartmentRepository departmentRepository;
private final RefreshTokenRepository refreshTokenRepository;

private final PasswordEncoder passwordEncoder;
private final JwtService jwtService;
private final AuthenticationManager authenticationManager;
private final CustomUserDetailsService userDetailsService;
```

**Injected via Constructor:**
- **Repositories** - Database access for users, roles, departments, and refresh tokens
- **PasswordEncoder** - BCrypt password hashing (configured in `SecurityConfig`)
- **JwtService** - JWT token generation and validation
- **AuthenticationManager** - Spring Security authentication
- **CustomUserDetailsService** - Loads user details for authentication

---

## Configuration Properties (Lines 48-55)

```java
@Value("${jwt.expiration}")
private long jwtExpiration;  // 900000 ms = 15 minutes

@Value("${jwt.refresh-expiration}")
private long refreshExpiration;  // 604800000 ms = 7 days

@Value("${app.security.secure-cookie}")
private boolean secureCookie;  // false in dev, true in production
```

**Loaded from `application.properties`:**
- `jwtExpiration` - Access token lifetime
- `refreshExpiration` - Refresh token lifetime
- `secureCookie` - Whether cookies require HTTPS

---

## Method: `register()` (Lines 57-97)

### Purpose
Creates a new user account and automatically logs them in.

### Code Walkthrough

#### Step 1: Check Username Availability (Lines 58-60)
```java
if (userRepository.findByUsername(request.getUsername()).isPresent()) {
    throw new RuntimeException("Username is already taken!");
}
```
- Queries database for existing username
- Throws exception if username exists
- Prevents duplicate usernames

#### Step 2: Create User Entity (Lines 62-66)
```java
User user = new User();
user.setUsername(request.getUsername());
user.setPassword(passwordEncoder.encode(request.getPassword()));
user.setFullName(request.getFullName());
user.setFullName(request.getFullName());  // Note: Duplicate line (bug)
```
- Creates new `User` entity
- **Password is hashed** using BCrypt (never stored in plaintext)
- Sets username and full name

#### Step 3: Link Department (Lines 68-73)
```java
if (request.getDepartment() != null && !request.getDepartment().isEmpty()) {
    Department dept = departmentRepository.findByName(request.getDepartment())
            .orElseThrow(() -> new RuntimeException("Department not found: " + request.getDepartment()));
    user.setDepartment(dept);
}
```
- Optional: Links user to department if provided
- Throws exception if department doesn't exist
- Department is a foreign key relationship

#### Step 4: Assign Role (Lines 75-87)
```java
Set<Role> roles = new HashSet<>();
String roleInput = (request.getRole() != null && !request.getRole().isEmpty())
        ? request.getRole()
        : "RECEPTION";  // Default role

String roleName = normalizeRoleName(roleInput);

Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
roles.add(role);
user.setRoles(roles);
```
- Defaults to "RECEPTION" if no role provided
- Normalizes role name (e.g., "Front Desk" → "RECEPTION")
- Fetches role from database
- Assigns role to user (many-to-many relationship)

#### Step 5: Save User (Line 89)
```java
userRepository.save(user);
```
- Persists user to database
- Triggers `@PrePersist` to set `createdAt` timestamp

#### Step 6: Auto-Login (Lines 91-96)
```java
return login(new LoginRequest() {{
    setUsername(request.getUsername());
    setPassword(request.getPassword());
}}, response);
```
- Creates anonymous `LoginRequest` object
- Calls `login()` method
- Returns tokens and user info
- User is immediately authenticated after registration

---

## Method: `login()` (Lines 99-118)

### Purpose
Authenticates user credentials and issues tokens.

### Code Walkthrough

#### Step 1: Authenticate Credentials (Lines 100-101)
```java
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
```
- **Critical security step**
- Validates username and password against database
- Throws `AuthenticationException` if credentials are invalid
- Uses `CustomUserDetailsService` internally to load user

#### Step 2: Load User (Lines 103-105)
```java
User user = userRepository.findByUsername(request.getUsername())
        .orElseThrow(() -> new RuntimeException("User not found"));
UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
```
- Fetches user entity from database
- Loads `UserDetails` with roles and permissions
- `UserDetails` is Spring Security's user representation

#### Step 3: Generate Tokens (Lines 107-108)
```java
String accessToken = jwtService.generateToken(userDetails);
String refreshToken = createRefreshToken(user).getToken();
```
- **Access Token (JWT):**
  - Contains username as subject
  - Signed with secret key
  - Valid for 15 minutes
  - Stateless (not stored in database)

- **Refresh Token:**
  - Random UUID
  - Stored in database
  - Valid for 7 days
  - Used to get new access tokens

#### Step 4: Set Cookies (Lines 110-111)
```java
addTokenCookie(response, "accessToken", accessToken, jwtExpiration / 1000);
addTokenCookie(response, "refreshToken", refreshToken, refreshExpiration / 1000);
```
- Adds tokens as HttpOnly cookies
- Converts milliseconds to seconds for `maxAge`
- Cookies are automatically sent with future requests

#### Step 5: Build Response (Lines 113-117)
```java
String roleName = user.getRoles().isEmpty() ? "" : user.getRoles().iterator().next().getName();
Set<String> permissions = new HashSet<>();
user.getRoles().forEach(role -> role.getPermissions().forEach(p -> permissions.add(p.getCode())));

return new AuthResponse(user.getUsername(), roleName, permissions);
```
- Extracts first role name (users can have multiple roles)
- Collects all permissions from all roles
- Returns `AuthResponse` with user info
- Frontend uses this to display user info and control UI

---

## Method: `refreshToken()` (Lines 120-159)

### Purpose
Issues a new access token using a valid refresh token.

### Code Walkthrough

#### Step 1: Extract Refresh Token (Lines 122-137)
```java
String refreshToken = null;

// 1. Try getting from body
if (requestBody != null && requestBody.getRefreshToken() != null) {
    refreshToken = requestBody.getRefreshToken();
}

// 2. If not in body, try from cookie
if (refreshToken == null && request.getCookies() != null) {
    for (Cookie cookie : request.getCookies()) {
        if ("refreshToken".equals(cookie.getName())) {
            refreshToken = cookie.getValue();
            break;
        }
    }
}
```
- **Flexible token extraction:**
  - First checks request body
  - Falls back to cookie
- Supports both cookie-based and body-based refresh

#### Step 2: Validate Token Presence (Lines 139-141)
```java
if (refreshToken == null) {
    throw new RuntimeException("Refresh token is missing!");
}
```
- Ensures token was provided
- Prevents null pointer exceptions

#### Step 3: Validate and Process Token (Lines 143-158)
```java
return refreshTokenRepository.findByToken(refreshToken)
        .map(this::verifyExpiration)  // Check if expired
        .map(RefreshToken::getUser)   // Get associated user
        .map(user -> {
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String accessToken = jwtService.generateToken(userDetails);

            addTokenCookie(response, "accessToken", accessToken, jwtExpiration / 1000);

            String roleName = user.getRoles().isEmpty() ? "" : user.getRoles().iterator().next().getName();
            Set<String> permissions = new HashSet<>();
            user.getRoles().forEach(role -> role.getPermissions().forEach(p -> permissions.add(p.getCode())));

            return new AuthResponse(user.getUsername(), roleName, permissions);
        })
        .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
```
- **Functional programming style using `Optional`:**
  1. Find token in database
  2. Verify not expired (deletes if expired)
  3. Extract user from token
  4. Generate new access token
  5. Set new access token cookie
  6. Return user info

- **Note:** Refresh token is NOT regenerated, only access token

---

## Method: `logout()` (Lines 161-165)

### Purpose
Logs out user by clearing cookies.

### Code Walkthrough

```java
public void logout(HttpServletResponse response) {
    addTokenCookie(response, "accessToken", null, 0);
    addTokenCookie(response, "refreshToken", null, 0);
}
```
- Sets both cookies with `maxAge=0`
- Browser immediately deletes cookies
- User is logged out on frontend
- **Note:** Refresh token remains in database (could be improved)

---

## Method: `getCurrentUser()` (Lines 169-178)

### Purpose
Retrieves current user information.

### Code Walkthrough

```java
public AuthResponse getCurrentUser(String username) {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

    String roleName = user.getRoles().isEmpty() ? "" : user.getRoles().iterator().next().getName();
    Set<String> permissions = new HashSet<>();
    user.getRoles().forEach(role -> role.getPermissions().forEach(p -> permissions.add(p.getCode())));

    return new AuthResponse(user.getUsername(), roleName, permissions);
}
```
- Called by `/api/v1/auth/me` endpoint
- Username comes from Spring Security context
- Returns fresh user data from database
- Useful for refreshing user info without re-login

---

## Helper Method: `addTokenCookie()` (Lines 180-192)

### Purpose
Creates and adds HttpOnly cookies to response.

### Code Walkthrough

```java
private void addTokenCookie(HttpServletResponse response, String name, String value, long maxAge) {
    ResponseCookie cookie = ResponseCookie.from(name, value)
            .httpOnly(true)        // JavaScript cannot access
            .secure(secureCookie)  // HTTPS only (if enabled)
            .path("/")             // Available to all endpoints
            .maxAge(maxAge)        // Lifetime in seconds
            .sameSite("Lax")       // CSRF protection
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
}
```

**Cookie Security Features:**
- **HttpOnly** - Prevents XSS attacks (JavaScript can't read cookie)
- **Secure** - HTTPS-only in production
- **SameSite=Lax** - Prevents CSRF attacks
- **Path=/** - Cookie sent to all endpoints

---

## Helper Method: `createRefreshToken()` (Lines 194-204)

### Purpose
Creates or updates refresh token for a user.

### Code Walkthrough

```java
@Transactional
public RefreshToken createRefreshToken(User user) {
    RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
            .orElse(new RefreshToken());

    refreshToken.setUser(user);
    refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpiration));
    refreshToken.setToken(UUID.randomUUID().toString());

    return refreshTokenRepository.save(refreshToken);
}
```

**Logic:**
1. Check if user already has a refresh token
2. If yes, update it; if no, create new one
3. Set/update user association
4. Set new expiration date (7 days from now)
5. Generate new random UUID token
6. Save to database

**Result:** One refresh token per user (replaces old token on login)

---

## Helper Method: `verifyExpiration()` (Lines 206-212)

### Purpose
Checks if refresh token is expired and deletes if so.

### Code Walkthrough

```java
public RefreshToken verifyExpiration(RefreshToken token) {
    if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
        refreshTokenRepository.delete(token);
        throw new RuntimeException("Refresh token was expired. Please make a new signin request");
    }
    return token;
}
```

**Logic:**
1. Compare expiry date with current time
2. If expired:
   - Delete from database (cleanup)
   - Throw exception (user must re-login)
3. If valid:
   - Return token unchanged

---

## Helper Method: `normalizeRoleName()` (Lines 214-237)

### Purpose
Maps various role input formats to standardized role names.

### Code Walkthrough

```java
private String normalizeRoleName(String input) {
    String normalized = input.trim().toUpperCase();
    switch (normalized) {
        case "ADMINISTRATOR":
        case "ADMIN":
            return "ADMIN";
        case "DOCTOR":
        case "MEDICAL DOCTOR":
            return "DOCTOR";
        case "NURSE":
            return "NURSE";
        case "LAB TECHNICIAN":
        case "LAB":
        case "LAB_TECH":
            return "LAB_TECH";
        case "FRONT DESK":
        case "RECEPTIONIST":
        case "RECEPTION":
            return "RECEPTION";
        default:
            return normalized.replace(" ", "_");
    }
}
```

**Mapping Examples:**
- "Front Desk" → "RECEPTION"
- "Administrator" → "ADMIN"
- "Lab Technician" → "LAB_TECH"
- "doctor" → "DOCTOR"
- "Custom Role" → "CUSTOM_ROLE"

**Benefits:**
- User-friendly role names in registration
- Consistent database values
- Handles case variations

---

## Key Takeaways

### Security Best Practices
1. ✅ Passwords are hashed with BCrypt
2. ✅ Tokens stored in HttpOnly cookies
3. ✅ Dual-token strategy (access + refresh)
4. ✅ Refresh tokens validated against database
5. ✅ Expired tokens automatically deleted

### Design Patterns
1. **Service Layer** - Business logic separated from controllers
2. **Dependency Injection** - All dependencies injected via constructor
3. **Transaction Management** - Database operations are transactional
4. **Functional Programming** - `Optional` chaining in `refreshToken()`

### Potential Improvements
1. Delete refresh token from database on logout
2. Add token rotation (new refresh token on each refresh)
3. Add rate limiting for login attempts
4. Add audit logging for authentication events
5. Support multiple active refresh tokens per user
6. Add email verification for registration
7. Add password strength validation
8. Add "remember me" functionality

---

## Related Files

- [JwtService](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/JwtService.java) - JWT token operations
- [CustomUserDetailsService](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/security/CustomUserDetailsService.java) - User loading
- [AuthController](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/controller/AuthController.java) - REST endpoints
- [SecurityConfig](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/config/SecurityConfig.java) - Security configuration
