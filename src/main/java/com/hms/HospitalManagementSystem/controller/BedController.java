package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.ipd.BedRequest;
import com.hms.HospitalManagementSystem.dto.ipd.BedResponse;
import com.hms.HospitalManagementSystem.enums.BedType;
import com.hms.HospitalManagementSystem.service.BedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ipd/beds")
@RequiredArgsConstructor
public class BedController {

    private final BedService bedService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MOD_ADMIN', 'CMP_ADMIN_DEPT_WRITE')")
    public ResponseEntity<BedResponse> createBed(@Valid @RequestBody BedRequest request) {
        return new ResponseEntity<>(bedService.createBed(request), HttpStatus.CREATED);
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyAuthority('MOD_ADMIN', 'MOD_PATIENTS', 'MOD_APPOINTMENTS', 'MOD_TRIAGE', 'MOD_CONSULTATION', 'MOD_LAB', 'MOD_BILLING', 'MOD_DASHBOARD')")
    public ResponseEntity<org.springframework.data.domain.Slice<BedResponse>> getAvailableBeds(
            @RequestParam Long wardId,
            @RequestParam(required = false) BedType type,
            @org.springframework.data.web.PageableDefault(sort = "bedNumber", direction = org.springframework.data.domain.Sort.Direction.ASC) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(bedService.getAvailableBeds(wardId, type, pageable));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MOD_ADMIN', 'MOD_PATIENTS', 'MOD_APPOINTMENTS', 'MOD_TRIAGE', 'MOD_CONSULTATION', 'MOD_LAB', 'MOD_BILLING', 'MOD_DASHBOARD')")
    public ResponseEntity<org.springframework.data.domain.Slice<BedResponse>> getAllBeds(
            @org.springframework.data.web.PageableDefault(sort = "bedNumber", direction = org.springframework.data.domain.Sort.Direction.ASC) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(bedService.getAllBeds(pageable));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('MOD_ADMIN', 'CMP_ADMIN_DEPT_WRITE', 'MOD_TRIAGE')")
    public ResponseEntity<BedResponse> updateBedStatus(@PathVariable Long id, @RequestParam boolean isActive) {
        return ResponseEntity.ok(bedService.updateBedStatus(id, isActive));
    }
}
