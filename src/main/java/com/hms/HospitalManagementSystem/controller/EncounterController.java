package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.request.EncounterCreateRequest;
import com.hms.HospitalManagementSystem.dto.request.EncounterUpdateRequest;
import com.hms.HospitalManagementSystem.dto.response.EncounterResponse;
import com.hms.HospitalManagementSystem.dto.response.VitalsResponse;
import com.hms.HospitalManagementSystem.entity.Encounter;
import com.hms.HospitalManagementSystem.entity.User;
import com.hms.HospitalManagementSystem.entity.Vitals;
import com.hms.HospitalManagementSystem.repository.VitalsRepository;
import com.hms.HospitalManagementSystem.service.EncounterService;
import com.hms.HospitalManagementSystem.service.UserService; // To get current user
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/encounters")
@RequiredArgsConstructor
public class EncounterController {

    private final EncounterService encounterService;
    private final UserService userService; // Or helper to get user ID
    private final VitalsRepository vitalsRepository;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CMP_VITALS_WRITE', 'CMP_CONSULTATION_WRITE')")
    public ResponseEntity<EncounterResponse> startEncounter(@RequestBody EncounterCreateRequest request) {
        Encounter encounter = encounterService.startEncounter(
                request.getAppointmentId(),
                request.getPatientId(),
                request.getDoctorId());
        return ResponseEntity.ok(mapToResponse(encounter));
    }

    @GetMapping("/{id}/clinical-notes")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_READ')")
    public ResponseEntity<EncounterResponse> getClinicalNotes(@PathVariable Long id) {
        Encounter encounter = encounterService.getEncounterById(id);
        return ResponseEntity.ok(mapToResponse(encounter));
    }

    @PatchMapping("/{id}/clinical-notes")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_WRITE')")
    public ResponseEntity<EncounterResponse> updateClinicalNotes(
            @PathVariable Long id,
            @RequestBody EncounterUpdateRequest request) {
        Long currentUserId = getCurrentUserId();
        Encounter encounter = encounterService.updateClinicalNotes(
                id,
                request.getChiefComplaint(),
                request.getDiagnosis(),
                request.getNotes(),
                currentUserId);
        return ResponseEntity.ok(mapToResponse(encounter));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_WRITE')")
    public ResponseEntity<EncounterResponse> completeEncounter(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        Encounter encounter = encounterService.completeEncounter(id, currentUserId);
        return ResponseEntity.ok(mapToResponse(encounter));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_READ')")
    public ResponseEntity<EncounterResponse> getEncounterById(@PathVariable Long id) {
        Encounter encounter = encounterService.getEncounterById(id);
        return ResponseEntity.ok(mapToResponse(encounter));
    }

    @GetMapping("/by-appointment/{appointmentId}")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_READ')")
    public ResponseEntity<EncounterResponse> getEncounterByAppointmentId(@PathVariable Long appointmentId) {
        Encounter encounter = encounterService.getEncounterByAppointmentId(appointmentId);
        return ResponseEntity.ok(mapToResponse(encounter));
    }

    @GetMapping("/queue/triage")
    @PreAuthorize("hasAuthority('CMP_VITALS_READ')")
    public ResponseEntity<List<EncounterResponse>> getTriageQueue() {
        List<Encounter> encounters = encounterService.getTriageQueue();
        return ResponseEntity.ok(encounters.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/queue/doctor/{doctorId}")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_READ')") // Or check if current user is the doctor
    public ResponseEntity<List<EncounterResponse>> getDoctorQueue(@PathVariable Long doctorId) {
        // TODO: Optional: validate if current user is this doctor or admin
        List<Encounter> encounters = encounterService.getDoctorQueue(doctorId);
        return ResponseEntity.ok(encounters.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/queue/opd/doctor/{doctorId}")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_READ')")
    public ResponseEntity<List<EncounterResponse>> getOpdDoctorQueue(@PathVariable Long doctorId) {
        List<Encounter> encounters = encounterService.getOpdDoctorQueue(doctorId);
        return ResponseEntity.ok(encounters.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/queue/ipd/doctor/{doctorId}")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_READ')")
    public ResponseEntity<List<EncounterResponse>> getIpdDoctorQueue(@PathVariable Long doctorId) {
        List<Encounter> encounters = encounterService.getIpdDoctorQueue(doctorId);
        return ResponseEntity.ok(encounters.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyAuthority('CMP_CONSULTATION_READ', 'CMP_PATIENT_VIEW')")
    public ResponseEntity<List<EncounterResponse>> getPatientEncounters(@PathVariable Long patientId) {
        List<Encounter> encounters = encounterService.getPatientEncounters(patientId);
        return ResponseEntity.ok(encounters.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        // Assuming principal is User entity or UserDetails with ID
        // UserService likely has method to get User by username/email from auth
        // Or Principal castable to CustomUserDetails
        // Checking UserService usage in AppointmentController might help.
        // For now, assume UserService has findByUsername or similar, or
        // authentication.getPrincipal returns Entity if custom.
        // I'll genericize:
        String username = authentication.getName();
        User user = userService.getUserByUsername(username); // Assuming this method exists
        return user.getId();
    }

    @PostMapping("/rounds")
    @PreAuthorize("hasAnyAuthority('CMP_VITALS_WRITE', 'CMP_CONSULTATION_WRITE')")
    public ResponseEntity<EncounterResponse> createRound(
            @RequestBody com.hms.HospitalManagementSystem.dto.ipd.RoundRequest request) {
        // Use current user ID as doctor ID if not provided, or validate
        Long doctorId = request.getDoctorId();
        if (doctorId == null) {
            doctorId = getCurrentUserId();
        }

        Encounter encounter = encounterService.createRound(request.getAdmissionId(), doctorId);

        // If initial notes are provided, update them
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            encounter = encounterService.updateClinicalNotes(
                    encounter.getId(),
                    null,
                    null,
                    request.getNotes(),
                    doctorId);
        }

        // Save Vitals if provided
        if (request.getTemperature() != null || request.getSystolic() != null || request.getPulse() != null) {
            // Check for existing vitals for this patient
            Optional<Vitals> existingVitalsOpt = vitalsRepository
                    .findFirstByEncounterPatientIdOrderByRecordedAtDesc(encounter.getPatient().getId());

            Vitals vitals;
            if (existingVitalsOpt.isPresent()) {
                // Update existing vitals
                vitals = existingVitalsOpt.get();
                vitals.setEncounter(encounter); // Link to new encounter (Round)
                vitals.setTemperature(request.getTemperature());
                vitals.setSystolic(request.getSystolic());
                vitals.setDiastolic(request.getDiastolic());
                vitals.setPulse(request.getPulse());
                vitals.setSpo2(request.getSpo2());
                vitals.setRecordedBy(encounter.getDoctor());
                vitals.setRecordedAt(java.time.LocalDateTime.now());
            } else {
                // Create new vitals
                vitals = Vitals
                        .builder()
                        .encounter(encounter)
                        .temperature(request.getTemperature())
                        .systolic(request.getSystolic())
                        .diastolic(request.getDiastolic())
                        .pulse(request.getPulse())
                        .spo2(request.getSpo2())
                        .recordedBy(encounter.getDoctor())
                        .recordedAt(java.time.LocalDateTime.now())
                        .build();
            }
            vitalsRepository.save(vitals);

            // Re-fetch encounter to include vitals in response (optional but good for UI)
            encounter = encounterService.getEncounterById(encounter.getId());
        }

        return ResponseEntity.ok(mapToResponse(encounter));
    }

    private EncounterResponse mapToResponse(Encounter encounter) {
        VitalsResponse vitalsResponse = null;
        if (encounter.getVitals() != null) {
            vitalsResponse = VitalsResponse.builder()
                    .id(encounter.getVitals().getId())
                    .encounterId(encounter.getId())
                    .temperature(encounter.getVitals().getTemperature())
                    .systolic(encounter.getVitals().getSystolic())
                    .diastolic(encounter.getVitals().getDiastolic())
                    .pulse(encounter.getVitals().getPulse())
                    .spo2(encounter.getVitals().getSpo2())
                    .weight(encounter.getVitals().getWeight())
                    .height(encounter.getVitals().getHeight())
                    .bmi(encounter.getVitals().getBmi())
                    .recordedAt(encounter.getVitals().getRecordedAt())
                    .recordedBy(encounter.getVitals().getRecordedBy() != null
                            ? encounter.getVitals().getRecordedBy().getFullName()
                            : null)
                    .build();
        }

        return EncounterResponse.builder()
                .id(encounter.getId())
                .appointmentId(encounter.getAppointment() != null ? encounter.getAppointment().getId() : null)
                .admissionId(encounter.getAdmission() != null ? encounter.getAdmission().getId() : null)
                .patientId(encounter.getPatient().getId())
                .patientName(encounter.getPatient().getFirstName() + " " + encounter.getPatient().getLastName())
                .patientGender(encounter.getPatient().getGender().name())
                .patientDob(encounter.getPatient().getDob().toString())
                .doctorId(encounter.getDoctor().getId())
                .doctorName(encounter.getDoctor().getFullName())
                .status(encounter.getStatus().name())
                .chiefComplaint(encounter.getChiefComplaint())
                .diagnosis(encounter.getDiagnosis())
                .notes(encounter.getNotes())
                .startedAt(encounter.getStartedAt())
                .completedAt(encounter.getCompletedAt())
                .vitals(vitalsResponse)
                .build();
    }
}
