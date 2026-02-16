package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.ipd.WardRequest;
import com.hms.HospitalManagementSystem.dto.ipd.WardResponse;
import com.hms.HospitalManagementSystem.service.WardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ipd/wards")
@RequiredArgsConstructor
public class WardController {

    private final WardService wardService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MOD_ADMIN', 'CMP_ADMIN_DEPT_WRITE')")
    public ResponseEntity<WardResponse> createWard(@Valid @RequestBody WardRequest request) {
        return new ResponseEntity<>(wardService.createWard(request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MOD_ADMIN', 'MOD_PATIENTS', 'MOD_APPOINTMENTS', 'MOD_TRIAGE', 'MOD_CONSULTATION', 'MOD_LAB', 'MOD_BILLING', 'MOD_DASHBOARD')")
    public ResponseEntity<List<WardResponse>> getAllWards() {
        return ResponseEntity.ok(wardService.getAllWards());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MOD_ADMIN', 'MOD_PATIENTS', 'MOD_APPOINTMENTS', 'MOD_TRIAGE', 'MOD_CONSULTATION', 'MOD_LAB', 'MOD_BILLING', 'MOD_DASHBOARD')")
    public ResponseEntity<WardResponse> getWardById(@PathVariable Long id) {
        return ResponseEntity.ok(wardService.getWardById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MOD_ADMIN', 'CMP_ADMIN_DEPT_WRITE')")
    public ResponseEntity<WardResponse> updateWard(@PathVariable Long id, @Valid @RequestBody WardRequest request) {
        return ResponseEntity.ok(wardService.updateWard(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MOD_ADMIN', 'CMP_ADMIN_DEPT_WRITE')")
    public ResponseEntity<Void> deleteWard(@PathVariable Long id) {
        wardService.deleteWard(id);
        return ResponseEntity.noContent().build();
    }
}
