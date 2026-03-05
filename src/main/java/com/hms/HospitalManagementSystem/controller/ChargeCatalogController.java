package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.request.ChargeCatalogRequest;
import com.hms.HospitalManagementSystem.dto.response.ChargeCatalogResponse;
import com.hms.HospitalManagementSystem.service.ChargeCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/charge-catalog")
@RequiredArgsConstructor
public class ChargeCatalogController {

    private final ChargeCatalogService chargeCatalogService;

    @PostMapping
    @PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_WRITE')") // Adjust permission as needed
    public ResponseEntity<ChargeCatalogResponse> createCharge(@Valid @RequestBody ChargeCatalogRequest request) {
        return new ResponseEntity<>(chargeCatalogService.createCharge(request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ACT_VIEW')")
    public ResponseEntity<org.springframework.data.domain.Slice<ChargeCatalogResponse>> getAllCharges(
            @org.springframework.data.web.PageableDefault(sort = "name", direction = org.springframework.data.domain.Sort.Direction.ASC) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(chargeCatalogService.getAllCharges(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACT_VIEW')")
    public ResponseEntity<ChargeCatalogResponse> getChargeById(@PathVariable Long id) {
        return ResponseEntity.ok(chargeCatalogService.getChargeById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_WRITE')")
    public ResponseEntity<ChargeCatalogResponse> updateCharge(@PathVariable Long id,
            @Valid @RequestBody ChargeCatalogRequest request) {
        return ResponseEntity.ok(chargeCatalogService.updateCharge(id, request));
    }
}
