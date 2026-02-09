# Authentication API Flow & Architecture

This document provides a visual and technical overview of how the Authentication system works in the HMS Backend.

## 1. System Architecture

The security module is built on **Spring Security 6** and uses **Stateless JWT Authentication**.

### Key Components

| Component | Responsibility |
| :--- | :--- |
| **SecurityConfig** | Main configuration. Disables CSRF, sets stateless session, configures CORS, and registers the JWT Filter. |
| **JwtAuthenticationFilter** | Intercepts *every* request. Checks for `Authorization: Bearer <token>`. Validates token and sets User in Context. |
| **AuthService** | Business logic for Login, Register, Refresh, and Logout. Transactional. |
| **JwtService** | Low-level JWT operations: Signing, Validating, Extracting Claims. |
| **CustomUserDetailsService** | Loads user data (Password, Authorities) from the DB for Spring Security. |
| **DataInitializer** | Seeds the database with default Roles (`ADMIN`, `DOCTOR`, etc.) and Permissions on startup. |

---

## 2. Authentication Flows

### A. Login Flow (`POST /api/v1/auth/login`)

The user exchanges credentials (username/password) for an Access Token and a Refresh Token.

```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthService
    participant AuthManager as AuthenticationManager
    participant UserRepo
    participant JwtService
    participant RefreshRepo as RefreshTokenRepository

    Client->>AuthController: POST /login {user, pass}
    AuthController->>AuthService: login(request)
    
    AuthService->>AuthManager: authenticate(user, pass)
    note right of AuthManager: Validates hash using BCrypt
    AuthManager-->>AuthService: Authentication Successful
    
    AuthService->>UserRepo: findByUsername(user)
    UserRepo-->>AuthService: User Entity (Roles loaded)
    
    AuthService->>JwtService: generateToken(UserDetails)
    JwtService-->>AuthService: Access Token (JWT)
    
    AuthService->>RefreshRepo: findByUser(user)
    RefreshRepo-->>AuthService: Existing Token (or empty)
    AuthService->>RefreshRepo: save(RefreshToken)
    
    AuthService-->>AuthController: AuthResponse (Access + Refresh + Context)
    AuthController-->>Client: 200 OK
```

### B. Registration Flow (`POST /api/v1/auth/register`)

Registers a new user and immediately logs them in.

```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthService
    participant UserRepo
    participant RoleRepo
    
    Client->>AuthController: POST /register
    AuthController->>AuthService: register(request)
    
    AuthService->>UserRepo: existsByUsername?
    alt Exists
        AuthService-->>Client: Error: Username taken
    end
    
    AuthService->>RoleRepo: findByName("RECEPTION")
    RoleRepo-->>AuthService: Role Entity
    
    AuthService->>UserRepo: save(User with encoded password)
    
    note right of AuthService: Internally calls login()
    AuthService->>AuthService: login()
    
    AuthService-->>Client: AuthResponse (Tokens)
```

### C. Token Refresh Flow (`POST /api/v1/auth/refresh-token`)

Used when the Access Token expires. The client sends the long-lived user-specific Refresh Token to get a new Access Token.

```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthService
    participant RefreshRepo
    participant JwtService
    
    Client->>AuthController: POST /refresh-token {token}
    AuthController->>AuthService: refreshToken(request)
    
    AuthService->>RefreshRepo: findByToken(token)
    
    alt Token Not Found
        AuthService-->>Client: Error: Token not in DB
    else Token Found
        AuthService->>AuthService: verifyExpiration()
        alt Expired
            AuthService->>RefreshRepo: delete(token)
            AuthService-->>Client: Error: Expired
        else Valid
            AuthService->>JwtService: generateToken(user)
            JwtService-->>AuthService: New Access Token
            AuthService-->>Client: AuthResponse (New JWT)
        end
    end
```

### D. Protected Request Flow (Any secured endpoint)

How the system validates requests to endpoints like `/api/v1/patients`.

```mermaid
sequenceDiagram
    participant Client
    participant Filter as JwtAuthenticationFilter
    participant JwtService
    participant UserDetailsService
    participant SecurityContext
    participant Controller
    
    Client->>Filter: GET /api/v1/patients (Header: Bearer xyz)
    
    Filter->>Filter: Extract JWT
    Filter->>JwtService: extractUsername(token)
    
    alt Username found & Context Base
        Filter->>UserDetailsService: loadUserByUsername()
        UserDetailsService-->>Filter: UserDetails
        
        Filter->>JwtService: isTokenValid(token, userDetails)
        
        alt Valid
            Filter->>SecurityContext: setAuthentication(token)
            Filter->>Controller: chain.doFilter() (Request proceeds)
            Controller-->>Client: 200 OK
        else Invalid
            Filter->>Controller: chain.doFilter()
            Note right of Filter: Request proceeds anonymously
            Controller-->>Client: 401 Unauthorized (via EntryPoint)
        end
    end
```

### E. Logout Flow (`POST /api/v1/auth/logout`)

Revokes the Refresh Token so it cannot be used to generate new Access Tokens.

```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthService
    participant RefreshRepo
    
    Client->>AuthController: POST /logout {refreshToken}
    AuthController->>AuthService: logout(request)
    AuthService->>RefreshRepo: delete(refreshToken)
    AuthController-->>Client: 200 OK
```

---

## 3. Key Configuration Files

### `SecurityConfig.java`
Controls the entire security filter chain.
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll() // Open generic auth endpoints
            .anyRequest().authenticated() // Lock everything else
        )
        .sessionManagement(...) // Stateless
        .authenticationProvider(authenticationProvider)
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // Inject our filter
    return http.build();
}
```

### `JwtService.java`
Handles the math of encryption.
- **Access Token**: Short life (30 min). Contains Claims (Subject, IssuedAt, Exp).
- **Secret Key**: `HmacSHA256` key defined in `application.properties`.

### `RefreshToken.java`
Persists the session.
- Allows us to "Kill" a session by deleting this row from the DB.
- If we only used JWTs, we couldn't invalidate them before expiry (without a blacklist).
