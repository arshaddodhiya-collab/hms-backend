# Search & Filtering

This document details the **PatientSpecification** implementation and search capabilities for the Patient module.

## 1. Overview

The Patient module uses **JPA Specifications** for dynamic, type-safe queries. This approach provides:
- Flexible search criteria
- Compile-time type safety
- Composable query conditions
- Integration with Spring Data JPA pagination

## 2. PatientSpecification

### Class Structure

```java
public class PatientSpecification {
    
    public static Specification<Patient> search(String query) {
        // Implementation
    }
    
    public static Specification<Patient> hasContact(String contact) {
        // Implementation
    }
}
```

### 2.1 Search Specification

**Method**: `search(String query)`

**Purpose**: Create a specification for searching patients by multiple fields

**Search Fields**:
- `firstName` (case-insensitive, partial match)
- `lastName` (case-insensitive, partial match)
- `contact` (case-insensitive, partial match)
- `email` (case-insensitive, partial match)

**Implementation**:
```java
public static Specification<Patient> search(String query) {
    return (root, criteriaQuery, criteriaBuilder) -> {
        if (!StringUtils.hasText(query)) {
            return criteriaBuilder.conjunction(); // Return all if no query
        }
        String likePattern = "%" + query.toLowerCase() + "%";
        return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("contact")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), likePattern));
    };
}
```

**Behavior**:
- If `query` is null or empty: Returns all patients (conjunction)
- If `query` is provided: Returns patients where ANY field matches (OR condition)
- All comparisons are case-insensitive
- Supports partial matching (LIKE %query%)

**Example Queries**:
```java
// Search for "john"
Specification<Patient> spec = PatientSpecification.search("john");
// Matches: firstName="John", lastName="Johnson", email="john@example.com"

// Search for "987"
Specification<Patient> spec = PatientSpecification.search("987");
// Matches: contact="9876543210"

// Empty search
Specification<Patient> spec = PatientSpecification.search(null);
// Returns all patients
```

---

### 2.2 Contact Filter Specification

**Method**: `hasContact(String contact)`

**Purpose**: Create a specification for exact contact number match

**Implementation**:
```java
public static Specification<Patient> hasContact(String contact) {
    return (root, query, cb) -> StringUtils.hasText(contact) 
        ? cb.equal(root.get("contact"), contact)
        : cb.conjunction();
}
```

**Behavior**:
- If `contact` is null or empty: Returns all patients
- If `contact` is provided: Returns patients with exact contact match
- Case-sensitive exact match

**Example Usage**:
```java
// Find patient with specific contact
Specification<Patient> spec = PatientSpecification.hasContact("9876543210");
```

---

## 3. Using Specifications in Service

### Basic Search

```java
@Transactional(readOnly = true)
public Page<PatientResponse> searchPatients(String query, Pageable pageable) {
    Specification<Patient> spec = PatientSpecification.search(query);
    return patientRepository.findAll(spec, pageable).map(patientMapper::toResponse);
}
```

### Combining Specifications

Specifications can be combined using `and()`, `or()`, and `not()`:

```java
// Search for patients named "John" with specific contact
Specification<Patient> spec = PatientSpecification.search("john")
        .and(PatientSpecification.hasContact("9876543210"));

// Search excluding deleted patients
Specification<Patient> spec = PatientSpecification.search(query)
        .and((root, query, cb) -> cb.equal(root.get("deleted"), false));
```

---

## 4. Pagination and Sorting

### Default Pagination

```java
// Default: page 0, size 20, sort by createdAt DESC
Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
```

### Custom Pagination

```java
// Page 2, size 10, sort by lastName ASC
Pageable pageable = PageRequest.of(2, 10, Sort.by("lastName").ascending());

// Multiple sort fields
Pageable pageable = PageRequest.of(0, 20, 
    Sort.by("lastName").ascending()
        .and(Sort.by("firstName").ascending()));
```

### Sortable Fields

| Field | Type | Indexed | Description |
|-------|------|---------|-------------|
| `id` | Long | Yes (PK) | Patient ID |
| `firstName` | String | Yes | First name |
| `lastName` | String | Yes | Last name |
| `dob` | LocalDate | No | Date of birth |
| `contact` | String | Yes | Contact number |
| `email` | String | No | Email address |
| `createdAt` | Timestamp | No | Registration date |
| `updatedAt` | Timestamp | No | Last update date |

**Performance Note**: Sorting by indexed fields (`firstName`, `lastName`, `contact`) is faster.

---

## 5. REST API Query Examples

### Basic Search

```bash
# Search for "john"
GET /api/v1/patients?query=john

# Search for contact containing "987"
GET /api/v1/patients?query=987

# Get all patients (no query)
GET /api/v1/patients
```

### Pagination

```bash
# First page, 10 items
GET /api/v1/patients?page=0&size=10

# Second page, 20 items
GET /api/v1/patients?page=1&size=20

# Third page with search
GET /api/v1/patients?query=doe&page=2&size=15
```

### Sorting

```bash
# Sort by last name ascending
GET /api/v1/patients?sort=lastName,asc

# Sort by creation date descending (default)
GET /api/v1/patients?sort=createdAt,desc

# Multiple sort fields
GET /api/v1/patients?sort=lastName,asc&sort=firstName,asc
```

### Combined Query

```bash
# Search "john", page 1, size 10, sort by name
GET /api/v1/patients?query=john&page=1&size=10&sort=lastName,asc
```

---

## 6. Advanced Specification Examples

### Filter Active Patients Only

```java
public static Specification<Patient> isActive() {
    return (root, query, cb) -> cb.equal(root.get("active"), true);
}

// Usage
Specification<Patient> spec = PatientSpecification.search(query)
        .and(PatientSpecification.isActive());
```

### Filter by Age Range

```java
public static Specification<Patient> ageBetween(int minAge, int maxAge) {
    return (root, query, cb) -> {
        LocalDate maxDob = LocalDate.now().minusYears(minAge);
        LocalDate minDob = LocalDate.now().minusYears(maxAge + 1);
        return cb.between(root.get("dob"), minDob, maxDob);
    };
}

// Usage: Find patients between 30-40 years old
Specification<Patient> spec = PatientSpecification.ageBetween(30, 40);
```

### Filter by Gender

```java
public static Specification<Patient> hasGender(Patient.Gender gender) {
    return (root, query, cb) -> gender != null 
        ? cb.equal(root.get("gender"), gender)
        : cb.conjunction();
}

// Usage
Specification<Patient> spec = PatientSpecification.search(query)
        .and(PatientSpecification.hasGender(Patient.Gender.MALE));
```

### Filter by Blood Group

```java
public static Specification<Patient> hasBloodGroup(Patient.BloodGroup bloodGroup) {
    return (root, query, cb) -> bloodGroup != null 
        ? cb.equal(root.get("bloodGroup"), bloodGroup)
        : cb.conjunction();
}

// Usage
Specification<Patient> spec = PatientSpecification.hasBloodGroup(Patient.BloodGroup.O_POSITIVE);
```

---

## 7. Performance Considerations

### Index Usage

The following indexes are defined on the `patients` table:
- `idx_patient_name`: Composite index on (first_name, last_name)
- `idx_patient_contact`: Index on contact

**Best Practices**:
1. Use indexed fields for sorting when possible
2. Avoid sorting by non-indexed fields for large datasets
3. Limit page size to reasonable values (10-50)
4. Use search queries that leverage indexes

### Query Optimization

```java
// GOOD: Uses indexed fields
Specification<Patient> spec = PatientSpecification.search("john");
// Leverages idx_patient_name and idx_patient_contact

// GOOD: Exact contact match
Specification<Patient> spec = PatientSpecification.hasContact("9876543210");
// Leverages idx_patient_contact

// CAUTION: Sorting by non-indexed field
Pageable pageable = PageRequest.of(0, 20, Sort.by("dob"));
// May be slower for large datasets
```

---

## 8. Client-Side Implementation

### React/Angular Example

```typescript
interface SearchParams {
  query?: string;
  page?: number;
  size?: number;
  sort?: string;
}

async function searchPatients(params: SearchParams) {
  const queryParams = new URLSearchParams();
  
  if (params.query) queryParams.append('query', params.query);
  if (params.page !== undefined) queryParams.append('page', params.page.toString());
  if (params.size) queryParams.append('size', params.size.toString());
  if (params.sort) queryParams.append('sort', params.sort);
  
  const response = await fetch(`/api/v1/patients?${queryParams}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  return response.json();
}

// Usage
const results = await searchPatients({
  query: 'john',
  page: 0,
  size: 10,
  sort: 'lastName,asc'
});
```

### Debounced Search

```typescript
import { debounce } from 'lodash';

const debouncedSearch = debounce(async (query: string) => {
  const results = await searchPatients({ query, page: 0, size: 10 });
  // Update UI with results
}, 300); // Wait 300ms after user stops typing

// In input handler
onSearchInput(event) {
  debouncedSearch(event.target.value);
}
```

---

## 9. Related Documentation

- [Patient Module Overview](./01_PATIENT_MODULE_OVERVIEW.md)
- [Patient Service & Endpoints](./02_PATIENT_SERVICE_AND_ENDPOINTS.md)
- [Database Schema](./04_DATABASE_SCHEMA.md)
