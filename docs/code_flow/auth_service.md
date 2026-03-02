# Auth & JWT Service — Code Flow Documentation

## Module Overview

| Aspect | Details |
|--------|---------|
| **Package** | `com.hms.HospitalManagementSystem.service.impl` |
| **Classes** | `AuthServiceImpl`, `JwtServiceImpl` |
| **Purpose** | User registration, login/logout, JWT token lifecycle, refresh token management |
| **Used By** | `AuthController`, Security filter chain |
| **Depends On** | `UserRepository`, `RoleRepository`, `DepartmentRepository`, `RefreshTokenRepository`, `PasswordEncoder`, `AuthenticationManager`, `CustomUserDetailsService`, `JwtService` |

---

## AuthService — Purpose & Responsibility

AuthService is the **central authentication gateway** for the HMS application. It handles:
- **User Registration** — creating new staff accounts with role assignment
- **Login** — authenticating users via Spring Security and issuing JWT + refresh tokens
- **Token Refresh** — reissuing access tokens using valid refresh tokens
- **Logout** — clearing authentication cookies
- **Current User Retrieval** — returning the logged-in user's profile and permissions

---

## Service Flow Diagrams

### 1. Registration Flow (`register`)

```
Client POST /api/auth/register
        │
        ▼
┌─────────────────────────┐
│  Check username unique  │ ── Conflict? → throw ConflictException
└─────────┬───────────────┘
          ▼
┌─────────────────────────┐
│  Create User entity     │
│  - Encode password      │
│  - Resolve department   │
│  - Normalize & set role │
└─────────┬───────────────┘
          ▼
┌─────────────────────────┐
│  Save user to DB        │
└─────────┬───────────────┘
          ▼
┌─────────────────────────┐
│  Call login() internally│ ── Issues JWT + Refresh Token
└─────────┬───────────────┘
          ▼
     AuthResponse
  (id, username, role, permissions)
```

**Key Details:**
- Passwords are hashed using `PasswordEncoder` (BCrypt)
- Role normalization maps friendly names → system roles (e.g., `"Front Desk"` → `"RECEPTION"`, `"Lab Technician"` → `"LAB_TECH"`)
- After saving, it delegates to `login()` to auto-authenticate the new user

### 2. Login Flow (`login`)

```
Client POST /api/auth/login
        │
        ▼
┌──────────────────────────────────┐
│ AuthenticationManager.authenticate│ ── Bad creds? → BadCredentialsException
│ (UsernamePasswordAuthToken)      │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Load User from DB (by username)  │
│ Load UserDetails via             │
│   CustomUserDetailsService       │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Generate JWT access token        │
│   (with userId in claims)        │
│ Create/update RefreshToken in DB │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Set HttpOnly cookies:            │
│   - accessToken (JWT expiry)     │
│   - refreshToken (long expiry)   │
└─────────┬────────────────────────┘
          ▼
     AuthResponse
  (id, username, role, permissions)
```

**Key Details:**
- Tokens are stored as **HttpOnly cookies** (not in response body) for XSS protection
- Cookie settings: `httpOnly=true`, `sameSite=Lax`, `secure` flag from config
- The refresh token is a random UUID saved in the database (not a JWT)

### 3. Token Refresh Flow (`refreshToken`)

```
Client POST /api/auth/refresh
        │
        ▼
┌──────────────────────────────────┐
│ Extract refreshToken from:       │
│   1. Request body                │
│   2. Cookie (fallback)           │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Look up token in DB              │ ── Not found? → BadCredentialsException
│ Verify expiration                │ ── Expired? → Delete token + throw
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Generate new JWT access token    │
│ Set new accessToken cookie       │
└─────────┬────────────────────────┘
          ▼
     AuthResponse
```

### 4. Logout Flow (`logout`)

```
Client POST /api/auth/logout
        │
        ▼
┌──────────────────────────────────┐
│ Clear cookies (set maxAge = 0):  │
│   - accessToken = null           │
│   - refreshToken = null          │
└──────────────────────────────────┘
```

---

## JwtService — Purpose & Responsibility

JwtService is a **pure utility service** for JWT token operations. It has **zero business logic** — only token construction and validation.

### Operations

| Method | Purpose |
|--------|---------|
| `generateToken(UserDetails)` | Creates a JWT with username as subject |
| `generateTokenWithUserId(UserDetails, Long)` | Creates a JWT with `userId` in custom claims |
| `extractUsername(String)` | Reads the `sub` (subject) claim from token |
| `extractUserId(String)` | Reads the custom `userId` claim from token |
| `isTokenValid(String, UserDetails)` | Validates: username matches + not expired |
| `isTokenValid(String, UserDetails, Long)` | Validates: username + userId + not expired |

### Token Structure

```json
{
  "sub": "admin",
  "userId": 1,
  "iat": 1709371924,
  "exp": 1709375524
}
```

- Algorithm: **HS256** (HMAC-SHA256)
- Secret key: Base64-decoded from `jwt.secret` config property
- Expiration: Configurable via `jwt.expiration` (in milliseconds)

---

## Where These Services Are Used

| Consumer | How |
|----------|-----|
| `AuthController` | Calls `register`, `login`, `refreshToken`, `logout`, `getCurrentUser` |
| `JwtAuthFilter` (Security) | Calls `JwtService.extractUsername()`, `isTokenValid()` on every request |
| `AuthServiceImpl` (internal) | Calls `JwtService.generateTokenWithUserId()` during login/refresh |

---

## Configuration Properties

| Property | Description |
|----------|-------------|
| `jwt.secret` | Base64-encoded signing key for HMAC-SHA256 |
| `jwt.expiration` | Access token lifetime (ms) |
| `jwt.refresh-expiration` | Refresh token lifetime (ms) |
| `app.security.secure-cookie` | Whether cookies use `Secure` flag (for HTTPS) |

---

## Error Handling

| Error | When |
|-------|------|
| `ConflictException` | Username already taken during registration |
| `ResourceNotFoundException` | Department or role not found during registration |
| `BadCredentialsException` | Invalid password, missing/expired refresh token |

---
---

# Detailed Method-by-Method Breakdown

## AuthServiceImpl — All Methods Explained

---

### `register(RegisterRequest request, HttpServletResponse response)`

**What it does:** Creates a new staff user account and auto-logs them in.

**Step-by-step:**

1. **Duplicate check** — queries `userRepository.findByUsername()` to ensure uniqueness
2. **Build User entity** — encodes password, resolves department, normalizes role name
3. **Save** — persists the user
4. **Auto-login** — delegates to `login()` internally to issue tokens immediately

**Important Code:**

```java
// 1. Duplicate username check
if (userRepository.findByUsername(request.getUsername()).isPresent()) {
    throw new ConflictException("Username is already taken!");
}

// 2. Password encoding — raw password is NEVER stored
user.setPassword(passwordEncoder.encode(request.getPassword()));

// 3. Department resolution — looks up by name string
if (request.getDepartment() != null && !request.getDepartment().isEmpty()) {
    Department dept = departmentRepository.findByName(request.getDepartment())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Department not found: " + request.getDepartment()));
    user.setDepartment(dept);
}

// 4. Role normalization — converts friendly names to system names
String roleName = normalizeRoleName(roleInput);
// "Front Desk" → "RECEPTION", "Lab Technician" → "LAB_TECH"

// 5. Auto-login after registration — reuses login() method
return login(new LoginRequest() {{
    setUsername(request.getUsername());
    setPassword(request.getPassword());
}}, response);
```

**Example Request:**
```json
POST /api/auth/register
{
  "username": "dr_sharma",
  "password": "SecurePass123",
  "fullName": "Dr. Rajesh Sharma",
  "department": "Cardiology",
  "role": "Doctor"
}
```

**Example Response:**
```json
{
  "id": 15,
  "username": "dr_sharma",
  "role": "DOCTOR",
  "permissions": ["patient:read", "appointment:write", "encounter:write", "prescription:write"]
}
```
+ Sets `accessToken` and `refreshToken` HttpOnly cookies

---

### `login(LoginRequest request, HttpServletResponse response)`

**What it does:** Authenticates a user with username/password, generates tokens, sets cookies.

**Step-by-step:**

1. **Authenticate** — Spring Security's `AuthenticationManager` validates credentials
2. **Load user** — fetches `User` entity and `UserDetails` for token generation
3. **Generate tokens** — creates JWT (access) and UUID (refresh) tokens
4. **Set cookies** — stores both tokens as HttpOnly cookies in the response

**Important Code:**

```java
// 1. Spring Security authentication — throws BadCredentialsException on failure
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(
        request.getUsername(), request.getPassword()
    )
);

// 2. Load user and UserDetails
User user = userRepository.findByUsername(request.getUsername())
        .orElseThrow(() -> new BadCredentialsException("User not found"));
UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

// 3. Generate tokens
String accessToken = jwtService.generateTokenWithUserId(userDetails, user.getId());
String refreshToken = createRefreshToken(user).getToken(); // UUID stored in DB

// 4. Set HttpOnly cookies — NOT in response body (XSS protection)
addTokenCookie(response, "accessToken", accessToken, jwtExpiration / 1000);
addTokenCookie(response, "refreshToken", refreshToken, refreshExpiration / 1000);
```

**Why `generateTokenWithUserId` instead of `generateToken`?**
The standard `generateToken` only puts `username` in the JWT subject. `generateTokenWithUserId` adds a custom `userId` claim so the security filter can resolve the user's ID without a database query on every request.

**Example Request:**
```json
POST /api/auth/login
{
  "username": "dr_sharma",
  "password": "SecurePass123"
}
```

**Example Response:**
```json
{
  "id": 15,
  "username": "dr_sharma",
  "role": "DOCTOR",
  "permissions": ["patient:read", "appointment:write", "encounter:write"]
}
```
**Response Headers:**
```
Set-Cookie: accessToken=eyJhbGciOiJIUzI1NiJ9...; Path=/; HttpOnly; SameSite=Lax; Max-Age=3600
Set-Cookie: refreshToken=550e8400-e29b-41d4-a716-446655440000; Path=/; HttpOnly; SameSite=Lax; Max-Age=604800
```

---

### `refreshToken(RefreshTokenRequest requestBody, HttpServletRequest request, HttpServletResponse response)`

**What it does:** Issues a new JWT access token using a valid refresh token. The refresh token itself is NOT rotated (same UUID remains valid until it expires).

**Step-by-step:**

1. **Extract refresh token** — tries request body first, falls back to cookie
2. **Validate** — looks up token in DB and checks expiration
3. **Issue new access token** — generates a fresh JWT and sets the cookie

**Important Code:**

```java
// 1. Try getting refresh token from body first
String refreshToken = null;
if (requestBody != null && requestBody.getRefreshToken() != null) {
    refreshToken = requestBody.getRefreshToken();
}

// 2. Fallback: read from cookie
if (refreshToken == null && request.getCookies() != null) {
    for (Cookie cookie : request.getCookies()) {
        if ("refreshToken".equals(cookie.getName())) {
            refreshToken = cookie.getValue();
            break;
        }
    }
}

// 3. Validate and issue new access token
return refreshTokenRepository.findByToken(refreshToken)
        .map(this::verifyExpiration)      // checks if token is expired
        .map(RefreshToken::getUser)        // gets the user from token
        .map(user -> {
            // Generate new access token only — refresh token stays the same
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String accessToken = jwtService.generateTokenWithUserId(userDetails, user.getId());
            addTokenCookie(response, "accessToken", accessToken, jwtExpiration / 1000);
            return buildAuthResponse(user);
        })
        .orElseThrow(() -> new BadCredentialsException("Refresh token is not in database!"));
```

**Why two sources for refresh token?**
The API supports both cookie-based (browser) and body-based (mobile/API client) token submission. The cookie approach is preferred for web apps (automatic sending), while the body approach supports non-browser clients.

**Example Request (body-based):**
```json
POST /api/auth/refresh
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Example Request (cookie-based):**
```
POST /api/auth/refresh
Cookie: refreshToken=550e8400-e29b-41d4-a716-446655440000
(empty body)
```

---

### `logout(HttpServletResponse response)`

**What it does:** Clears authentication cookies by setting them to null with `maxAge=0`.

**Important Code:**

```java
public void logout(HttpServletResponse response) {
    // Setting maxAge=0 tells the browser to immediately delete the cookie
    addTokenCookie(response, "accessToken", null, 0);
    addTokenCookie(response, "refreshToken", null, 0);
}
```

> **Note:** This does NOT invalidate the refresh token in the database. If someone has the refresh token value, they could still use it until it expires. For full security, the refresh token should also be deleted from `RefreshTokenRepository`.

---

### `getCurrentUser(String username)`

**What it does:** Returns the profile and permissions of the currently authenticated user.

**Important Code:**

```java
public AuthResponse getCurrentUser(String username) {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    return buildAuthResponse(user);
}
```

The `username` parameter comes from the JWT token's subject claim, extracted by the security filter before reaching this method.

**Example Response:**
```json
{
  "id": 15,
  "username": "dr_sharma",
  "role": "DOCTOR",
  "permissions": ["patient:read", "appointment:write", "encounter:write", "prescription:write", "lab:read"]
}
```

---

### `createRefreshToken(User user)`

**What it does:** Creates or updates a refresh token for the given user. Each user has **at most one** refresh token at a time.

**Important Code:**

```java
public RefreshToken createRefreshToken(User user) {
    // Find existing token for user OR create new
    RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
            .orElse(new RefreshToken());

    refreshToken.setUser(user);
    refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpiration));
    refreshToken.setToken(UUID.randomUUID().toString()); // random UUID, NOT a JWT

    return refreshTokenRepository.save(refreshToken);
}
```

**Why upsert instead of always creating?**
This ensures only ONE refresh token per user exists. If a user logs in from a new device, the old refresh token is overwritten — effectively logging out the old session. This is a security measure: it prevents unlimited token accumulation.

---

### `verifyExpiration(RefreshToken token)`

**What it does:** Checks if a refresh token has expired. If expired, deletes it from the database and throws an exception.

**Important Code:**

```java
public RefreshToken verifyExpiration(RefreshToken token) {
    if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
        // Token is expired — delete it and force re-login
        refreshTokenRepository.delete(token);
        throw new BadCredentialsException(
            "Refresh token was expired. Please make a new signin request"
        );
    }
    return token;
}
```

---

### `buildAuthResponse(User user)` (private)

**What it does:** Maps a `User` entity to the `AuthResponse` DTO, extracting role and permissions.

**Important Code:**

```java
private AuthResponse buildAuthResponse(User user) {
    // Get the first role name (HMS uses single-role per user in practice)
    String roleName = user.getRoles().isEmpty()
        ? "" : user.getRoles().iterator().next().getName();

    // Flatten all permissions from all roles into a Set
    Set<String> permissions = new HashSet<>();
    user.getRoles().forEach(role ->
        role.getPermissions().forEach(p -> permissions.add(p.getCode()))
    );

    return new AuthResponse(user.getId(), user.getUsername(), roleName, permissions);
}
```

**Permission aggregation example:**
```
User "dr_sharma"
  └── Role: DOCTOR
        ├── Permission: patient:read
        ├── Permission: appointment:write
        ├── Permission: encounter:write
        └── Permission: prescription:write

Result: permissions = {"patient:read", "appointment:write", "encounter:write", "prescription:write"}
```

---

### `normalizeRoleName(String input)` (private)

**What it does:** Converts human-friendly role names into system-standard role names.

**Important Code:**

```java
private String normalizeRoleName(String input) {
    String normalized = input.trim().toUpperCase();
    switch (normalized) {
        case "ADMINISTRATOR":
        case "ADMIN":          return "ADMIN";
        case "DOCTOR":
        case "MEDICAL DOCTOR": return "DOCTOR";
        case "NURSE":          return "NURSE";
        case "LAB TECHNICIAN":
        case "LAB":
        case "LAB_TECH":       return "LAB_TECH";
        case "FRONT DESK":
        case "RECEPTIONIST":
        case "RECEPTION":      return "RECEPTION";
        default:
            return normalized.replace(" ", "_"); // Fallback: "Custom Role" → "CUSTOM_ROLE"
    }
}
```

**Mapping Table:**

| User Input | Normalized Output |
|-----------|-------------------|
| `"Doctor"` | `"DOCTOR"` |
| `"Medical Doctor"` | `"DOCTOR"` |
| `"Front Desk"` | `"RECEPTION"` |
| `"Receptionist"` | `"RECEPTION"` |
| `"Lab Technician"` | `"LAB_TECH"` |
| `"Lab"` | `"LAB_TECH"` |
| `"admin"` | `"ADMIN"` |
| `"Custom Role"` | `"CUSTOM_ROLE"` |

---

### `addTokenCookie(HttpServletResponse response, String name, String value, long maxAge)` (private)

**What it does:** Creates and adds an HttpOnly cookie to the response.

**Important Code:**

```java
private void addTokenCookie(HttpServletResponse response, String name,
        String value, long maxAge) {
    ResponseCookie cookie = ResponseCookie.from(name, value)
            .httpOnly(true)             // JS cannot access — XSS protection
            .secure(secureCookie)        // HTTPS only (from config)
            .path("/")                   // Available on all routes
            .maxAge(maxAge)              // Lifetime in seconds
            .sameSite("Lax")             // CSRF protection
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
}
```

**Cookie Security Properties Explained:**

| Property | Value | Why |
|----------|-------|-----|
| `httpOnly` | `true` | JavaScript cannot read the cookie → prevents XSS token theft |
| `secure` | configurable | When `true`, cookie only sent over HTTPS → prevents interception |
| `sameSite` | `Lax` | Cookie sent on top-level navigation but not on cross-site AJAX → CSRF protection |
| `path` | `/` | Cookie is available for all API endpoints |

---
---

# JwtServiceImpl — All Methods Explained

---

### `generateToken(UserDetails userDetails)`

**What it does:** Creates a basic JWT with only the username as the subject.

```java
public String generateToken(UserDetails userDetails) {
    return generateToken(new HashMap<>(), userDetails);
    // Delegates to the overloaded version with empty extra claims
}
```

> **In practice**, this method is NOT used by AuthService. The `generateTokenWithUserId` variant is used instead because the application needs the `userId` claim.

---

### `generateTokenWithUserId(UserDetails userDetails, Long userId)`

**What it does:** Creates a JWT with `userId` as a custom claim. **This is the primary token generator used in the application.**

```java
public String generateTokenWithUserId(UserDetails userDetails, Long userId) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);  // Custom claim for quick user ID resolution
    return buildToken(claims, userDetails, jwtExpiration);
}
```

**Generated Token Payload:**
```json
{
  "userId": 15,
  "sub": "dr_sharma",
  "iat": 1709371924,
  "exp": 1709375524
}
```

---

### `buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration)` (private)

**What it does:** The core JWT construction method. All token generation methods delegate to this.

```java
private String buildToken(Map<String, Object> extraClaims,
        UserDetails userDetails, long expiration) {
    return Jwts.builder()
            .setClaims(extraClaims)                          // Custom claims (e.g., userId)
            .setSubject(userDetails.getUsername())            // "sub" = username
            .setIssuedAt(new Date(System.currentTimeMillis()))  // "iat" = now
            .setExpiration(new Date(System.currentTimeMillis() + expiration))  // "exp"
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)  // Sign with HMAC-SHA256
            .compact();                                      // Serialize to string
}
```

**Example output:** `eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjE1LCJzdWIiOiJkcl9zaGFybWEiLC...`

---

### `extractUsername(String token)`

**What it does:** Parses the JWT and returns the `sub` (subject) claim — the username.

```java
public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
    // Claims::getSubject is a method reference to getClaim("sub")
}
```

**Example:** `extractUsername("eyJhbG...")` → `"dr_sharma"`

---

### `extractUserId(String token)`

**What it does:** Parses the JWT and returns the custom `userId` claim.

```java
public Long extractUserId(String token) {
    return extractClaim(token, claims -> claims.get("userId", Long.class));
}
```

**Example:** `extractUserId("eyJhbG...")` → `15`

---

### `extractClaim(String token, Function<Claims, T> claimsResolver)`

**What it does:** A generic method that parses the JWT and applies any claim extraction function.

```java
public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);  // Parse entire JWT
    return claimsResolver.apply(claims);             // Apply the extraction function
}
```

**How the generic pattern works:**
```java
// Extract username: claimsResolver = Claims::getSubject
extractClaim(token, Claims::getSubject);       // → "dr_sharma"

// Extract userId: claimsResolver = custom lambda
extractClaim(token, c -> c.get("userId", Long.class));  // → 15

// Extract expiration: claimsResolver = Claims::getExpiration
extractClaim(token, Claims::getExpiration);    // → Date(2026-03-02T16:00:00)
```

---

### `isTokenValid(String token, UserDetails userDetails)`

**What it does:** Validates a JWT by checking the username matches and the token hasn't expired.

```java
public boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    //      ^^^ username in token matches DB user       ^^^ not past expiration
}
```

---

### `isTokenValid(String token, UserDetails userDetails, Long userId)`

**What it does:** Extended validation that also checks the `userId` claim matches.

```java
public boolean isTokenValid(String token, UserDetails userDetails, Long userId) {
    final String username = extractUsername(token);
    final Long tokenUserId = extractUserId(token);
    return (username.equals(userDetails.getUsername()))
            && (tokenUserId != null && tokenUserId.equals(userId))  // userId must match
            && !isTokenExpired(token);
}
```

**Why validate userId?** Prevents a scenario where a user's account is deleted and a new user is created with the same username — the old JWT would fail because the userId wouldn't match.

---

### `extractAllClaims(String token)` (private)

**What it does:** The core JWT parsing method. Verifies the signature and returns all claims.

```java
private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
            .setSigningKey(getSignInKey())    // Use the same key that signed the token
            .build()
            .parseClaimsJws(token)            // Parse + verify signature
            .getBody();                       // Return claims payload
}
```

> If the token is tampered with or signed with a different key, `parseClaimsJws` throws a `SignatureException`. If the token is malformed, it throws `MalformedJwtException`. If expired, it throws `ExpiredJwtException`.

---

### `getSignInKey()` (private)

**What it does:** Converts the Base64-encoded secret from config into a cryptographic `Key` object.

```java
private Key getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);  // Decode from Base64 string
    return Keys.hmacShaKeyFor(keyBytes);                   // Create HMAC-SHA key
}
```

**The key flow:**
```
application.yml: jwt.secret = "dGhpcyBpcyBhIHZlcnkgc2VjcmV0IGtleQ=="
                                          ↓ (Base64 decode)
                              byte[]: [116, 104, 105, 115, ...]
                                          ↓ (HMAC-SHA key)
                              Key object (used for sign + verify)
```

---

## Complete Authentication Lifecycle Example

```
1. REGISTER
   POST /api/auth/register { username: "nurse_priya", password: "pass123", role: "Nurse" }
   → User created, password BCrypt-hashed
   → normalizeRoleName("Nurse") → "NURSE"
   → Auto-login: JWT + refresh token set as cookies
   → Response: { id: 20, username: "nurse_priya", role: "NURSE", permissions: [...] }

2. LATER... access token expires (e.g., after 1 hour)

3. REFRESH
   POST /api/auth/refresh (refresh token sent via cookie automatically)
   → Refresh token found in DB, not expired
   → New JWT access token generated, new cookie set
   → Response: { id: 20, username: "nurse_priya", role: "NURSE", permissions: [...] }

4. ON EVERY API CALL (handled by JwtAuthFilter, not AuthService)
   GET /api/patients (accessToken cookie sent automatically)
   → JwtAuthFilter extracts accessToken from cookie
   → extractUsername(token) → "nurse_priya"
   → loadUserByUsername("nurse_priya") → UserDetails
   → isTokenValid(token, userDetails) → true
   → Request proceeds to controller

5. LOGOUT
   POST /api/auth/logout
   → Cookies cleared (maxAge=0)
   → Browser deletes cookies
   → Subsequent requests have no token → 401 Unauthorized
```
