package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.request.EncounterCreateRequest;
import com.hms.HospitalManagementSystem.dto.request.EncounterUpdateRequest;
import com.hms.HospitalManagementSystem.dto.response.EncounterResponse;
// import com.hms.HospitalManagementSystem.dto.response.VitalsResponse;
import com.hms.HospitalManagementSystem.dto.response.RoundResponse;
import com.hms.HospitalManagementSystem.dto.ipd.RoundRequest;
import com.hms.HospitalManagementSystem.entity.User;

import com.hms.HospitalManagementSystem.service.EncounterService;
import com.hms.HospitalManagementSystem.service.UserService; // To get current user
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

// import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/v1/encounters")
@RequiredArgsConstructor
public class EncounterController {

    private final EncounterService encounterService;
    private final UserService userService; // Or helper to get user ID

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CMP_VITALS_WRITE', 'CMP_CONSULTATION_WRITE')")
    public ResponseEntity<EncounterResponse> startEncounter(@Valid @RequestBody EncounterCreateRequest request) {
        EncounterResponse encounter = encounterService.startEncounter(
                request.getAppointmentId(),
                request.getPatientId(),
                request.getDoctorId());
        return ResponseEntity.ok(encounter);
    }

    @GetMapping("/{id}/clinical-notes")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_READ')")
    public ResponseEntity<EncounterResponse> getClinicalNotes(@PathVariable Long id) {
        EncounterResponse encounter = encounterService.getEncounterById(id);
        return ResponseEntity.ok(encounter);
    }

    @PatchMapping("/{id}/clinical-notes")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_WRITE')")
    public ResponseEntity<EncounterResponse> updateClinicalNotes(
            @PathVariable Long id,
            @Valid @RequestBody EncounterUpdateRequest request) {
        Long currentUserId = getCurrentUserId();
        EncounterResponse encounter = encounterService.updateClinicalNotes(
                id,
                request.getChiefComplaint(),
                request.getDiagnosis(),
                request.getNotes(),
                currentUserId);
        return ResponseEntity.ok(encounter);
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_WRITE')")
    public ResponseEntity<EncounterResponse> completeEncounter(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        EncounterResponse encounter = encounterService.completeEncounter(id, currentUserId);
        return ResponseEntity.ok(encounter);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_READ')")
    public ResponseEntity<EncounterResponse> getEncounterById(@PathVariable Long id) {
        EncounterResponse encounter = encounterService.getEncounterById(id);
        return ResponseEntity.ok(encounter);
    }

    @GetMapping("/by-appointment/{appointmentId}")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_READ')")
    public ResponseEntity<EncounterResponse> getEncounterByAppointmentId(@PathVariable Long appointmentId) {
        EncounterResponse encounter = encounterService.getEncounterByAppointmentId(appointmentId);
        return ResponseEntity.ok(encounter);
    }

    @GetMapping("/queue/triage")
    @PreAuthorize("hasAuthority('CMP_VITALS_READ')")
    public ResponseEntity<Slice<EncounterResponse>> getTriageQueue(
            @PageableDefault(sort = "startedAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Slice<EncounterResponse> encounters = encounterService.getTriageQueue(pageable);
        return ResponseEntity.ok(encounters);
    }

    @GetMapping("/queue/doctor/{doctorId}")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_READ')") // Or check if current user is the doctor
    public ResponseEntity<Slice<EncounterResponse>> getDoctorQueue(
            @PathVariable Long doctorId,
            @PageableDefault(sort = "startedAt", direction = Sort.Direction.ASC) Pageable pageable) {
        // TODO: Optional: validate if current user is this doctor or admin
        Slice<EncounterResponse> encounters = encounterService.getDoctorQueue(doctorId, pageable);
        return ResponseEntity.ok(encounters);
    }

    @GetMapping("/queue/opd/doctor/{doctorId}")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_READ')")
    public ResponseEntity<Slice<EncounterResponse>> getOpdDoctorQueue(
            @PathVariable Long doctorId,
            @PageableDefault(sort = "startedAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Slice<EncounterResponse> encounters = encounterService.getOpdDoctorQueue(doctorId, pageable);
        return ResponseEntity.ok(encounters);
    }

    @GetMapping("/queue/ipd/doctor/{doctorId}")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_READ')")
    public ResponseEntity<Slice<EncounterResponse>> getIpdDoctorQueue(
            @PathVariable Long doctorId,
            @PageableDefault(sort = "startedAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Slice<EncounterResponse> encounters = encounterService.getIpdDoctorQueue(doctorId, pageable);
        return ResponseEntity.ok(encounters);
    }

    @PostMapping("/rounds")
    @PreAuthorize("hasAuthority('CMP_CONSULTATION_WRITE')")
    public ResponseEntity<RoundResponse> addRound(
            @Valid @RequestBody RoundRequest request) {
        Long currentUserId = getCurrentUserId();
        RoundResponse round = encounterService.addRound(request, currentUserId);

        return ResponseEntity.ok(round);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyAuthority('CMP_CONSULTATION_READ', 'CMP_PATIENT_VIEW')")
    public ResponseEntity<Slice<EncounterResponse>> getPatientEncounters(
            @PathVariable Long patientId,
            @PageableDefault(sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Slice<EncounterResponse> encounters = encounterService.getPatientEncounters(patientId, pageable);
        return ResponseEntity.ok(encounters);
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
}
