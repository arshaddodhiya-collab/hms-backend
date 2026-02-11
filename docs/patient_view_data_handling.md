# Patient View Data Handling & Information Architecture

The "Patient View" is designed to be the **Clinical Dashboard** for a doctor. It follows the **"10-Second Rule"**: A doctor should be able to grasp the patient's status in 10 seconds, then drill down if needed.

This document outlines the breakdown of data sections, their purpose, and the technical strategy for handling them without overloading the client or server.

## 1. The Information Architecture

### A. Basic Details (The "Who")
- **What:** Name, Age, Gender, Blood Group, Allergy Tag.
- **Why Needed:** **Safety.** The doctor must confirm they are treating the correct person and check for immediate risks (Allergies) before prescribing anything.
- **Handling:** Loaded instantly with the main `GET /patients/{id}` call.

### B. Active Vitals (The "Now")
- **What:** Blood Pressure, Temperature, Pulse, Weight (displayed as top cards).
- **Why Needed:** **Triage.** This is not the entire vitals history; this is specifically the **Latest Vitals** recorded (e.g., by the nurse 5 minutes ago). It tells the doctor the patient's *current* physical state.
- **Handling:** 
  - **Backend:** The API `GET /patients/{id}/vitals/latest` runs a query optimized to fetch only the most recent record: `SELECT * FROM vitals WHERE patient_id = ? ORDER BY date DESC LIMIT 1`.
  - **Frontend:** Displayed prominently at the top of the dashboard.

### C. The Tabs (The "History")
To prevent "Information Overload," historical data sections are hidden behind tabs and are **Lazy Loaded** (fetched only when clicked).

#### 1. Overview / Medical History
- **What:** Chronic conditions (Diabetes, Hypertension), past surgeries.
- **Why:** Provides context for long-term care and diagnosis.
- **Handling:** Sent with the main patient profile (typically small text data, so safe to include in the initial load).

#### 2. Appointments
- **What:** List of past and future visits.
- **Why:** To track attendance, view visit history, or schedule follow-ups.
- **Handling:** **Paginated.** We never fetch *all* appointments at once. The system fetches a page (e.g., `GET /appointments?patientId=123&page=0&size=5`). The user clicks "Next" to see more.

#### 3. Prescriptions
- **What:** Active and past medications.
- **Why:** To prevent drug interactions (e.g., "I can't prescribe Drug X because the patient is already taking Drug Y").
- **Handling:** The default view shows only **Active** prescriptions. Historical/Completed prescriptions are archived or shown on request.

#### 4. Documents
- **What:** X-Rays, Lab Reports (PDFs), Scans.
- **Why:** Evidence and proof for diagnosis.
- **Handling:** **On-Demand Only.** The list view shows only metadata (filenames, dates). The actual heavy file (PDF/Image) is *only* downloaded when the user clicks "View".

## 2. Technical Data Strategy (Performance Optimization)

To ensure high performance, we strictly avoid loading one giant JSON object containing all patient data. Instead, we split the data retrieval into "Widgets" or functional blocks.

| UI Section | API Call Strategy | Load Timing |
| :--- | :--- | :--- |
| **Profile Header** | `GET /patients/{id}` | **Immediate** (Page Load) |
| **Vitals Cards** | `GET /encounters/latest/vitals` | **Immediate** (Page Load) |
| **Medical History** | Included in Profile | **Immediate** (Page Load) |
| **Appointments** | `GET /appointments?patient={id}` | **Lazy** (On Tab Click) |
| **Lab Results** | `GET /lab-requests?patient={id}` | **Lazy** (On Tab Click) |
| **Documents** | `GET /documents?patient={id}` | **Lazy** (On Tab Click) |

## 3. Summary

None of the displayed information is redundant. Each section answers a specific clinical question at a specific stage of the workflow:

1.  **Header:** "Who is this?" (Patient Identification)
2.  **Vitals:** "How are they right now?" (Current Status/Triage)
3.  **Tabs:** "What happened in the past?" (Historical Context & Evidence)

By splitting the API calls and utilizing tabs with lazy loading, we keep the page load time fast and the interface clean, providing the doctor with the right information exactly when they look for it.
