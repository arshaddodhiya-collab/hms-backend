# DATABASE_IMPLEMENTATION_PLAN.md

## 1️⃣ Database Overview
This database supports the **Hospital Management System (HMS)**. It is designed for **MySQL 8.0+** using **InnoDB** for ACID compliance. The schema is highly normalized to support granular reporting on clinical data (Vitals, Prescriptions, Lab Results).

### Domain Areas
- **Auth & IAM**: Users, Roles, Permissions (RBAC), Departments
- **Core Master Data**: Patients, Doctors, Staff
- **Clinical Operations**: Appointments, Encounters (Consultations), Vitals, Prescriptions
- **Diagnostics**: Lab Requests, Test Results, Test Catalog
- **Financials**: Invoices, Payments, Services
- **Audit**: System logs, Entity change logs

### Scalability Strategy
- **Normalization**: Clinical data (`vitals`, `prescriptions`, `lab_results`) is normalized into separate tables to allow for trend analysis.
- **JSON Columns**: Reserved only for truly unstructured or dynamic data (e.g., `invoice_items` snapshot).
- **Soft Deletes**: All tables have `is_deleted` to preserve history.

---

## 2️⃣ Naming Conventions
- **Tables**: Singular, `snake_case` (e.g., `user`, `patient_visit`).
- **Columns**: `snake_case` (e.g., `first_name`, `is_active`).
- **Foreign Keys**: `referenced_table_id` (e.g., `user_id`, `patient_id`).
- **Indexes**: `idx_tablename_columnname` (e.g., `idx_user_email`).
- **Constraints**: `fk_tablename_referencedtable` (e.g., `fk_appointment_patient`).

---

## 3️⃣ Common Columns (Applied to ALL Tables)
Every table (except join tables) includes these standard audit columns:

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PK, AUTO_INCREMENT` | Primary Key |
| `uuid` | `CHAR(36)` | `NOT NULL, UNIQUE` | Public Reference ID (UUID v4) |
| `is_deleted` | `BOOLEAN` | `DEFAULT FALSE` | Soft delete flag |
| `created_at` | `DATETIME` | `DEFAULT NOW()` | Creation timestamp |
| `updated_at` | `DATETIME` | `DEFAULT NOW() ON UPDATE NOW()` | Update timestamp |
| `created_by` | `BIGINT` | `NULLABLE` | User ID who created it |
| `updated_by` | `BIGINT` | `NULLABLE` | User ID who updated it |

---

## 4️⃣ Core Entity Tables

### 🔐 Auth & Master Data

#### 🗄️ Table: `department`
Hospital departments (e.g., Cardiology, General Medicine).

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `name` | `VARCHAR(100)` | `UQ, NN` | Department Name |
| `head_user_id` | `BIGINT` | `FK` | Head of Dept (User) |
| `description` | `TEXT` | | |

#### 🗄️ Table: `user`
Represents all system users (Admin, Doctor, Nurse, Receptionist).

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `username` | `VARCHAR(50)` | `UQ, NN` | Login ID |
| `password` | `VARCHAR(255)` | `NN` | BCrypt Hash |
| `full_name` | `VARCHAR(100)` | `NN` | Display Name |
| `email` | `VARCHAR(100)` | `UQ` | Contact Email |
| `phone` | `VARCHAR(20)` | | Contact Phone |
| `department_id` | `BIGINT` | `FK` | Staff Department |
| `is_active` | `BOOLEAN` | `DEF TRUE` | Account status |

**Indexes**: `idx_user_username`, `idx_user_email`

#### 🗄️ Table: `role`
Pre-defined roles.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `name` | `VARCHAR(50)` | `UQ, NN` | e.g. 'Administrator', 'Doctor' |
| `description` | `VARCHAR(255)`| | Human readable description |

**Seed Data (Roles)**:
- `Administrator`
- `Doctor`
- `Nurse`
- `Lab Technician`
- `Front Desk`

#### 🗄️ Table: `permission`
Granular permissions (e.g., `PATIENT_CREATE`, `LAB_VIEW`).

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `code` | `VARCHAR(50)` | `UQ, NN` | Permission Key (e.g. MOD_PATIENTS) |
| `module` | `VARCHAR(50)` | `NN` | Grouping (e.g. PATIENTS) |
| `description` | `VARCHAR(255)`| | |

**Seed Data (Permissions)**:
> Derived from `src/app/core/constants/permissions.constants.ts`

| Module | Code | Description |
| :--- | :--- | :--- |
| **Modules** | `MOD_DASHBOARD` | Access Dashboard |
| | `MOD_PATIENTS` | Access Patients Module |
| | `MOD_APPOINTMENTS` | Access Appointments Module |
| | `MOD_TRIAGE` | Access Triage Module |
| | `MOD_CONSULTATION` | Access Consultation Module |
| | `MOD_LAB` | Access Lab Module |
| | `MOD_BILLING` | Access Billing Module |
| | `MOD_ADMIN` | Access Admin Module |
| | `MOD_VOICE` | Access Voice Module |
| **Actions** | `ACT_VIEW` | &nbsp; |
| | `ACT_CREATE` | &nbsp; |
| | `ACT_EDIT` | &nbsp; |
| | `ACT_DELETE` | &nbsp; |
| **Components** | `CMP_VITALS_WRITE` | &nbsp; |
| | `CMP_VITALS_READ` | &nbsp; |
| | `CMP_PATIENT_ADD` | &nbsp; |
| | `CMP_PATIENT_LIST` | &nbsp; |
| | `CMP_PATIENT_VIEW` | &nbsp; |
| | `CMP_PATIENT_EDIT` | &nbsp; |
| | `CMP_APPOINTMENT_LIST` | &nbsp; |
| | `CMP_APPOINTMENT_CREATE` | &nbsp; |
| | `CMP_APPOINTMENT_VIEW` | &nbsp; |
| | `CMP_APPOINTMENT_EDIT` | &nbsp; |
| | `CMP_CONSULTATION_READ` | &nbsp; |
| | `CMP_CONSULTATION_WRITE` | &nbsp; |
| | `CMP_LAB_ENTRY` | &nbsp; |
| | `CMP_LAB_READ` | &nbsp; |
| | `CMP_INVOICE_GENERATE` | &nbsp; |
| | `CMP_PAYMENT_RECEIPT` | &nbsp; |
| | `CMP_BILLING_SUMMARY` | &nbsp; |
| | `CMP_ADMIN_DEPT_READ` | &nbsp; |
| | `CMP_ADMIN_DEPT_WRITE` | &nbsp; |
| | `CMP_ADMIN_USER_READ` | &nbsp; |
| | `CMP_ADMIN_USER_WRITE` | &nbsp; |
| | `CMP_ADMIN_ROLE_WRITE` | &nbsp; |

#### 🗄️ Table: `patient`
Core patient demographic data.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `first_name` | `VARCHAR(50)` | `NN` | |
| `last_name` | `VARCHAR(50)` | `NN` | |
| `dob` | `DATE` | `NN` | Date of Birth |
| `gender` | `ENUM` | `NN` | 'MALE', 'FEMALE', 'OTHER' |
| `contact` | `VARCHAR(20)` | `NN` | Phone number |
| `email` | `VARCHAR(100)` | | Email address |
| `address` | `TEXT` | | Full address |
| `blood_group`| `VARCHAR(5)` | | A+, O-, etc. |
| `allergies` | `TEXT` | | Known allergies |
| `avatar` | `VARCHAR(255)` | | Profile image URL |

**Indexes**: `idx_patient_name`, `idx_patient_contact`

---

## 5️⃣ Relationship Mapping

### 🔗 User-Role-Permission (RBAC)
- **`user_role`**: Many-to-Many (`user_id`, `role_id`).
    - Maps Users to Roles (e.g. User 'john' is a 'Doctor').
- **`role_permission`**: Many-to-Many (`role_id`, `permission_id`).
    - Maps Roles to Permissions.
    - **Seed Data** (from `mock-users.config.ts`):
        - **Administrator**: All Permissions.
        - **Doctor**: `MOD_DASHBOARD`, `MOD_PATIENTS`, `MOD_APPOINTMENTS`, `MOD_CONSULTATION`, `ACT_VIEW`, `ACT_CREATE`, `ACT_EDIT`, `CMP_PATIENT_LIST`, `CMP_PATIENT_VIEW`, `CMP_APPOINTMENT_LIST`, `CMP_APPOINTMENT_VIEW`, `CMP_VITALS_READ`, `CMP_CONSULTATION_WRITE`, `CMP_CONSULTATION_READ`, `CMP_LAB_READ`.
        - **Nurse**: `MOD_DASHBOARD`, `MOD_PATIENTS`, `MOD_APPOINTMENTS`, `MOD_TRIAGE`, `MOD_CONSULTATION`, `ACT_VIEW`, `ACT_CREATE`, `CMP_APPOINTMENT_LIST`, `CMP_APPOINTMENT_VIEW`, `CMP_VITALS_WRITE`, `CMP_VITALS_READ`, `CMP_CONSULTATION_READ`, `CMP_LAB_READ`.
        - **Lab Technician**: `MOD_DASHBOARD`, `MOD_LAB`, `ACT_VIEW`, `ACT_CREATE`, `CMP_LAB_ENTRY`, `CMP_LAB_READ`.
        - **Front Desk**: `MOD_DASHBOARD`, `MOD_PATIENTS`, `MOD_APPOINTMENTS`, `MOD_BILLING`, `ACT_VIEW`, `ACT_CREATE`, `ACT_EDIT`, `CMP_PATIENT_ADD`, `CMP_PATIENT_LIST`, `CMP_PATIENT_VIEW`, `CMP_PATIENT_EDIT`, `CMP_APPOINTMENT_LIST`, `CMP_APPOINTMENT_CREATE`, `CMP_APPOINTMENT_VIEW`, `CMP_APPOINTMENT_EDIT`, `CMP_INVOICE_GENERATE`, `CMP_PAYMENT_RECEIPT`, `CMP_BILLING_SUMMARY`.

### 🔗 Clinical Logic
- `user` -> `department` (Many-to-One)
- `appointment` -> `patient` (Many-to-One)
- `encounter` -> `appointment` (One-to-One)
- `lab_request` -> `encounter` (Many-to-One)
- `lab_result` -> `lab_request` (Many-to-One)

---

## 6️⃣ Transactional Tables

#### 🗄️ Table: `appointment`
Scheduled visits.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `patient_id` | `BIGINT` | `FK, NN` | |
| `doctor_id` | `BIGINT` | `FK, NN` | User(Doctor) |
| `start_time` | `DATETIME` | `NN` | |
| `end_time` | `DATETIME` | | Optional duration |
| `status` | `ENUM` | `NN` | PENDING, CONFIRMED, CANCELLED, COMPLETED |
| `type` | `ENUM` | `DEF 'CONSULTATION'` | CONSULTATION, FOLLOWUP, EMERGENCY |
| `reason` | `VARCHAR(255)`| | Chief complaint for scheduling |
| `notes` | `TEXT` | | Internal notes |

#### 🗄️ Table: `encounter`
The central clinical record for a visit.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `appointment_id`| `BIGINT` | `FK, UQ` | Link to schedule |
| `patient_id` | `BIGINT` | `FK, NN` | |
| `doctor_id` | `BIGINT` | `FK, NN` | |
| `chief_complaint`| `TEXT` | | |
| `diagnosis` | `TEXT` | | |
| `notes` | `TEXT` | | Internal/Private Notes |
| `status` | `ENUM` | `NN` | IN_PROGRESS, FINISHED |

#### 🗄️ Table: `vitals`
Normalized vitals data.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `encounter_id` | `BIGINT` | `FK, UQ` | One set of vitals per encounter |
| `temperature` | `DECIMAL(4,1)`| | |
| `systolic` | `INT` | | |
| `diastolic` | `INT` | | |
| `pulse` | `INT` | | |
| `spo2` | `INT` | | |
| `weight` | `DECIMAL(5,2)`| | |
| `height` | `DECIMAL(5,2)`| | |
| `bmi` | `DECIMAL(4,1)`| | |


### 🏥 Inpatient (IPD)

#### 🗄️ Table: `ward`
Hospital wards/units.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `name` | `VARCHAR(50)` | `UQ, NN` | e.g., General Ward, ICU |
| `type` | `VARCHAR(20)` | `NN` | |
| `capacity` | `INT` | `NN` | Total Beds |
| `is_active` | `BOOLEAN` | `DEF TRUE` | |

#### 🗄️ Table: `bed`
Individual beds in a ward.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `ward_id` | `BIGINT` | `FK, NN` | |
| `number` | `VARCHAR(10)` | `NN` | B-101 |
| `type` | `ENUM` | `NN` | GENERAL, ICU, VENTILATOR |
| `is_occupied`| `BOOLEAN` | `DEF FALSE` | |
| `is_active` | `BOOLEAN` | `DEF TRUE` | |

#### 🗄️ Table: `admission`
IPD Admissions.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `patient_id` | `BIGINT` | `FK, NN` | |
| `bed_id` | `BIGINT` | `FK, NN` | |
| `doctor_id` | `BIGINT` | `FK, NN` | |
| `admission_date`| `DATETIME` | `NN` | |
| `discharge_date`| `DATETIME` | | |
| `diagnosis` | `TEXT` | | |
| `discharge_summary`| `TEXT` | | |
| `status` | `ENUM` | `NN` | ADMITTED, DISCHARGED |

#### 🗄️ Table: `prescription`
Header for a prescription order.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `encounter_id` | `BIGINT` | `FK, UQ` | |
| `note` | `VARCHAR(255)`| | General instruction |
| `status` | `ENUM` | `NN` | DRAFT, ISSUED |

#### 🗄️ Table: `prescription_item`
Individual medicines in a prescription.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `prescription_id`| `BIGINT` | `FK, NN` | |
| `medicine_name` | `VARCHAR(100)`| `NN` | e.g. Paracetamol |
| `dosage` | `VARCHAR(50)` | `NN` | e.g. 500mg |
| `frequency` | `VARCHAR(50)` | `NN` | e.g. 1-0-1 |
| `duration` | `VARCHAR(50)` | | e.g. 5 Days |

#### 🗄️ Table: `lab_test_catalog`
Master data for available lab tests.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `name` | `VARCHAR(100)` | `NN, UQ` | e.g., "Complete Blood Count" |
| `code` | `VARCHAR(20)` | `UQ` | e.g., "CBC" |
| `price` | `DECIMAL(10,2)`| `NN` | Base price |
| `reference_range`| `TEXT` | | Default normal ranges |

#### 🗄️ Table: `lab_request`
Orders for lab tests.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `encounter_id` | `BIGINT` | `FK` | Trace back to visit |
| `patient_id` | `BIGINT` | `FK, NN` | |
| `test_name` | `VARCHAR(100)`| `NN` | Snapshot of test name |
| `status` | `ENUM` | `NN` | REQUESTED, IN_PROGRESS, COMPLETED |
| `technician_notes`| `TEXT` | | |

#### 🗄️ Table: `lab_result` (Normalized)
Individual parameter results for a lab request.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `lab_request_id` | `BIGINT` | `FK, NN` | Parent Request |
| `parameter_name` | `VARCHAR(100)`| `NN` | e.g., "Hemoglobin" |
| `result_value` | `VARCHAR(100)`| `NN` | e.g., "14.5" |
| `unit` | `VARCHAR(20)` | | e.g., "g/dL" |
| `reference_range`| `VARCHAR(100)`| | Snapshot of range |
| `is_abnormal` | `BOOLEAN` | `DEF FALSE` | Flag for highlighting |

#### 🗄️ Table: `invoice`
Billing records.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `patient_id` | `BIGINT` | `FK, NN` | |
| `admission_id`| `BIGINT` | `FK` | Optional for IPD |
| `total_amount` | `DECIMAL(10,2)`| `NN` | |
| `status` | `ENUM` | `NN` | PENDING, PAID, CANCELLED |
| `items` | `JSON` | `NN` | `[{ "desc": "Consultation", "amount": 500 }]` |
| `payment_date` | `DATETIME` | | |

---

## 7️⃣ Security & Access Control Tables

#### 🗄️ Table: `user_role` (Join Table)
| Column | Type | Constraints |
| :--- | :--- | :--- |
| `user_id` | `BIGINT` | `FK(user), PK_Composite` |
| `role_id` | `BIGINT` | `FK(role), PK_Composite` |

#### 🗄️ Table: `role_permission` (Join Table)
| Column | Type | Constraints |
| :--- | :--- | :--- |
| `role_id` | `BIGINT` | `FK(role), PK_Composite` |
| `permission_id`| `BIGINT` | `FK(permission), PK_Composite` |

---

## 8️⃣ Audit & Logging Tables

#### 🗄️ Table: `audit_log`
Centralized audit trail for sensitive actions.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | `BIGINT PK` | |
| `user_id` | `BIGINT` | Who did it? |
| `action` | `VARCHAR(50)` | CREATE, UPDATE, DELETE, LOGIN |
| `entity_name` | `VARCHAR(50)` | Patient, Appointment |
| `entity_id` | `VARCHAR(50)` | References UUID of target |
| `details` | `JSON` | Old vs New values (Diff) |
| `ip_address` | `VARCHAR(45)` | Source IP |
| `created_at` | `DATETIME` | Timestamp |

**Retention**: Move to cold storage after 1 year.

---

## 9️⃣ Performance & Indexing Strategy
1.  **UUID Lookups**: Verify `uuid` columns have `UNIQUE` constraint (implicitly indexed).
2.  **Soft Delete Filtering**: Most queries will include `WHERE is_deleted = false`.
3.  **Lab Results**: `idx_lab_result_request` on `lab_request_id` is crucial for fetching report data.

---

## 🔟 Data Integrity Rules
1.  **Strict Typing**: Vitals (`systolic`, `diastolic`, `pulse`) are now `INT`.
2.  **Referential Integrity**: Prescriptions and Lab Results must have a parent container.
3.  **Orphan Prevention**: Ensure dependent `lab_result` rows are marked deleted when `lab_request` is deleted.
