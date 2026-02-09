# Postman Testing Guide - Authentication Module

This guide provides the necessary JSON payloads and steps to test the Authentication endpoints using Postman.

## Base URL
`http://localhost:8080/api/v1/auth`

## 1. Register a New User
**Endpoint**: `POST /register`

**Body** (raw JSON):
```json
{
  "username": "newuser",
  "password": "password123",
  "fullName": "New User",
  "department": "ICU",
  "role": "Nurse"
}
```

**Expected Response**: `200 OK`
```json
{
    "accessToken": "eyJhbGciOiJIUz...",
    "refreshToken": "uuid-string...",
    "username": "newuser",
    "role": "RECEPTION",
    "permissions": ["MOD_DASHBOARD", ...]
}
```

---

## 2. Login
**Endpoint**: `POST /login`

**Body** (raw JSON):
```json
{
  "username": "testuser",
  "password": "password123"
}
```
*(Note: `testuser` was created correctly during our verification steps)*

**Expected Response**: `200 OK`
```json
{
    "accessToken": "eyJhbGciOiJIUz...",
    "refreshToken": "uuid-string...",
    "username": "testuser",
    "role": "RECEPTION",
    "permissions": ["ACT_VIEW", "MOD_PATIENTS", ...]
}
```

---

## 3. Get Current User Profile (Me)
**Endpoint**: `GET /me`

**Headers**:
- `Authorization`: `Bearer <paste_access_token_here>`

**Expected Response**: `200 OK`
```json
{
    "accessToken": null,
    "refreshToken": null,
    "username": "testuser",
    "role": "RECEPTION",
    "permissions": ["ACT_VIEW", "MOD_PATIENTS", ...]
}
```

---

## 4. Refresh Access Token
**Endpoint**: `POST /refresh-token`

**Body** (raw JSON):
```json
{
  "refreshToken": "<paste_refresh_token_here>"
}
```

**Expected Response**: `200 OK`
```json
{
    "accessToken": "new-jwt-token...",
    "refreshToken": "same-or-new-uuid...",
    "username": "testuser",
    "role": "RECEPTION",
    "permissions": [...]
}
```

---

## 5. Logout
**Endpoint**: `POST /logout`

**Body** (raw JSON):
```json
{
  "refreshToken": "<paste_refresh_token_here>"
}
```

**Expected Response**: `200 OK` (No content or empty body)

---

## 6. Verify Logout (Negative Test)
Try to use the **same refresh token** again after logging out.

**Endpoint**: `POST /refresh-token`
**Body**: Same as Step 4.

**Expected Response**: `500 Internal Server Error`
```json
{
    "message": "Refresh token is not in database!",
    ...
}
```
