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
    "username": "testuser",
    "role": "RECEPTION",
    "permissions": ["ACT_VIEW", "MOD_PATIENTS", ...]
}
```
*(Note: `accessToken` and `refreshToken` are now returned as **HttpOnly Cookies**, not in the JSON body. Postman handles these cookies automatically.)*

---

## 3. Get Current User Profile (Me)
**Endpoint**: `GET /me`

**Headers**:
- No explicit `Authorization` header needed if testing in Postman (Cookies are sent automatically).

**Expected Response**: `200 OK`
```json
{
    "username": "testuser",
    "role": "RECEPTION",
    "permissions": ["ACT_VIEW", "MOD_PATIENTS", ...]
}
```

---

## 4. Refresh Access Token
**Endpoint**: `POST /refresh-token`

**Body** (raw JSON):
- Empty or `{}`. The refresh token is read from the **HttpOnly Cookie**.

**Expected Response**: `200 OK`
```json
{
    "username": "testuser",
    "role": "RECEPTION",
    "permissions": [...]
}
```
*(New `accessToken` cookie will be set in the response)*

---

## 5. Logout
**Endpoint**: `POST /logout`

**Body**: Empty.

**Expected Response**: `200 OK`
*(Cookies `accessToken` and `refreshToken` will be cleared/expired)*

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
