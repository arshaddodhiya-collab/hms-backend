package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.request.VitalsRequest;
import com.hms.HospitalManagementSystem.dto.response.VitalsResponse;
import com.hms.HospitalManagementSystem.entity.User;
import com.hms.HospitalManagementSystem.entity.Vitals;
import com.hms.HospitalManagementSystem.service.UserService;
import com.hms.HospitalManagementSystem.service.VitalsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/encounters")
@RequiredArgsConstructor
public class VitalsController {

    private final VitalsService vitalsService;
    private final UserService userService;

    @PostMapping("/{encounterId}/vitals")
    @PreAuthorize("hasAnyAuthority('CMP_VITALS_WRITE', 'CMP_CONSULTATION_WRITE')")
    public ResponseEntity<VitalsResponse> saveVitals(
            @PathVariable Long encounterId,
            @Valid @RequestBody VitalsRequest request) {

        Long currentUserId = getCurrentUserId();

        // Map DTO to Entity
        Vitals vitalsData = Vitals.builder()
                .temperature(request.getTemperature())
                .systolic(request.getSystolic())
                .diastolic(request.getDiastolic())
                .pulse(request.getPulse())
                .spo2(request.getSpo2())
                .weight(request.getWeight())
                .height(request.getHeight())
                .build();

        Vitals savedVitals = vitalsService.saveVitals(encounterId, vitalsData, currentUserId);
        return ResponseEntity.ok(mapToResponse(savedVitals));
    }

    @GetMapping("/{encounterId}/vitals")
    @PreAuthorize("hasAnyAuthority('CMP_VITALS_READ', 'CMP_CONSULTATION_READ')")
    public ResponseEntity<VitalsResponse> getVitals(@PathVariable Long encounterId) {
        Vitals vitals = vitalsService.getVitalsByEncounterId(encounterId);
        return ResponseEntity.ok(mapToResponse(vitals));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new org.springframework.security.authentication.BadCredentialsException("User not authenticated");
        }
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        return user.getId();
    }

    private VitalsResponse mapToResponse(Vitals vitals) {
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
                .recordedBy(vitals.getRecordedBy().getFullName())
                .build();
    }
}
