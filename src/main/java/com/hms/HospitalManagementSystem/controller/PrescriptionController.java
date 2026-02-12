package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.request.PrescriptionRequest;
import com.hms.HospitalManagementSystem.dto.response.PrescriptionItemResponse;
import com.hms.HospitalManagementSystem.dto.response.PrescriptionResponse;
import com.hms.HospitalManagementSystem.entity.Prescription;
import com.hms.HospitalManagementSystem.entity.PrescriptionItem;
import com.hms.HospitalManagementSystem.entity.User;
import com.hms.HospitalManagementSystem.service.PrescriptionService;
import com.hms.HospitalManagementSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/encounters")
@RequiredArgsConstructor
public class PrescriptionController {

        private final PrescriptionService prescriptionService;
        private final UserService userService;

        @PostMapping("/{encounterId}/prescriptions")
        @PreAuthorize("hasAuthority('CMP_PRESCRIPTION_WRITE')")
        public ResponseEntity<PrescriptionResponse> savePrescription(
                        @PathVariable Long encounterId,
                        @RequestBody PrescriptionRequest request) {

                Long currentUserId = getCurrentUserId();

                // Validate that items exist and have required fields
                if (request.getItems() == null || request.getItems().isEmpty()) {
                        throw new IllegalArgumentException("Prescription must have at least one item");
                }

                // Map DTO items to Entity items
                // Note: PrescriptionService expects List<PrescriptionItem> which are entities.
                // It's better if Service took DTOs, but I'll map here.
                List<PrescriptionItem> items = request.getItems().stream()
                                .map(item -> {
                                        // Validate required fields
                                        if (item.getMedicineName() == null || item.getMedicineName().trim().isEmpty()) {
                                                throw new IllegalArgumentException(
                                                                "Medicine name is required for all prescription items");
                                        }
                                        if (item.getDosage() == null || item.getDosage().trim().isEmpty()) {
                                                throw new IllegalArgumentException(
                                                                "Dosage is required for all prescription items");
                                        }
                                        if (item.getFrequency() == null || item.getFrequency().trim().isEmpty()) {
                                                throw new IllegalArgumentException(
                                                                "Frequency is required for all prescription items");
                                        }
                                        if (item.getDuration() == null || item.getDuration().trim().isEmpty()) {
                                                throw new IllegalArgumentException(
                                                                "Duration is required for all prescription items");
                                        }

                                        return PrescriptionItem.builder()
                                                        .medicineName(item.getMedicineName())
                                                        .dosage(item.getDosage())
                                                        .frequency(item.getFrequency())
                                                        .duration(item.getDuration())
                                                        .build();
                                })
                                .collect(Collectors.toList());

                Prescription prescription = prescriptionService.savePrescription(
                                encounterId,
                                request.getNote(),
                                items,
                                currentUserId);
                return ResponseEntity.ok(mapToResponse(prescription));
        }

        @GetMapping("/{encounterId}/prescriptions")
        @PreAuthorize("hasAnyAuthority('CMP_PRESCRIPTION_READ', 'CMP_CONSULTATION_READ')")
        public ResponseEntity<PrescriptionResponse> getPrescription(@PathVariable Long encounterId) {
                Prescription prescription = prescriptionService.getPrescriptionByEncounterId(encounterId);
                return ResponseEntity.ok(mapToResponse(prescription));
        }

        private Long getCurrentUserId() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()) {
                        throw new RuntimeException("User not authenticated");
                }
                String username = authentication.getName();
                User user = userService.getUserByUsername(username);
                return user.getId();
        }

        private PrescriptionResponse mapToResponse(Prescription prescription) {
                return PrescriptionResponse.builder()
                                .id(prescription.getId())
                                .encounterId(prescription.getEncounter().getId())
                                .note(prescription.getNote())
                                .status(prescription.getStatus().name())
                                .issuedAt(prescription.getIssuedAt())
                                .items(prescription.getItems().stream()
                                                .map(item -> PrescriptionItemResponse.builder()
                                                                .id(item.getId())
                                                                .medicineName(item.getMedicineName())
                                                                .dosage(item.getDosage())
                                                                .frequency(item.getFrequency())
                                                                .duration(item.getDuration())
                                                                .build())
                                                .collect(Collectors.toList()))
                                .build();
        }
}
