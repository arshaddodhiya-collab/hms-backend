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
import jakarta.validation.Valid;

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
                        @Valid @RequestBody PrescriptionRequest request) {

                Long currentUserId = getCurrentUserId();

                // Map DTO items to Entity items
                List<PrescriptionItem> items = request.getItems().stream()
                                .map(item -> PrescriptionItem.builder()
                                                .medicineName(item.getMedicineName())
                                                .dosage(item.getDosage())
                                                .frequency(item.getFrequency())
                                                .duration(item.getDuration())
                                                .build())
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
                if (authentication == null || authentication.getName() == null) {
                        throw new org.springframework.security.authentication.BadCredentialsException(
                                        "User not authenticated");
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
