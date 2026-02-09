# Auth Service & API Endpoints

This document details the **Authentication Service** logic and the exposed **API Endpoints**.

## 1. AuthService (`com.hms.HospitalManagementSystem.service.AuthService`)

The `AuthService` orchestrates the authentication flow, bridging the `JwtService`, `Repositories`, and `AuthenticationManager`.

### Key Features
- **Transactional**: All operations are wrapped in `@Transactional` to ensure data integrity.
- **Registration**:
    - Validates if username exists.
    - Encodes password using `BCryptPasswordEncoder`.
    - Assigns default role (currently `RECEPTION` for testing, configurable).
    - Creates `User` entity.
- **Login**:
    - Authenticates credentials using Spring Security's `AuthenticationManager`.
    - Generates **Access Token** (via `JwtService`).
    - Creates or Updates **Refresh Token** (persisted in DB).
- **Refresh Token**:
    - Validates the provided refresh token against the database (`RefreshToken` entity).
    - Checks for expiration (default 7 days).
    - Issues a new **Access Token**.
    - Rotates or reuses the Refresh Token (current implementation reuses valid tokens).

## 2. Refresh Token Persistence (`RefreshToken` Entity)

To enable secure session management and revocation, Refresh Tokens are stored in the database.

- **Table**: `refresh_tokens`
- **Fields**:
    - `id`: PK
    - `token`: Unique UUID string.
    - `user_id`: FK to Users table (One-to-One).
    - `expiry_date`: Timestamp when the token expires.

## 3. Data Initializer (`com.hms.HospitalManagementSystem.config.DataInitializer`)

On application startup, this component seeds the database with essential security data:

1.  **Permissions**: Loads generic permissions (e.g., `MOD_PATIENTS`, `ACT_VIEW`) from `permissions.constants.ts` equivalent.
2.  **Roles**: Creates default roles if missing:
    - `ADMIN`: Full access.
    - `DOCTOR`: Medical access.
    - `NURSE`: Triage and patient care.
    - `RECEPTION`: Front desk operations.
3.  **Users**: Creates default users if missing:
    - `admin` / `admin123` (Role: ADMIN)
    - `doctor` / `doctor123` (Role: DOCTOR)

## 4. API Endpoints (`AuthController`)

Base URL: `/api/v1/auth`

### POST `/register`
Registers a new user.
**Request**:
```json
{
  "username": "jdoe",
  "password": "password123",
  "fullName": "John Doe",
  "departmentId": 1
}
```
**Response**: `AuthResponse` (Access Token, Refresh Token, User Details).

### POST `/login`
Authenticates an existing user.
**Request**:
```json
{
  "username": "jdoe",
  "password": "password123"
}
```
**Response**: `AuthResponse`

### POST `/refresh-token`
Obtains a new Access Token using a valid Refresh Token.
**Request**:
```json
{
  "refreshToken": "c6a71ce3-47d0-432d-9d47-ea8062e6781e"
}
```
**Response**: `AuthResponse` (New Access Token, Same/New Refresh Token).
