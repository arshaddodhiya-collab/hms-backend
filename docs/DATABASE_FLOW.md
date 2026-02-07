## 1️⃣ Big Picture: How This Database Is Designed to Work

This schema is **event-driven around a PATIENT**.

Think of it like this:

```
PATIENT
  ├── APPOINTMENT (OPD entry point)
  │     └── ENCOUNTER (doctor interaction)
  │            ├── VITALS
  │            ├── PRESCRIPTION → PRESCRIPTION_ITEM
  │            └── LAB_REQUEST → LAB_RESULT
  │
  └── ADMISSION (IPD entry point)
         ├── BED → WARD
         └── INVOICE
```

👉 **PATIENT is the center of everything**
👉 **ENCOUNTER is the medical truth**
👉 **APPOINTMENT / ADMISSION are logistical wrappers**

This is **correct HMIS modeling**.

---

## 2️⃣ Authentication & Permission Flow (VERY IMPORTANT)

### Tables involved

* `USER`
* `ROLE`
* `PERMISSION`
* (join tables implied: `user_role`, `role_permission`)
* `AUDIT_LOG`

### How it works in real life

#### Login

1. User logs in
2. Backend validates `USER.username + password`
3. Fetch:

   * User roles
   * Permissions per role
4. Create JWT token with:

   * user_id
   * role names
   * permission codes

👉 Database is **NOT hit on every request**
👉 Permissions are cached (JWT / Redis)

---

#### Authorization (example)

Frontend checks:

```ts
// Check if user can enter vitals
hasPermission('CMP_VITALS_WRITE')
```

Backend checks:

```java
@PreAuthorize("hasAuthority('CMP_VITALS_WRITE')")
```

Backend logic:

```
USER → JOIN(user_roles) → ROLE → JOIN(role_permissions) → PERMISSION(code)
```

**Permission Codes (Examples):**
*   `MOD_LAB` (Access Lab Module)
*   `CMP_VITALS_WRITE` (Action: Record Vitals)
*   `CMP_CONSULTATION_READ` (Action: View History)

No permission → request denied **before DB write**

---

#### Audit

Every important action inserts into:

```text
AUDIT_LOG
- who did it
- what entity
- what action
- before/after details
```

Example:

> Doctor updates diagnosis → audit entry created

This is **mandatory in healthcare systems**.

---

## 3️⃣ OPD FLOW (Appointment → Encounter)

### Step-by-step (real world)

#### 1. Patient books appointment

```text
PATIENT
  └── APPOINTMENT
```

DB write:

```sql
INSERT INTO appointment
(patient_id, doctor_id, start_time, status, type)
VALUES (?, ?, ?, 'SCHEDULED', 'CONSULTATION');
```

Nothing medical yet — just scheduling.

---

#### 2. Patient arrives

Appointment status:

```text
SCHEDULED → CHECKED_IN
```

---

#### 3. Doctor starts consultation

Now **medical data begins**.

```text
APPOINTMENT → ENCOUNTER
```

DB write:

```sql
INSERT INTO encounter
(appointment_id, patient_id, doctor_id, status)
VALUES (?, ?, ?, 'IN_PROGRESS');
```

👉 **Encounter is the legal medical record**

---

## 4️⃣ Clinical Encounter Flow (Doctor Work)

Everything below belongs to **ONE ENCOUNTER**.

### Vitals

Nurse records vitals:

```text
ENCOUNTER → VITALS
```

One-to-one or one-to-many (configurable).

---

### Diagnosis & Notes

Stored directly in:

```text
ENCOUNTER
- chief_complaint
- diagnosis
- notes
```

This avoids unnecessary joins.

---

### Prescription

Doctor prescribes medicines:

```text
ENCOUNTER
  └── PRESCRIPTION
         └── PRESCRIPTION_ITEM (multiple)
```

Why split?

* Prescription = document
* Items = line items

This supports:

* Editing
* Re-printing
* Pharmacy integration

---

## 5️⃣ Lab Flow (Very Important Separation)

### 1. Doctor orders tests

```text
ENCOUNTER → LAB_REQUEST
```

Example:

```sql
INSERT INTO lab_request
(encounter_id, patient_id, test_name, status)
VALUES (?, ?, 'CBC', 'ORDERED');
```

---

### 2. Lab processes request

Status changes:

```text
ORDERED → COLLECTED → COMPLETED
```

---

### 3. Results recorded

```text
LAB_REQUEST → LAB_RESULT (multiple parameters)
```

Why this design is good:

* One test → many values
* Abnormal flags per parameter
* Easy reporting

---

## 6️⃣ IPD FLOW (Admission / Bed Management)

### Admission

```text
PATIENT → ADMISSION
```

Admission does:

* Locks a `BED`
* Assigns doctor
* Tracks stay duration

```sql
UPDATE bed SET is_occupied = true WHERE id = ?;
INSERT INTO admission (...);
```

---

### Bed & Ward logic

```text
WARD → BED → ADMISSION
```

Why separate?

* Capacity management
* Bed type (ICU, General, Private)
* Reports like occupancy %

---

### Discharge

On discharge:

* `discharge_date` set
* `BED.is_occupied = false`
* Triggers billing finalization

---

## 7️⃣ Billing & Invoice Flow

Billing is **event-driven**.

Sources:

* Admission charges
* Lab tests
* Procedures
* Medicines

Stored as:

```text
INVOICE
- items (JSON)
- total_amount
- status
```

Why JSON?

* Flexible line items
* Easy integration with billing engines
* Faster development

In enterprise setups → items become a table later.

---

## 8️⃣ Soft Deletes & Safety

You used:

```text
is_deleted BOOLEAN
```

This is **correct**.

Why?

* Medical data must never be physically deleted
* Legal & audit compliance

Example:

```sql
UPDATE patient SET is_deleted = true WHERE id = ?;
```

---

## 9️⃣ How Spring Boot Actually Uses This Database

### Transaction boundaries

Example: Complete OPD visit

```java
@Transactional
public void completeEncounter(...) {
    saveVitals();
    savePrescription();
    saveLabRequests();
    updateEncounterStatus();
    insertAuditLog();
}
```

Either **everything succeeds** or **everything rolls back**.

---

### Read patterns

* Patient summary → JOINs
* Dashboard → aggregates
* Reports → optimized queries / views

---

## 🔁 TL;DR — DATABASE FLOW SUMMARY

```
USER logs in
   ↓
Permission validated
   ↓
PATIENT created
   ↓
APPOINTMENT booked
   ↓
ENCOUNTER starts
   ├── VITALS
   ├── PRESCRIPTION → ITEMS
   └── LAB_REQUEST → LAB_RESULT
   ↓
(If IPD)
ADMISSION → BED → INVOICE
   ↓
AUDIT_LOG captures everything
```

---

## ✅ Final Verdict (Honest Review)

✔ Your schema is **correctly normalized**
✔ Flow matches **real hospital operations**
✔ Permissions & audit are production-ready
✔ Can scale to multi-department HMIS
