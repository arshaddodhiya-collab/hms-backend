# HMS Pagination — Full-Stack Guide

## Overview

HMS uses **Spring Data `Slice<T>`** for all list endpoints. `Slice` is a lighter alternative to `Page` — it avoids the expensive `COUNT(*)` query, making it faster for large tables. The tradeoff is that `Slice` doesn't know the **total number** of records.

---

## Backend (Spring Boot)

### How It Works

Every list endpoint follows a 4-layer pattern:

```
Controller → Service Interface → Service Impl → Repository
```

Each layer accepts a `Pageable` parameter and returns `Slice<T>`.

### 1. Repository Layer

Spring Data automatically generates paginated queries when the return type is `Slice<T>` and the method accepts `Pageable`.

```java
// Derived query — Spring generates the SQL automatically
Slice<Bed> findAllBy(Pageable pageable);

// Custom JPQL — just add Pageable param, Spring handles LIMIT/OFFSET
@Query("SELECT b FROM Bed b WHERE b.ward.id = :wardId AND b.isOccupied = false")
Slice<Bed> findAvailableBedsByWard(@Param("wardId") Long wardId, Pageable pageable);
```

> **Key Rule**: Keep existing `List<T>` methods if they're used internally (e.g., `DashboardServiceImpl`). Only API-facing methods need `Slice`.

### 2. Service Interface

```java
Slice<BedResponse> getAllBeds(Pageable pageable);
```

### 3. Service Implementation

Pass `pageable` to the repository. Use `Slice.map()` to convert entities to DTOs:

```java
@Override
@Transactional(readOnly = true)
public Slice<BedResponse> getAllBeds(Pageable pageable) {
    return bedRepository.findAllBy(pageable)
            .map(ipdMapper::toBedResponse);
}
```

### 4. Controller Layer

Use `@PageableDefault` to set default `page`, `size`, and `sort`:

```java
@GetMapping
public ResponseEntity<Slice<BedResponse>> getAllBeds(
        @PageableDefault(size = 10, sort = "number", direction = Sort.Direction.ASC)
        Pageable pageable) {
    return ResponseEntity.ok(bedService.getAllBeds(pageable));
}
```

> **⚠️ Important**: The `sort` field name must match the **entity field name** (e.g., `number`, not `bedNumber`). A mismatch causes a 500 error.

### Required Imports (Backend)

```java
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
```

### Slice JSON Response Format

When serialized, a `Slice` returns this JSON structure:

```json
{
  "content": [ ... ],          // The actual data array
  "pageable": {
    "pageNumber": 0,           // Current page (0-indexed)
    "pageSize": 10,            // Items per page
    "sort": { "sorted": true, "unsorted": false, "empty": false },
    "offset": 0
  },
  "size": 10,                  // Page size
  "number": 0,                 // Current page number
  "numberOfElements": 10,      // Items in THIS page
  "first": true,               // Is this the first page?
  "last": false,               // Is this the last page?
  "empty": false               // Is the content empty?
}
```

> **Note**: Unlike `Page`, `Slice` does **NOT** include `totalElements` or `totalPages`.

### Query Parameters

All paginated endpoints accept these query params automatically (Spring resolves them into `Pageable`):

| Param    | Default | Description                        | Example           |
|----------|---------|------------------------------------|--------------------|
| `page`   | `0`     | Page number (0-indexed)            | `?page=2`          |
| `size`   | `10`    | Items per page                     | `?size=50`         |
| `sort`   | varies  | Sort field + direction             | `?sort=name,asc`   |

Example: `GET /api/v1/ipd/beds?page=0&size=50&sort=number,asc`

---

## Frontend (Angular)

### How It Works

The frontend services call backend endpoints and extract the `content` array from the `Slice` JSON response using RxJS `map`. A `size=100` query param is sent to fetch large result sets in one request.

### Pattern Used in Every Service

```typescript
import { HttpParams } from '@angular/common/http';
import { map } from 'rxjs/operators';

getBeds(): Observable<Bed[]> {
  const params = new HttpParams().set('size', '100');
  return this.apiService
    .get<any>(`${this.path}/beds`, params)
    .pipe(map((res) => res.content || res));
}
```

**Breakdown:**
1. `new HttpParams().set('size', '100')` — Requests up to 100 items per page
2. `.get<any>(...)` — Uses `any` since the response is a Slice object, not a direct array
3. `.pipe(map(res => res.content || res))` — Extracts the `content` array; falls back to `res` itself if the response is already a plain array

### Page Model (TypeScript Interface)

Located at `src/app/core/models/page.model.ts`:

```typescript
export interface Page<T> {
  content: T[];
  totalElements?: number;   // Optional — Slice doesn't provide this
  totalPages?: number;       // Optional — Slice doesn't provide this
  size: number;
  number: number;
  last?: boolean;
  first?: boolean;
  numberOfElements?: number;
  empty?: boolean;
}
```

> `totalElements` and `totalPages` are marked optional since `Slice` doesn't include them.

### Services Updated

| Service                  | Methods Updated                                                    |
|--------------------------|--------------------------------------------------------------------|
| `appointment.service.ts` | `getAppointments`, `getAppointmentsByDate`, `getDoctorAppointments`, `getPatientAppointments`, `getUpcomingAppointmentsForDoctor` |
| `billing.service.ts`     | `getOutstandingInvoices`, `getAllInvoices`                          |
| `charge-catalog.service.ts` | `getAllCharges`                                                 |
| `encounter.service.ts`   | `getDoctorQueue`, `getOpdDoctorQueue`, `getPatientEncounters`      |
| `triage.service.ts`      | `getTriageQueue`                                                   |
| `ipd.service.ts`         | `getAdmissions`, `getBeds`, `getAvailableBeds`                     |
| `lab.service.ts`         | `getAllLabTests`, `getLabQueue`                                    |
| `admin.service.ts`       | `getDepartments`, `getUsers`, `getRoles`, `getAllPermissions`      |
| `dashboard.service.ts`   | `getRecentActivity`                                                |
| `patient.service.ts`     | `getPatients` (uses `Page<Patient>` model directly)                |

---

## Page vs Slice — Quick Comparison

| Feature               | `Page<T>`                  | `Slice<T>`                 |
|------------------------|----------------------------|----------------------------|
| SQL                    | `SELECT` + `COUNT(*)`      | `SELECT` only              |
| `totalElements`        | ✅ Yes                     | ❌ No                      |
| `totalPages`           | ✅ Yes                     | ❌ No                      |
| `hasNext()`            | ✅ Yes                     | ✅ Yes                     |
| Performance            | Slower on large tables     | Faster                     |
| Use case               | Need exact total count     | Infinite scroll / listings |

---

## Adding Pagination to a New Endpoint

### Backend Checklist

1. **Repository**: Add method with `Pageable` param and `Slice<T>` return type
2. **Service Interface**: Change `List<T>` → `Slice<T>`, add `Pageable` param
3. **Service Impl**: Pass `pageable` to repository, use `.map()` for DTO conversion
4. **Controller**: Add `@PageableDefault(sort = "fieldName", direction = DESC) Pageable pageable`

### Frontend Checklist

1. Add `HttpParams` import
2. Add `const params = new HttpParams().set('size', '100')`
3. Pass `params` to the API call
4. Add `.pipe(map(res => res.content || res))` to extract the array
