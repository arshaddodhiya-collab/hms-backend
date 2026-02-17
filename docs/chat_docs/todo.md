# Chat Module Implementation Todo List

Based on [PRD: Chat-Based Patient Data Access System](chat_patient_data_access_prd.md).

## Phase 1: Foundation & Entity Layer
- [ ] **Data Model**
    - [ ] Create `ChatMessage` entity `src/main/java/com/hms/HospitalManagementSystem/entity/ChatMessage.java`
        - [ ] Fields: `id`, `patient` (User), `content`, `sender` (ENUM), `type` (ENUM), `timestamp`
    - [ ] Create `SenderType` Enum (USER, BOT)
    - [ ] Create `MessageType` Enum (TEXT, CARD, LINK)
    - [ ] Create `ChatMessageRepository` interface

- [ ] **DTOs**
    - [ ] Create `ChatMessageRequest` (content)
    - [ ] Create `ChatMessageResponse` (id, content, sender, timestamp, attachments?)

## Phase 2: Core Logic & Service Layer
- [ ] **Service Skeleton**
    - [ ] Create `ChatService` interface and implementation
    - [ ] Implement `sendMessage(User patient, String content)`

- [ ] **Intent Parsing (Regex/Simple)**
    - [ ] Implement `IntentParser` component
    - [ ] Define patterns for:
        - [ ] Appointments (`/(appoint|visit|schedule|doc)/i`)
        - [ ] Lab Results (`/(lab|result|blood|report)/i`)
        - [ ] Prescriptions (`/(pill|med|drug|presc)/i`)
        - [ ] Billing (`/(bill|invoice|payment)/i`)

- [ ] **Module Integration**
    - [ ] Integrate `AppointmentService` to fetch upcoming appointments
    - [ ] Integrate `LabService` to fetch recent results
    - [ ] Integrate `BillingService` to fetch outstanding invoices

## Phase 3: API & Security
- [ ] **Controller**
    - [ ] Create `ChatController`
    - [ ] Endpoint: `POST /api/v1/chat/send` (User sends message)
    - [ ] Endpoint: `GET /api/v1/chat/history` (Load past messages)

- [ ] **Security**
    - [ ] Ensure endpoints are secured with `PreAuthorize("hasRole('PATIENT')")`
    - [ ] Validate `patient_id` from SecurityContext, not request body

## Phase 4: UI & Edge Cases
- [ ] **Response Formatting**
    - [ ] Implement structured response builder (Text vs Cards)
    - [ ] Handle "No Data" scenarios gracefully
    - [ ] Handle "Unknown Intent" with a Help menu

## Phase 5: Testing & Documentation
- [ ] **Unit Tests**
    - [ ] Test `IntentParser` with various phrases
    - [ ] Test `ChatService` flow

- [ ] **Integration Tests**
    - [ ] Test full API flow with MockMVC
