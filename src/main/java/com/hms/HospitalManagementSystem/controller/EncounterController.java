package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.request.EncounterCreateRequest;
import com.hms.HospitalManagementSystem.dto.request.EncounterUpdateRequest;
import com.hms.HospitalManagementSystem.dto.response.EncounterResponse;
import com.hms.HospitalManagementSystem.dto.response.VitalsResponse;
import com.hms.HospitalManagementSystem.dto.response.RoundResponse;
import com.hms.HospitalManagementSystem.dto.ipd.RoundRequest;
import com.hms.HospitalManagementSystem.entity.Encounter;
import com.hms.HospitalManagementSystem.entity.User;
import com.hms.HospitalManagementSystem.entity.Vitals;
import com.hms.HospitalManagementSystem.entity.Round;

import com.hms.HospitalManagementSystem.service.EncounterService;
import com.hms.HospitalManagementSystem.service.UserService; // To get current user
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/encounters")
@RequiredArgsConstructor
public class EncounterController {

    private final EncounterService encounterService;
    private final UserService userService; // Or helper to get user ID
    // private final VitalsRepository vitalsRepository;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CMP_VITALS_WRITE', 'CMP_CONSULTATION_WRITE')")
    public ResponseEntity<EncounterResponse> startEncounter(@Valid @RequestBody EncounterCreateRequest request) {
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
            @Valid @RequestBody EncounterUpdateRequest request) {
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

    @PostMapping("/rounds")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_WRITE')")
    public ResponseEntity<RoundResponse> addRound(
            @Valid @RequestBody RoundRequest request) {
        Long currentUserId = getCurrentUserId();
        Round round = encounterService.addRound(request, currentUserId);

        return ResponseEntity.ok(RoundResponse.builder()
                .id(round.getId())
                .encounterId(round.getEncounter().getId())
                .doctorId(round.getDoctor().getId())
                .doctorName(round.getDoctor().getFullName())
                .notes(round.getNotes())
                .createdAt(round.getCreatedAt())
                .updatedAt(round.getUpdatedAt())
                .build());
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
        if (authentication == null || authentication.getName() == null) {
            throw new org.springframework.security.authentication.BadCredentialsException("User not authenticated");
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

    private EncounterResponse mapToResponse(Encounter encounter) {
        VitalsResponse vitalsResponse = null;
        if (encounter.getVitals() != null) {
            vitalsResponse = mapToVitalsResponse(encounter.getVitals());
        }

        List<VitalsResponse> vitalsHistory = encounter.getVitalsHistory().stream()
                .map(this::mapToVitalsResponse)
                .collect(Collectors.toList());

        List<RoundResponse> roundResponses = encounter.getRounds().stream()
                .map(this::mapToRoundResponse)
                .collect(Collectors.toList());

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
                .rounds(roundResponses)
                .vitalsHistory(vitalsHistory)
                .build();
    }

    private VitalsResponse mapToVitalsResponse(Vitals vitals) {
        return VitalsResponse.builder()
                .id(vitals.getId())
                .encounterId(vitals.getEncounter().getId())
                .temperature(vitals.getTemperature())
                .systolic(vitals.getSystolic())
                .diastolic(vitals.getDiastolic())
                .pulse(vitals.getPulse())
                .spo2(vitals.getSpo2())
                .weight(vitals.getWeight())
                .height(vitals.getHeight())
                .bmi(vitals.getBmi())
                .recordedAt(vitals.getRecordedAt())
                .recordedBy(vitals.getRecordedBy() != null
                        ? vitals.getRecordedBy().getFullName()
                        : null)
                .build();
    }

    private RoundResponse mapToRoundResponse(Round round) {
        return RoundResponse.builder()
                .id(round.getId())
                .encounterId(round.getEncounter().getId())
                .doctorId(round.getDoctor().getId())
                .doctorName(round.getDoctor().getFullName())
                .notes(round.getNotes())
                .createdAt(round.getCreatedAt())
                .updatedAt(round.getUpdatedAt())
                .build();
    }
}
