# Lab Module API Reference

## Base URL
`/api/v1`

## Endpoints

### 1. Lab Test Catalog

#### Get All Tests
Retrieves the list of available lab tests.
- **GET** `/lab-tests`
- **Auth**: `CMP_LAB_READ` or `CMP_CONSULTATION_READ`

**Response**:
```json
[
  {
    "id": 1,
    "name": "Complete Blood Count",
    "code": "CBC",
    "price": 500.00,
    "referenceRange": "N/A",
    "active": true
  }
]
```

#### Create Lab Test
Adds a new test to the catalog.
- **POST** `/lab-tests`
- **Auth**: `MOD_ADMIN` or `MOD_LAB`
- **Body**:
```json
{
  "name": "Hemoglobin A1C",
  "code": "HBA1C",
  "price": 600.00
}
```

---

### 2. Lab Requests

#### Create Lab Request
Orders a test for a patient.
- **POST** `/lab-requests`
- **Auth**: `CMP_CONSULTATION_WRITE` or `CMP_LAB_ENTRY`
- **Body**:
```json
{
  "encounterId": 100,
  "patientId": 45,
  "labTestId": 3,
  "notes": "Fasting required"
}
```

#### Get Lab Queue
Retrieves lab requests, optionally filtered.
- **GET** `/lab-requests`
- **Auth**: `CMP_LAB_READ` or `CMP_LAB_ENTRY`
- **Query Params**:
  - `status`: List of statuses (e.g., `ORDERED,SAMPLED`)
  - `encounterId`: Filter by encounter

#### Get Request Details
- **GET** `/lab-requests/{id}`
- **Auth**: `CMP_LAB_READ`
- **Response**: Includes `results` list and `parameters` list (for expected parameters).

#### Update Status
Manually update status (e.g., to SAMPLED).
- **PATCH** `/lab-requests/{id}/status?status=SAMPLED`
- **Auth**: `CMP_LAB_ENTRY`

---

### 3. Lab Results

#### Add Results
Submits results for a request. **Automatically completes the request.**
- **POST** `/lab-requests/{id}/results`
- **Auth**: `CMP_LAB_ENTRY`
- **Body**:
```json
[
  {
    "parameterName": "Glucose",
    "resultValue": "95",
    "unit": "mg/dL",
    "referenceRange": "70-100",
    "isAbnormal": false
  }
]
```

## Validation Rules

### Input Validation
The API utilizes strict Regex validation for medical fields:

1.  **Result Value & Reference Range**:
    - **Pattern**: `^[a-zA-Z0-9\s\.\-\+\<\>\/\%]+$`
    - **Allowed**: Alphanumeric, spaces, dots, hyphens, plus, less/greater than, slash, percent.
    - **Blocked**: Random special characters (@, #, $, etc.).

2.  **Parameter Name**:
    - **Pattern**: `^[a-zA-Z0-9\s\(\)\-\.]+$`
    - **Allowed**: Alphanumeric, spaces, brackets, hyphens, dots.

3.  **Unit**:
    - **Pattern**: `^[a-zA-Z0-9\s\/\%\^\(\)]*$`

### Business Validation
- **Status Check**: Cannot add results to a request that is already `COMPLETED` or `CANCELLED`.
- **Parameter Check**: (Frontend) Duplicate parameter names are not allowed in the same submission.
