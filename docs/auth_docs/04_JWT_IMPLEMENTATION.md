# JWT Implementation Details

This document explains the implementation of the JWT Service and Authentication Filter.

## 1. JwtService (`com.hms.HospitalManagementSystem.service.JwtService`)

Responsible for all JWT operations.

### Configuration
Uses values from `application.properties`:
- `jwt.secret`: The signing key (HMAC SHA256).
- `jwt.expiration`: Access token validity (30 minutes).
- `jwt.refresh-expiration`: Refresh token validity (7 days).

### Key Methods

#### `generateToken(UserDetails userDetails)`
Generates a standard Access Token.
- **Subject**: `username`
- **IssuedAt**: Current time
- **Expiration**: Current time + 30 mins
- **Algorithm**: HS256

#### `generateRefreshToken(UserDetails userDetails)`
Generates a long-lived Refresh Token.
- **Expiration**: Current time + 7 days

#### `extractUsername(String token)`
Parses the token to retrieve the subject (username). Used by the filter to identify the user.

#### `isTokenValid(String token, UserDetails userDetails)`
Checks two things:
1.  Does the username in the token match the `UserDetails`?
2.  Is the token expired?

## 2. JwtAuthenticationFilter (`com.hms.HospitalManagementSystem.security.JwtAuthenticationFilter`)

Intercepts every HTTP request to check for valid specific credentials.

### Flow
1.  **Check Header**: Looks for `Authorization: Bearer <token>`.
2.  **Extract Token**: Removes "Bearer " prefix.
3.  **Extract Username**: Calls `JwtService.extractUsername(token)`.
4.  **Check Context**: If username exists and `SecurityContext` is empty:
    1.  Load `UserDetails` from database.
    2.  Validate token using `JwtService.isTokenValid()`.
    3.  If valid, create `UsernamePasswordAuthenticationToken`.
    4.  Set it in `SecurityContextHolder`.
5.  **Chain**: Always calls `filterChain.doFilter()` to proceed (authenticated or anonymous).

### Error Handling
If the token is invalid or expired, the filter catches the exception, logs it (optional), and allows the request to proceed anonymously. The `JwtAuthenticationEntryPoint` will then handle the 401 error if the endpoint requires authentication.
