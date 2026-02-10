# Comprehensive Authentication Flow Guide (Deep Dive)

This document provides a detailed explanation of the **Cookie-based JWT Authentication System** implemented in the Hospital Management System (HMS) backend. It covers the entire lifecycle of a request, from registration to accessing protected resources, including architectural decisions and security measures.

## 1. Architecture Overview

The system uses a **Stateless** authentication model where the server does not store user sessions in memory. Instead, it relies on **JSON Web Tokens (JWT)** stored in **HttpOnly Cookies** on the client side.

### Key Technologies
*   **Spring Security 6**: The core framework handling authentication and authorization.
*   **JWT (JJWT)**: Used to create signed tokens containing user identity and expiration.
*   **HttpOnly Cookies**: Secure storage mechanism to prevent Cross-Site Scripting (XSS) attacks.
*   **RBAC (Role-Based Access Control)**: Database-driven permission system (`User` -> `Role` -> `Permission`).

---

## 2. Core Components

| Component | Responsibility |
| :--- | :--- |
| **`SecurityConfig`** | The "gatekeeper". Configures the filter chain, CORS, CSRF (disabled), and session management (Stateless). |
| **`JwtAuthenticationFilter`** | Intercepts *every* request. Checks for `accessToken` cookie, validates it, and sets the Security Context. |
| **`AuthService`** | Business logic for Register, Login, Refresh, and Logout operations. Handles cookie creation. |
| **`CustomUserDetailsService`** | Loads specific user data (including roles/permissions) from the database efficiently. |
| **`JwtService`** | Utility class for generating, signing, and extracting data from JWT tokens. |

---

## 3. Data Model (RBAC)

The security model is built on three main entities:

1.  **User**: The person logging in (e.g., `admin`).
2.  **Role**: A grouping of permissions (e.g., `DOCTOR`, `RECEPTION`).
3.  **Permission**: Granular access rights (e.g., `MOD_PATIENTS`, `ACT_VIEW`).

**Relationship**: `User` *Has-Many* `Roles` *Has-Many* `Permissions`.
*(Note: Current implementation simplifies this by assigning one main Role per user effectively, but the database supports multiple)*.

---

## 4. Authentication Flows

### A. Registration Flow (Simplified)

When a new user signs up, the system automatically assigns them a role and logs them in.

```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthService
    participant RoleRepository
    participant UserRepository

    Client->>AuthController: POST /register (JSON Body)
    AuthController->>AuthService: register(request, response)
    AuthService->>UserRepository: Check if username exists
    AuthService->>AuthService: Normalize Role (e.g., "Doctor" -> "DOCTOR")
    AuthService->>RoleRepository: Find Role by Name
    AuthService->>UserRepository: Save User with Role
    AuthService->>AuthService: Auto-Login (Proceed to Login Flow)
    AuthService-->>Client: Returns 200 OK + Set-Cookies
```

---

### B. Login Flow (The core logic)

This is where the cookies are generated.

```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthService
    participant AuthenticationManager
    participant JwtService
    participant DB

    Client->>AuthController: POST /login (username, password)
    AuthController->>AuthService: login(request, response)
    AuthService->>AuthenticationManager: authenticate(username, password)
    AuthenticationManager->>DB: Verify credentials
    
    rect rgb(200, 255, 200)
        Note right of AuthService: Token Generation
        AuthService->>JwtService: generateToken(userDetails) -> Access Token (15m)
        AuthService->>DB: createRefreshToken(user) -> Refresh Token (7d)
    end

    rect rgb(200, 200, 255)
        Note right of AuthService: Cookie Creation
        AuthService->>AuthService: addTokenCookie("accessToken", httpOnly=true)
        AuthService->>AuthService: addTokenCookie("refreshToken", httpOnly=true)
    end

    AuthService-->>Client: 200 OK (AuthResponse JSON)
    Note left of Client: Browser automatically stores HttpOnly Cookies
```

**Key Code Insight (`AuthService.login`):**
```java
// Sets the HTTP Response Cookies directly
addTokenCookie(response, "accessToken", accessToken, 15 * 60); 
addTokenCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60);
```

---

### C. Protected Request Flow (Validation)

How the backend knows who you are for subsequent requests (e.g., `GET /api/v1/patients`).

```mermaid
sequenceDiagram
    participant Client
    participant JwtAuthenticationFilter
    participant JwtService
    participant SecurityContext
    participant Controller

    Client->>JwtAuthenticationFilter: GET /resource (Cookies included automatically)
    JwtAuthenticationFilter->>JwtAuthenticationFilter: Extract "accessToken" from Cookies
    
    alt Token is Missing
        JwtAuthenticationFilter-->>Client: Continue Filter Chain (Anonymous User)
        Note right of Client: 403/401 Access Denied
    else Token Found
        JwtAuthenticationFilter->>JwtService: extractUsername(token)
        JwtAuthenticationFilter->>JwtService: isTokenValid(token)
        
        alt Token Valid
            JwtAuthenticationFilter->>SecurityContext: setAuthentication(user, roles, permissions)
            JwtAuthenticationFilter->>Controller: Forward Request
            Controller-->>Client: 200 OK (Resource Data)
        else Token Invalid/Expired
            JwtAuthenticationFilter->>SecurityContext: Clear Context
            JwtAuthenticationFilter-->>Client: 401 Unauthorized
        end
    end
```

---

### D. Token Refresh Flow

When the `accessToken` (15 mins) expires, the client uses the `refreshToken` (7 days) to get a new one silently.

```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthService
    participant DB

    Client->>AuthController: POST /refresh-token (Empty Body)
    Note right of Client: Sends "refreshToken" cookie
    AuthController->>AuthService: refreshToken(request, response)
    AuthService->>DB: Find RefreshToken
    AuthService->>AuthService: Verify Expiration
    
    alt Token Valid
        AuthService->>JwtService: Generate NEW Access Token
        AuthService->>AuthService: addTokenCookie("accessToken", new_token)
        AuthService-->>Client: 200 OK (New Cookie Set)
    else Token Invalid
        AuthService-->>Client: 403 Forbidden (Re-login required)
    end
```

---

### E. Logout Flow

Clearing the session.

```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthService

    Client->>AuthController: POST /logout
    AuthController->>AuthService: logout(response)
    
    rect rgb(255, 200, 200)
        Note right of AuthService: Invalidate Cookies
        AuthService->>AuthService: Set "accessToken" Max-Age = 0
        AuthService->>AuthService: Set "refreshToken" Max-Age = 0
    end
    
    AuthService-->>Client: 200 OK
    Note left of Client: Browser deletes cookies
```

---

## 5. Security Details

### Why HttpOnly Cookies?
Storing JWTs in `localStorage` behaves like saving your house key under the doormat. Any JavaScript code (including malicious scripts injected via XSS) can read it.
**HttpOnly Cookies** cannot be accessed by JavaScript (`document.cookie` returns nothing). They are only sent automatically by the browser to the server.

### Permissions in `AuthResponse`
While the tokens are hidden in cookies, the frontend still needs to know *what* the user can do (e.g., "Can I see the 'Delete Patient' button?").
The `AuthResponse` DTO provides this **safe** information:

```json
{
    "username": "doctor_strange",
    "role": "DOCTOR",
    "permissions": ["MOD_PATIENTS", "ACT_VIEW", "CMP_VITALS_WRITE"]
}
```
The Frontend uses this JSON to toggle UI elements, while the Backend uses the Cookie to enforce security.

### Eager Fetching (`Role.java`)
We use `@ManyToMany(fetch = FetchType.EAGER)` for permissions. This ensures that when we load a User, we immediately have their full security profile loaded in memory, preventing `LazyInitializationException` errors during the critical authentication phase.
