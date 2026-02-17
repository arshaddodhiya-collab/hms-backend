# Product Requirements Document (PRD): Chat-Based Patient Data Access System

**Status:** Draft  
**Version:** 1.0  
**Date:** 2026-02-17  
**Author:** Product Management Team  

---

## 1. Product Overview

### 1.1 Vision
To revolutionize patient engagement by providing a secure, intuitive, and conversational interface for accessing personal health information, eliminating the complexity of traditional portal navigation.

### 1.2 Problem Statement
Patients often find traditional HMS dashboards complex and difficult to navigate, especially elderly or non-tech-savvy users. Retrieving simple information like "next appointment date" or "last blood test result" requires multiple clicks and understanding of UI hierarchies, leading to frustration and increased call center volume.

### 1.3 Target Users
*   **Patients:** Primary users seeking their own medical data.
*   **Caregivers:** Authorized proxies managing health data for dependents.

### 1.4 Value Proposition
*   **Simplicity:** Natural language queries replace complex menu navigation.
*   **Accessibility:** Low barrier to entry for users with limited digital literacy.
*   **Efficiency:** Instant access to critical health data 24/7 without waiting for hospital staff.

---

## 2. Goals & Non-Goals

### 2.1 Goals
*   Enable patients to query appointments, lab reports, prescriptions, and billing info via chat.
*   Ensure 100% HIPAA-compliant data handling and secure authentication.
*   Reduce patient support queries related to basic data retrieval by 30% within 6 months.
*   Provide a seamless integration with the existing Spring Boot HMS backend.

### 2.2 Non-Goals
*   **Medical Diagnosis:** The bot will NOT provide medical advice or symptom checking.
*   **Emergency Response:** The system is not for urgent care; emergency keywords will trigger immediate fallback instructions (e.g., "Call 911").
*   **Full administrative control:** Patients cannot update demographic data or insurance details via chat in V1.

---

## 3. User Personas

| Persona | Description | Needs | Pain Points |
| :--- | :--- | :--- | :--- |
| **Tech-Savvy Tina** (30s) | Professional, busy mom. | Quick access to kids' vaccination records and her own status. | Hates logging into slow web portals; prefers async chat. |
| **Elderly Earl** (70s) | Chronic conditions. | Simple way to check "When is my doctor visit?". | Confused by complex dashboards, small buttons, and nested menus. |
| **Chronic Chris** (45s) | Diabetes management. | Frequent checks on lab trends (HbA1c) and prescription refills. | Needs to track data over time easily without digging through PDFs. |
| **Admin Alice** (Hospital Staff) | Front desk manager. | Fewer calls asking "When is my appointment?". | Overwhelmed by routine repetitive questions. |

---

## 4. Core Features

1.  **Conversational Data Access:**
    *   **Appointments:** "When is my next visit?", "Show history for generic checks."
    *   **Lab Reports:** "Show my latest blood test," "Download the PDF for my lipid profile."
    *   **Prescriptions:** "What is my current medication?", "List my active prescriptions."
    *   **Billing:** "Do I have any pending invoices?", "Show my last payment receipt."

2.  **Chat History:** Persistent chat logs allowing users to review previous information.
3.  **Document Download:** Secure generation of temporary download links for PDFs (Reports, Receipts).
4.  **Notifications:** (Future Scope) Proactive alerts via chat (e.g., "Your lab report is ready").
5.  **Escalation:** "Talk to a human" option to bridge to a support agent queue (if available) or leave a message.
6.  **Role-Based Access:** Strict enforcement that a user can ONLY access their own patient data using the JWT subject.

---

## 5. Functional Requirements

### 5.1 Supported Chat Commands
The NLP/Regex layer must support variations of:
*   `Get Appointments` (Next, Past, All)
*   `Get Lab Results` (Latest, Specific Date, By Test Name)
*   `Get Medications` (Active, History)
*   `Get Bills` (Outstanding, Paid)
*   `Help` / `Menu`

### 5.2 Data Retrieval Flow
1.  User sends message.
2.  Backend authenticates request via JWT.
3.  Parser identifies Intent (e.g., `INTENT_GET_APPT`).
4.  Service layer queries `AppointmentRepository` for the `patientId` associated with the user.
5.  Response formatter constructs a structured Card or Text response.
6.  Response sent back to UI.

### 5.3 Error Handling
*   **No Data:** "Detailed search found no appointments scheduled for upcoming dates."
*   **System Error:** "I'm having trouble connecting to the records system. Please try again later."
*   **Unrecognized:** "I didn't understand that. You can ask me about Appointments, Labs, or Bills."

### 5.4 Security Enforcement
*   **Authentication:** Every WebSocket/REST call must have a valid Bearer Token.
*   **Authorization:** The `patient_id` used for DB queries must be derived *solely* from the authenticated User's context (`SecurityContextHolder`), NEVER from the request body.

---

## 6. Non-Functional Requirements

### 6.1 Security & Compliance
*   **Encryption:** TLS 1.3 for data in transit; AES-256 for data at rest.
*   **Audit Logging:** Every data query initiated by chat is logged in the `security_audit_log` table.
*   **Session Management:** Chat sessions expire after 15 minutes of inactivity.

### 6.2 Performance
*   **Latency:** < 200ms for text responses.
*   **Concurrency:** Support 500 concurrent chat sessions per instance.

### 6.3 Scalability
*   Stateless architecture (Session stored in Redis or DB) to allow horizontal scaling of backend nodes.

---

## 7. System Architecture Overview

### 7.1 High-Level Architecture
*   **Frontend Check Interface:** Web Widget / Mobile View.
*   **API Layer:** Secure Generic Chat API.
*   **Orchestrator Service:** `ChatService` processing inputs.
*   **Intent Parser:** Simple Regex/Keyword matcher (Phase 1) -> NLP integration (Phase 2).
*   **Module Integrations:** Direct calls to `AppointmentService`, `LabService`, `BillingService`.

### 7.2 WebSocket vs REST
*   **Decision:** **REST + Polling** (MVP) or **Server-Sent Events (SSE)**.
*   *Reasoning:* For simple query-response interactions, full bidirectional WebSockets may be overkill and harder to load balance initially. REST is sufficient for "User asks -> Bot answers".

### 7.3 Integration Points
*   `AppointmentController` -> `ChatService` invokes `AppointmentService.getUpcoming(patientId)`.
*   `LabController` -> `ChatService` invokes `LabService.getLatestResults(patientId)`.

---

## 8. Data Model

### 8.1 ChatMessage Entity
```java
@Entity
public class ChatMessage {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    private User patient; // Linked via User entity

    private String content; // Text content
    
    @Enumerated(EnumType.STRING)
    private SenderType sender; // USER, BOT
    
    @Enumerated(EnumType.STRING)
    private MessageType type; // TEXT, CARD, LINK
    
    private LocalDateTime timestamp;
}
```

### 8.2 Command Strategy
*   **Regex Pattern Matching:**
    *   `/(appoint|visit|schedule|doc)/i` -> `Appointments`
    *   `/(lab|result|blood|report)/i` -> `LabResults`
    *   `/(pill|med|drug|presc)/i` -> `Prescriptions`

---

## 9. UX Guidelines

*   **Tone:** Empathetic, clear, and professional. Avoid overly robotic technical jargon (e.g., use "Blood Pressure" instead of "Vitals Encounter JSON").
*   **Structured Cards:** Don't dump text. Use UI cards for data:
    *   *Appointment Card:* Date | Doctor Name | Status | [Cancel Button]
*   **Ambiguity:** If user says "Show results", ask "Do you mean Lab Results or Imaging Results?".

---

## 10. Edge Cases

*   **Zero Records:** Differentiate between "System Error" and "No records found". User: "Show my labs". Bot: "You don't have any lab reports on file yet."
*   **Expired Session:** If JWT expires during chat, frontend must auto-refresh or prompt re-login without losing the drafted message.
*   **Gibberish:** Graceful fallback to a "Help Menu" after 2 failed attempts to understand.

---

## 11. Risks & Mitigation

| Risk | Mitigation |
| :--- | :--- |
| **Data Leakage** | Strict IDOR checks; never trust client-side IDs; strict logging. |
| **Misinterpretation** | "Did I get this right?" confirmation buttons for critical actions (like cancelling appt). |
| **Performance Drain** | Rate limiting (max 10 queries/min per user) to prevent DoS. |

---

## 12. Metrics & KPIs

*   **Adoption Rate:** % of daily active users who try the chat feature.
*   **Resolution Rate:** % of conversations that do not end in "Contact Support".
*   **Latency:** Average time to load "My Appointments" via chat vs UI navigation.

---

## 13. Future Enhancements

*   **Voice Integration:** Speech-to-Text inputs for accessibility.
*   **AI/LLM Integration:** Upgrade regex parser to a fine-tuned medical LLM (e.g., Med-PaLM) for context-aware Q&A.
*   **Multilingual Support:** Auto-translation for Spanish, French, etc.
*   **Symptom Checker:** Triage integration (checking basic vitals logic).
