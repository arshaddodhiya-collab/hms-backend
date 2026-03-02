# Dashboard Service — Code Flow Documentation

## Module Overview

| Aspect | Details |
|--------|---------|
| **Package** | `com.hms.HospitalManagementSystem.service.impl` |
| **Class** | `DashboardServiceImpl` |
| **Purpose** | Aggregated dashboard statistics and recent activity feed for the admin/home screen |
| **Used By** | `DashboardController` |
| **Depends On** | `PatientRepository`, `AppointmentRepository`, `LabRequestRepository`, `InvoiceRepository`, `AdmissionRepository` |

---

## Purpose & Responsibility

DashboardService is the **read-only analytics aggregator** that powers the application's home dashboard. It provides:
1. **Key Statistics** — patient count, appointment count, pending labs, revenue, critical patients
2. **Recent Activity Feed** — a unified activity stream from multiple modules

Both responses are **cached** using Spring's `@Cacheable` annotation for performance.

---

## Service Flow Diagrams

### 1. Get Dashboard Stats (`getStats`)

```
Client GET /api/dashboard/stats
        │
        ▼  (cached: "dashboardStats")
┌──────────────────────────────────┐
│ Calculate date range:            │
│  startOfDay = today 00:00        │
│  endOfDay = today 23:59          │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Query each repository:           │
│                                  │
│  totalPatients                   │
│    ← patientRepository.count()   │
│                                  │
│  expectedAppointmentsToday       │
│    ← appointmentRepository       │
│      .countByStartDateTimeBetween│
│      (today, today)              │
│                                  │
│  pendingLabRequests              │
│    ← labRequestRepository       │
│      .countByStatus(ORDERED)     │
│                                  │
│  todaysRevenue                   │
│    ← invoiceRepository           │
│      .sumNetAmountByIssueDateBtw │
│      (today, today)              │
│                                  │
│  criticalPatientsCount           │
│    ← admissionRepository.count() │
└─────────┬────────────────────────┘
          ▼
     DashboardDTO
  {
    totalPatients: 1250,
    expectedAppointmentsToday: 34,
    pendingLabRequests: 12,
    todaysRevenue: 45000.00,
    criticalPatientsCount: 8
  }
```

**Key Details:**
- **Cached** with key `"dashboardStats"` — avoids repeated expensive queries
- Revenue uses `sumNetAmountByIssueDateBetween()` — a custom aggregate query
- `criticalPatientsCount` currently counts **all admissions** (TODO: filter by ADMITTED status)

### 2. Get Recent Activity (`getRecentActivity`)

```
Client GET /api/dashboard/activity
        │
        ▼  (cached: "dashboardActivity")
┌──────────────────────────────────┐
│ Fetch recent items from          │  
│ 3 sources (top 5 each):         │
│                                  │
│  ★ Recent Appointments           │
│    ← findTop5ByDeletedFalse      │
│      OrderByCreatedAtDesc()      │
│                                  │
│  ★ Recent Lab Requests           │
│    ← findTop5ByOrderBy           │
│      CreatedAtDesc()             │
│                                  │
│  ★ Recent Patients               │
│    ← findTop5ByDeletedFalse      │
│      OrderByCreatedAtDesc()      │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Merge all activities             │
│ Sort by timestamp (descending)   │
│ Take top 10                      │
└─────────┬────────────────────────┘
          ▼
     List<ActivityDTO> (max 10)
```

### Activity DTO Shape

| Field | Example |
|-------|---------|
| `id` | 42 |
| `type` | `"APPOINTMENT"`, `"LAB"`, `"PATIENT"` |
| `description` | `"Appointment for John with Dr. Smith"` |
| `timestamp` | `2026-03-02T10:30:00` |
| `status` | `"SCHEDULED"`, `"ORDERED"`, `"ACTIVE"` |
| `userRoleRequiringAccess` | `"DOCTOR"`, `"NURSE"`, `"RECEPTIONIST"` |

---

## Caching Strategy

| Cache Name | Method | Eviction |
|-----------|--------|----------|
| `dashboardStats` | `getStats()` | Manual/time-based (configured in cache settings) |
| `dashboardActivity` | `getRecentActivity()` | Manual/time-based |

> Caching is essential here because dashboard queries hit multiple tables and run aggregate functions. Without caching, every dashboard load would execute 5+ database queries.

---

## Data Sources Summary

```
Dashboard
  ├── PatientRepository ──────── totalPatients, recent patients
  ├── AppointmentRepository ──── today's count, recent appointments
  ├── LabRequestRepository ───── pending count, recent labs
  ├── InvoiceRepository ──────── today's revenue
  └── AdmissionRepository ────── critical patient count
```
