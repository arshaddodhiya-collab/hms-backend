# Authentication & Security Flow

This document details the **JWT-based authentication flow** and **CSRF strategy** for the Hospital Management System (HMS), bridging the Angular Frontend and Spring Boot Backend.

## 1. Security Architecture Overview

- **Pattern**: Stateless Authentication using JSON Web Tokens (JWT).
- **Storage**: Tokens are stored in the Client (Frontend).
  - *Access Token*: `localStorage` (Short-lived, e.g., 15 mins).
  - *Refresh Token*: `localStorage` (Long-lived, e.g., 7 days) & Database (Hashed).
- **Transport**: Protected via HTTPS (Production) and `Authorization: Bearer <token>` header.
- **CSRF**: **Disabled**.
  - *Reasoning*: The application uses **Bearer Tokens** in headers, not cookies. Browsers do not automatically attach auth headers like they do with cookies, making standard CSRF attacks impossible.

---

## 2. Login Flow

The user provides credentials, and the server validates them, returning a pair of tokens.

```mermaid
sequenceDiagram
    autonumber
    participant U as User
    participant FE as Angular Frontend
    participant API as Spring Boot API
    participant DB as Database

    U->>FE: Enter Username/Password
    FE->>API: POST /api/v1/auth/login <br/> {username, password}
    API->>DB: Find User & Verify Password (BCrypt)
    
    alt Invalid Credentials
        API-->>FE: 401 Unauthorized
        FE-->>U: Show Error Message
    else Valid Credentials
        API->>API: Generate Access Token (JWT)
        API->>API: Generate Refresh Token (UUID)
        API->>DB: Save Refresh Token Hash (User ID, Device)
        API-->>FE: 200 OK <br/> { access_token, refresh_token, user_context }
        
        Note right of FE: Store tokens in localStorage
        FE->>FE: Update AuthService State
        FE-->>U: Redirect to Dashboard
    end
```

---

## 3. Authenticated Request Flow

For every subsequent request, the Frontend automatically attaches the Access Token.

```mermaid
sequenceDiagram
    autonumber
    participant FE as Angular Frontend
    participant INT as AuthInterceptor
    participant API as Spring Boot API
    participant FLT as JwtAuthFilter
    participant SEC as SecurityContext

    FE->>INT: Initiate HTTP Request <br/> (e.g., GET /api/v1/patients)
    
    Note right of INT: Check localStorage for 'token'
    
    alt Token Exists
        INT->>INT: Clone Request matches
        INT->>INT: Add Header: <br/> Authorization: Bearer eyJhbG...
    end
    
    INT->>API: Send Request
    API->>FLT: Intercept Request
    
    flt->>FLT: Extract Token from Header
    
    alt Token Valid
        FLT->>SEC: Authentication user = details
        FLT->>API: Chain.doFilter()
        API-->>FE: 200 OK (Data)
    else Token Invalid / Expired
        FLT->>API: Chain.doFilter() (Anonymous)
        API-->>FE: 401 Unauthorized
    end
```

---

## 4. Refresh Token Flow (Handling 401)

When the Access Token expires, the Frontend must silently refresh it using the Refresh Token.

```mermaid
sequenceDiagram
    autonumber
    participant FE as Angular Frontend
    participant INT as AuthInterceptor
    participant API as Spring Boot API

    FE->>API: GET /api/v1/patients <br/> (Expired Access Token)
    API-->>FE: 403 Forbidden / 401 Unauthorized
    
    Note right of FE: HttpErrorInterceptor catches 401
    
    FE->>FE: Check 'isRefreshing' flag
    
    alt Not Refreshing
        FE->>FE: Set isRefreshing = true
        FE->>API: POST /api/v1/auth/refresh-token <br/> { refresh_token }
        
        alt Refresh Successful
            API-->>FE: 200 OK <br/> { access_token, refresh_token }
            FE->>FE: Update localStorage
            FE->>FE: Notify waiters (Subject.next)
            FE->>API: Retry Original Request <br/> (New Access Token)
            API-->>FE: 200 OK
        else Refresh Failed (Token Revoked/Expired)
            API-->>FE: 403 Forbidden
            FE->>FE: Logout User
            FE->>FE: Clear localStorage
            FE-->>U: Redirect to Login
        end
    else Already Refreshing
        FE->>FE: Wait for 'refreshTokenSubject'
        FE->>API: Retry Original Request
    end
```

---

## 5. CSRF (Cross-Site Request Forgery)

### Why is it disabled?

CSRF attacks rely on the browser's behavior of **automatically sending cookies** (like `JSESSIONID`) with cross-origin requests.

#### Scenario A: Cookie-Based Auth (Vulnerable to CSRF)
This is why traditional apps need a CSRF Token.

```mermaid
sequenceDiagram
    autonumber
    participant A as Attacker Site
    participant B as Browser
    participant S as Server (Bank)

    Note over B, S: User is logged in (Session Cookie stored)
    
    A->>B: User visits malicious-site.com
    B->>S: POST /transfer (Hidden Form Request)
    Note right of B: Browser AUTOMATICALLY attaches Cookie: JSESSIONID=123
    
    S->>S: Validate Session (Success)
    S->>S: Transfer Money (Exploit Successful!)
```

#### Scenario B: Token-Based Auth (Our Approach - Safe)
This is why we can disable CSRF protection safely.

```mermaid
sequenceDiagram
    autonumber
    participant A as Attacker Site
    participant B as Browser
    participant S as Server (HMS API)

    Note over B, S: User is logged in (Token in localStorage)
    
    A->>B: User visits malicious-site.com
    B->>S: POST /api/v1/patients (Hidden Form Request)
    Note right of B: Browser DOES NOT attach Authorization Header
    
    S->>S: Check Header (Missing)
    S-->>B: 401 Unauthorized (Attack Failed)
```

### Conclusion
Because our `AuthInterceptor` must **explicitly** read from `localStorage` and attach the `Authorization` header, a malicious site (which cannot read your domain's `localStorage`) cannot forge a valid request. Therefore, **CSRF protection is unnecessary**.

### Security Trade-off
*   **Risk**: If we stored the JWT in an `HttpOnly` cookie (to prevent XSS), we *would* generally need CSRF protection (specifically SameSite=Strict helps, but CSRF tokens are safer).
*   **Current Choice**: Storing in `localStorage` makes the token accessible to JavaScript.
    *   **Vulnerability**: XSS (Cross-Site Scripting). If an attacker injects a script, they can steal the token.
    *   **Mitigation**: Strict Input Validation, Output Encoding, and Content Security Policy (CSP).

---

## 6. Implementation Checklist

### Backend
- [ ] Ensure `SecurityConfig` disables CSRF: `.csrf(csrf -> csrf.disable())`.
- [ ] Ensure `CorsConfiguration` allows headers: `Authorization`, `Content-Type`.
- [ ] Implement `JwtFilter` to parse `Bearer ` string.

### Frontend
- [ ] `AuthInterceptor` must handle adding the header.
- [ ] `ErrorInterceptor` must handle 401 loop for Refresh Token logic (currently missing in v3).
