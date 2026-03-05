# Add Slice-Based Pagination to All List Endpoints

Add `Slice<>` pagination (with `Pageable`) to all 19 list-returning endpoints across 11 modules. Uses `Slice<>` instead of `Page<>` for performance (avoids `COUNT(*)` queries).

## Proposed Changes

The same pattern is applied to every module across 4 layers:

**Pattern for each list endpoint:**
1. **Repository** — Add/modify methods to accept `Pageable` and return `Slice<>`
2. **Service Interface** — Change `List<T>` → `Slice<T>` and add `Pageable` parameter
3. **Service Impl** — Pass `Pageable` to repository calls
4. **Controller** — Add `@PageableDefault` + `Pageable` parameter, change return type

> [!IMPORTANT]
> **DashboardServiceImpl** calls some repository methods internally ([findTop5](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/repository/LabRequestRepository.java#34-35), etc.) — these remain unchanged. Only the externally-exposed list endpoints get paginated.

> [!NOTE]
> The `AppointmentController.mapToResponse()` inline mapping will be applied via `Slice.map()` instead of stream + collect.

---

### Appointment Module

#### [MODIFY] [AppointmentRepository.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/repository/AppointmentRepository.java)
Add paginated versions of: [findByDoctorIdAndStartDateTimeBetweenAndDeletedFalse](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/repository/AppointmentRepository.java#41-43), [findByStartDateTimeBetweenAndDeletedFalse](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/repository/AppointmentRepository.java#44-45), [findByPatientId](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/repository/InvoiceRepository.java#18-19), [findByPatientIdAndStatus](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/repository/AppointmentRepository.java#39-40). Keep existing `List<>` versions for [DashboardServiceImpl](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/impl/DashboardServiceImpl.java#23-128) and `AppointmentServiceImpl.completeActiveAppointmentsForPatient()`.

#### [MODIFY] [AppointmentService.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/AppointmentService.java)
Change: [getAllAppointments](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/impl/AppointmentServiceImpl.java#264-268), [getAppointmentsByDate](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/AppointmentService.java#33-34), [getDoctorAppointments](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/AppointmentService.java#29-30), [getPatientAppointments](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/impl/AppointmentServiceImpl.java#280-284), [getPatientAppointmentsByStatus](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/impl/AppointmentServiceImpl.java#285-289), [getUpcomingAppointmentsForDoctor](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/AppointmentService.java#43-44) to accept `Pageable` and return `Slice<>`.

#### [MODIFY] [AppointmentServiceImpl.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/impl/AppointmentServiceImpl.java)
Update methods to delegate pageable to repository. Keep [completeActiveAppointmentsForPatient](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/impl/AppointmentServiceImpl.java#290-308) using `List<>` (it's an internal batch operation, not an API endpoint).

#### [MODIFY] [AppointmentController.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/controller/AppointmentController.java)
Add `Pageable` param with `@PageableDefault(sort = "createdAt", direction = DESC)` to 5 list endpoints. Change return types to `Slice<AppointmentResponse>`. Use `slice.map(this::mapToResponse)`.

---

### Billing Module

#### [MODIFY] [InvoiceRepository.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/repository/InvoiceRepository.java)
Add: `Slice<Invoice> findByPatientIdAndStatusIn(Long, List<InvoiceStatus>, Pageable)` and paginated `findAll`.

#### [MODIFY] [BillingService.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/BillingService.java)
Change: [getOutstandingInvoices](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/impl/BillingServiceImpl.java#279-287), [getAllInvoices](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/BillingService.java#25-26) to accept `Pageable` and return `Slice<>`.

#### [MODIFY] [BillingServiceImpl.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/impl/BillingServiceImpl.java)
Update methods to pass pageable to repository.

#### [MODIFY] [BillingController.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/controller/BillingController.java)
Add `Pageable` param to 2 list endpoints.

---

### Encounter Module

#### [MODIFY] [EncounterRepository.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/repository/EncounterRepository.java)
Add paginated versions of: [findByStatus](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/repository/InvoiceRepository.java#20-21), [findByDoctorIdAndStatusIn](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/repository/EncounterRepository.java#23-25), [findByDoctorIdAndAppointmentIsNotNullAndStatusIn](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/repository/EncounterRepository.java#26-27), [findByDoctorIdAndAdmissionIsNotNullAndStatusIn](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/repository/EncounterRepository.java#28-29), [findByPatientId](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/repository/InvoiceRepository.java#18-19).

#### [MODIFY] [EncounterService.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/EncounterService.java)
Change: [getTriageQueue](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/impl/EncounterServiceImpl.java#175-180), [getDoctorQueue](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/impl/EncounterServiceImpl.java#181-191), [getOpdDoctorQueue](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/controller/EncounterController.java#100-106), [getIpdDoctorQueue](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/impl/EncounterServiceImpl.java#250-261), [getPatientEncounters](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/EncounterService.java#27-28) to accept `Pageable`.

#### [MODIFY] [EncounterServiceImpl.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/impl/EncounterServiceImpl.java)
Update 5 methods.

#### [MODIFY] [EncounterController.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/controller/EncounterController.java)
Add `Pageable` to 5 list endpoints.

---

### Lab Module

#### [MODIFY] [LabRequestRepository.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/repository/LabRequestRepository.java)
Add: `Slice<LabRequest> findByStatusIn(List<LabRequestStatus>, Pageable)`. Keep `List<>` version for DashboardServiceImpl.

#### [MODIFY] [LabService.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/LabService.java)
Change: [getAllLabTests](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/impl/LabServiceImpl.java#52-56), [getLabQueue](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/LabService.java#19-20), [getRequestsByEncounter](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/LabService.java#23-24) to accept `Pageable`.

#### [MODIFY] [LabServiceImpl.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/impl/LabServiceImpl.java)
Update methods.

#### [MODIFY] [LabController.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/controller/LabController.java)
Add `Pageable` to 2 list endpoints.

---

### Admission Module

#### [MODIFY] [AdmissionRepository.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/repository/AdmissionRepository.java)
Add: `Slice<Admission> findByStatus(AdmissionStatus, Pageable)`.

#### [MODIFY] [AdmissionService.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/AdmissionService.java) / [AdmissionServiceImpl.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/impl/AdmissionServiceImpl.java) / [AdmissionController.java](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/controller/AdmissionController.java)
Change [getActiveAdmissions](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/AdmissionService.java#17-18) to accept `Pageable` and return `Slice<>`.

---

### Medium & Low Priority (same pattern)

**ChargeCatalog**: [getAllCharges](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/ChargeCatalogService.java#11-12) → paginated  
**User**: [getAllUsers](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/controller/UserController.java#20-25), [getUsersByRole](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/UserService.java#15-16) → paginated  
**Department**: [getAllDepartments](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/DepartmentService.java#8-9) → paginated  
**Ward**: [getAllWards](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/controller/WardController.java#28-33) → paginated  
**Bed**: [getAllBeds](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/BedService.java#14-15), [getAvailableBeds](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/controller/BedController.java#29-36) → paginated  
**Role**: [getAllRoles](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/service/RoleService.java#10-11), [getAllPermissions](file:///home/artem/test/hms-final/hms-backend/src/main/java/com/hms/HospitalManagementSystem/controller/RoleController.java#27-32) → paginated  

Each follows: Repository → Service → Impl → Controller.

---

## Verification Plan

### Automated Tests

1. **Compile check**: `mvn compile -f /home/artem/test/hms-final/hms-backend/pom.xml`
2. **Run existing tests**: `mvn test -f /home/artem/test/hms-final/hms-backend/pom.xml`
   - Tests focus on non-list operations (book, admit, start/complete) so should still pass
   - If any test references the changed method signatures, they'll need updates
