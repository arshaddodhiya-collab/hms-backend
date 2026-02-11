# JWT Token Security Enhancement - User ID in Claims

## Overview
Enhanced the JWT authentication system to include the user ID in token claims. This prevents token reuse even if multiple users have the same username, adding an extra layer of security.

## Changes Made

### 1. JwtService.java
**File:** [`service/JwtService.java`](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/JwtService.java)

#### New Methods

**`extractUserId(String token)`**
```java
public Long extractUserId(String token) {
    return extractClaim(token, claims -> claims.get("userId", Long.class));
}
```
- Extracts the `userId` claim from the JWT token
- Returns `null` if claim doesn't exist

**`generateTokenWithUserId(UserDetails userDetails, Long userId)`**
```java
public String generateTokenWithUserId(UserDetails userDetails, Long userId) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    return buildToken(claims, userDetails, jwtExpiration);
}
```
- Generates JWT token with `userId` as a custom claim
- Includes username in subject (standard claim)
- Signs with HS256 algorithm

**`isTokenValid(String token, UserDetails userDetails, Long userId)`**
```java
public boolean isTokenValid(String token, UserDetails userDetails, Long userId) {
    final String username = extractUsername(token);
    final Long tokenUserId = extractUserId(token);
    return (username.equals(userDetails.getUsername())) 
            && (tokenUserId != null && tokenUserId.equals(userId))
            && !isTokenExpired(token);
}
```
- Overloaded method that validates both username AND userId
- Ensures token belongs to specific user
- Prevents token reuse across users

---

### 2. AuthService.java
**File:** [`service/AuthService.java`](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/AuthService.java)

#### Modified Methods

**`login()` - Line 107**
```java
String accessToken = jwtService.generateTokenWithUserId(userDetails, user.getId());
```
- Changed from `generateToken()` to `generateTokenWithUserId()`
- Passes user ID to be included in token claims

**`refreshToken()` - Line 148**
```java
String accessToken = jwtService.generateTokenWithUserId(userDetails, user.getId());
```
- Updated to include user ID in refreshed access token
- Maintains consistency with login flow

---

### 3. JwtAuthenticationFilter.java
**File:** [`security/JwtAuthenticationFilter.java`](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/security/JwtAuthenticationFilter.java)

#### New Dependencies
```java
import com.hms.HospitalManagementSystem.entity.User;
import com.hms.HospitalManagementSystem.repository.UserRepository;

private final UserRepository userRepository;
```
- Added `UserRepository` to verify user ID from database

#### Enhanced Token Validation
```java
// Extract both username and userId from token
username = jwtService.extractUsername(token);
userId = jwtService.extractUserId(token);

// Verify user exists with matching username and ID
User user = userRepository.findByUsername(username).orElse(null);

if (user != null && user.getId().equals(userId)) {
    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
    
    // Validate token with both username and userId
    if (jwtService.isTokenValid(token, userDetails, userId)) {
        // Set authentication in security context
    }
}
```

**Security Checks:**
1. Extract username and userId from token
2. Verify user exists in database
3. Verify database user ID matches token user ID
4. Validate token signature and expiration
5. Only then set authentication

**Logging:**
- Warns if token validation fails
- Warns if user ID mismatch detected
- Helps with debugging and security monitoring

---

## Security Benefits

### 1. Prevents Token Reuse
**Scenario:** Two users with username "admin"
- User A (ID: 1) gets token with `userId: 1`
- User B (ID: 2) gets token with `userId: 2`
- User A's token will NOT work for User B (even with same username)

### 2. Additional Validation Layer
- **Before:** Only validated username and signature
- **After:** Validates username, userId, AND signature
- Requires attacker to forge both username and userId

### 3. Database Consistency Check
- Token userId must match database user ID
- Prevents stale tokens after user deletion/recreation
- Detects token tampering attempts

---

## Token Structure

### Before Enhancement
```json
{
  "sub": "john.doe",
  "iat": 1707645600,
  "exp": 1707646500
}
```

### After Enhancement
```json
{
  "sub": "john.doe",
  "userId": 123,
  "iat": 1707645600,
  "exp": 1707646500
}
```

**Claims:**
- `sub` - Subject (username)
- `userId` - Custom claim (user ID)
- `iat` - Issued at timestamp
- `exp` - Expiration timestamp

---

## Testing

### Manual Testing

#### 1. Login and Check Token
```bash
# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "test.user", "password": "password"}' \
  -c cookies.txt -v

# Decode JWT from cookie to verify userId claim
# Use jwt.io or similar tool
```

#### 2. Verify Token Validation
```bash
# Make authenticated request
curl -X GET http://localhost:8080/api/v1/auth/me \
  -b cookies.txt

# Should succeed with valid token containing userId
```

#### 3. Test Token Refresh
```bash
# Refresh token
curl -X POST http://localhost:8080/api/v1/auth/refresh-token \
  -b cookies.txt -c cookies.txt

# New access token should also contain userId
```

### Expected Behavior

**Valid Token:**
- Username matches database
- UserId matches database
- Token not expired
- ✅ Authentication succeeds

**Invalid Token (Username Mismatch):**
- Username doesn't match database
- ❌ Authentication fails

**Invalid Token (UserId Mismatch):**
- Username matches but userId doesn't
- ❌ Authentication fails
- Warning logged: "User ID mismatch or user not found"

**Invalid Token (Expired):**
- Token signature valid but expired
- ❌ Authentication fails
- Error logged: "JWT Token has expired"

---

## Migration Notes

### Existing Tokens
- Old tokens (without userId claim) will fail validation
- Users must re-login to get new tokens with userId
- Consider adding grace period if needed

### Backward Compatibility
If you need to support old tokens temporarily:

```java
// In JwtAuthenticationFilter
Long userId = jwtService.extractUserId(token);

if (userId == null) {
    // Old token without userId - use fallback validation
    if (jwtService.isTokenValid(token, userDetails)) {
        // Allow old token but log warning
        logger.warn("Token without userId claim used by: {}", username);
    }
} else {
    // New token with userId - use enhanced validation
    if (jwtService.isTokenValid(token, userDetails, userId)) {
        // Proceed
    }
}
```

---

## Security Recommendations

### 1. Monitor Logs
Watch for warnings:
- "Token validation failed for user"
- "User ID mismatch or user not found"

These indicate potential security issues or attacks.

### 2. Rotate JWT Secret
- Change `jwt.secret` periodically
- Invalidates all existing tokens
- Forces all users to re-login

### 3. Add Rate Limiting
Prevent brute force attacks on token validation:
```java
// Consider adding rate limiting to login endpoint
@RateLimiter(name = "login", fallbackMethod = "loginFallback")
public AuthResponse login(LoginRequest request) {
    // ...
}
```

### 4. Audit Trail
Log all authentication events:
- Successful logins
- Failed login attempts
- Token validation failures
- User ID mismatches

---

## Related Files

- [JwtService.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/JwtService.java)
- [AuthService.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/AuthService.java)
- [JwtAuthenticationFilter.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/security/JwtAuthenticationFilter.java)
- [CustomUserDetailsService.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/security/CustomUserDetailsService.java)
