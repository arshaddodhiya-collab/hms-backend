# Frontend Integration Todo List (Angular + Spring Boot)

This checklist tracks the tasks required to connect the Angular frontend with the Spring Boot backend.

## 1. Environment & Configuration
- [ ] **Configure API Base URL**
    - [ ] Update `src/environments/environment.ts` with `apiUrl: 'http://localhost:8080/api/v1'`.
    - [ ] Update `src/environments/environment.prod.ts` with production API URL.
- [ ] **HTTP Interceptor**
    - [ ] Create/Update `AuthInterceptor` to inject JWT token.
    - [ ] Implement `ErrorInterceptor` to handle global API errors (401, 403, 500).
        - [ ] Parse standardized error response (`{ timestamp, status, error, message, path }`).
        - [ ] Show generic error notifications (Toastr/Snackbar).
- [ ] **CORS Check**
    - [ ] Verify requests from `http://localhost:4200` are blocked/allowed as expected.

## 2. Authentication Integration
- [ ] **Update AuthService**
    - [ ] Replace mock login with `POST /api/v1/auth/login`.
    - [ ] Store `access_token` and `refresh_token` in `LocalStorage`.
    - [ ] Implement `refreshToken()` flow:
        - [ ] Intercept 401 errors -> Call `POST /api/v1/auth/refresh-token` -> Retry original request.
    - [ ] Implement `logout()`: Call `POST /api/v1/auth/logout` and clear storage.
    - [ ] Implement `isAuthenticated()` check based on token presence/expiry.
- [ ] **User Context**
    - [ ] Fetch current user details on app init (`GET /api/v1/auth/me`).
    - [ ] Load permissions into `NgxPermissions` (or custom store) for `PermissionGuard`.

## 3. Core Module Integration (Services & Models)
- [ ] **Patient Integration**
    - [ ] Update `PatientService` methods:
        - [ ] `getPatients(page, size, search)` -> `GET /api/v1/patients?page=0&size=10&search=...`.
        - [ ] `getPatient(id)` -> `GET /api/v1/patients/{id}`.
        - [ ] `createPatient(data)` -> `POST /api/v1/patients`.
        - [ ] `updatePatient(id, data)` -> `PUT /api/v1/patients/{id}`.
    - [ ] Align `Patient` model with Backend Entity (Audit fields, Date format).
- [ ] **Department Integration**
    - [ ] `DepartmentService` -> `GET /api/v1/admin/departments` (Dropdowns).

## 4. Clinical Module Integration (OPD)
- [ ] **Appointment Integration**
    - [ ] `AppointmentService`:
        - [ ] `bookAppointment()` -> `POST /api/v1/appointments`.
        - [ ] `getAppointments(doctor, date)` -> `GET /api/v1/appointments?date=...`.
        - [ ] `checkIn(id)` -> `PATCH /api/v1/appointments/{id}/status` (Status=CHECKED_IN).
- [ ] **Encounter & Vitals**
    - [ ] `EncounterService`:
        - [ ] `startEncounter()` -> `POST /api/v1/encounters`.
        - [ ] `saveVitals(id, vitals)` -> `POST /api/v1/encounters/{id}/vitals`.
        - [ ] `saveClinicalNotes(id, notes)` -> `PATCH /api/v1/encounters/{id}/clinical-notes`.
    - [ ] **Queues**:
        - [ ] Nurse Dashboard -> `GET /api/v1/encounters?status=TRIAGE`.
        - [ ] Doctor Dashboard -> `GET /api/v1/encounters?status=IN_PROGRESS`.
- [ ] **Prescription Integration**
    - [ ] `PrescriptionService` -> `POST /api/v1/encounters/{id}/prescriptions`.

## 5. Diagnostic Module (Labs)
- [ ] **Lab Service**
    - [ ] `getTestCatalog()` -> `GET /api/v1/lab-tests` (Dropdowns).
    - [ ] `createRequest(data)` -> `POST /api/v1/lab-requests`.
    - [ ] `getQueue(status)` -> `GET /api/v1/lab-requests?status=ORDERED,SAMPLED`.
    - [ ] `enterResults(id, results)` -> `POST /api/v1/lab-requests/{id}/results`.

## 6. Inpatient Module (IPD)
- [ ] **Ward & Bed Service**
    - [ ] `getWards()` -> `GET /api/v1/ipd/wards`.
    - [ ] `getBedStatus(wardId)` -> `GET /api/v1/ipd/wards/{id}/beds`.
- [ ] **Admission Service**
    - [ ] `admitPatient(data)` -> `POST /api/v1/ipd/admissions`.
    - [ ] `dischargePatient(id)` -> `POST /api/v1/ipd/admissions/{id}/discharge`.

## 7. Financial Module (Billing)
- [ ] **Billing Service**
    - [ ] `generateInvoice(data)` -> `POST /api/v1/billing/invoices`.
    - [ ] `getInvoices(patientId)` -> `GET /api/v1/billing/invoices?patientId=...`.

## 8. Dashboard Integration
- [ ] **Dashboard Service**
    - [ ] `getStats()` -> `GET /api/v1/dashboard/stats`.
    - [ ] `getRecentActivity()` -> `GET /api/v1/dashboard/activity`.
    - [ ] Update `StatsCardsComponent` and `TodayActivityComponent` to use real data.

## 9. UI/UX Refinements
- [ ] **Loading States**
    - [ ] Add loading spinners/skeletons while waiting for API responses in all lists.
- [ ] **Validation Sync**
    - [ ] Match Frontend RegEx with Backend `@Pattern`/`@Size`.
