package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.ipd.AdmissionRequest;
import com.hms.HospitalManagementSystem.dto.ipd.AdmissionResponse;
import com.hms.HospitalManagementSystem.dto.ipd.DischargeRequest;
import com.hms.HospitalManagementSystem.dto.ipd.TransferRequest;
import com.hms.HospitalManagementSystem.service.AdmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ipd/admissions")
@RequiredArgsConstructor
public class AdmissionController {

    private final AdmissionService admissionService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MOD_ADMIN', 'MOD_PATIENTS', 'MOD_APPOINTMENTS', 'MOD_TRIAGE', 'MOD_CONSULTATION')") // Receptionist,
                                                                                                                        // Doctor,
                                                                                                                        // Nurse
    public ResponseEntity<AdmissionResponse> admitPatient(@Valid @RequestBody AdmissionRequest request) {
        return new ResponseEntity<>(admissionService.admitPatient(request), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/discharge")
    @PreAuthorize("hasAnyAuthority('MOD_ADMIN', 'MOD_CONSULTATION')") // Doctor, Admin
    public ResponseEntity<AdmissionResponse> dischargePatient(
            @PathVariable Long id,
            @Valid @RequestBody DischargeRequest request) {
        return ResponseEntity.ok(admissionService.dischargePatient(id, request));
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAnyAuthority('MOD_ADMIN', 'MOD_TRIAGE', 'MOD_CONSULTATION')") // Nurse, Doctor, Admin
    public ResponseEntity<AdmissionResponse> transferPatient(
            @PathVariable Long id,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(admissionService.transferPatient(id, request));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyAuthority('MOD_ADMIN', 'MOD_PATIENTS', 'MOD_APPOINTMENTS', 'MOD_TRIAGE', 'MOD_CONSULTATION', 'MOD_LAB', 'MOD_BILLING', 'MOD_DASHBOARD')")
    public ResponseEntity<org.springframework.data.domain.Slice<AdmissionResponse>> getActiveAdmissions(
            @org.springframework.data.web.PageableDefault(sort = "admissionDate", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(admissionService.getActiveAdmissions(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MOD_ADMIN', 'MOD_PATIENTS', 'MOD_APPOINTMENTS', 'MOD_TRIAGE', 'MOD_CONSULTATION', 'MOD_LAB', 'MOD_BILLING', 'MOD_DASHBOARD')")
    public ResponseEntity<AdmissionResponse> getAdmissionById(@PathVariable Long id) {
        return ResponseEntity.ok(admissionService.getAdmissionById(id));
    }
}
