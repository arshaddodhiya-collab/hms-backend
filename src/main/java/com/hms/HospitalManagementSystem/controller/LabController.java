package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.request.LabRequestCreateRequest;
import com.hms.HospitalManagementSystem.dto.request.LabResultRequest;
import com.hms.HospitalManagementSystem.dto.request.LabTestRequest;
import com.hms.HospitalManagementSystem.dto.response.LabRequestResponse;
import com.hms.HospitalManagementSystem.dto.response.LabResultResponse;
import com.hms.HospitalManagementSystem.entity.LabRequest;
import com.hms.HospitalManagementSystem.entity.LabResult;
import com.hms.HospitalManagementSystem.entity.LabTestCatalog;
import com.hms.HospitalManagementSystem.enums.LabRequestStatus;
import com.hms.HospitalManagementSystem.service.LabService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LabController {

    private final LabService labService;
    private final com.hms.HospitalManagementSystem.service.ReportService reportService;

    // --- Lab Test Catalog Endpoints ---

    @PostMapping("/lab-tests")
    @PreAuthorize("hasAnyAuthority('MOD_ADMIN', 'MOD_LAB')")
    public ResponseEntity<LabTestCatalog> createLabTest(@Valid @RequestBody LabTestRequest request) {
        LabTestCatalog createdTest = labService.createLabTest(request);
        return ResponseEntity.ok(createdTest);
    }

    @GetMapping("/lab-tests")
    @PreAuthorize("hasAnyAuthority('CMP_LAB_READ', 'CMP_CONSULTATION_READ', 'CMP_LAB_ENTRY')")
    public ResponseEntity<List<LabTestCatalog>> getAllLabTests() {
        return ResponseEntity.ok(labService.getAllLabTests());
    }

    // --- Lab Request Endpoints ---

    @PostMapping("/lab-requests")
    @PreAuthorize("hasAnyAuthority('CMP_LAB_ENTRY', 'CMP_CONSULTATION_WRITE')")
    public ResponseEntity<LabRequestResponse> createLabRequest(@Valid @RequestBody LabRequestCreateRequest request) {
        LabRequest labRequest = labService.createLabRequest(request);
        return ResponseEntity.ok(mapToResponse(labRequest));
    }

    @GetMapping("/lab-requests")
    @PreAuthorize("hasAnyAuthority('CMP_LAB_READ', 'CMP_LAB_ENTRY')")
    public ResponseEntity<List<LabRequestResponse>> getLabQueue(
            @RequestParam(required = false) List<LabRequestStatus> status,
            @RequestParam(required = false) Long encounterId) {

        List<LabRequest> requests;
        if (encounterId != null) {
            requests = labService.getRequestsByEncounter(encounterId);
        } else if (status != null && !status.isEmpty()) {
            requests = labService.getLabQueue(status);
        } else {
            // Default to active requests if no status provided? Or all?
            // Let's return all for now or ORDERED/SAMPLED/COMPLETED.
            requests = labService.getLabQueue(List.of(LabRequestStatus.values()));
        }

        return ResponseEntity.ok(requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/lab-requests/{id}")
    @PreAuthorize("hasAnyAuthority('CMP_LAB_READ', 'CMP_LAB_ENTRY', 'CMP_CONSULTATION_READ')")
    public ResponseEntity<LabRequestResponse> getLabRequestById(@PathVariable Long id) {
        LabRequest labRequest = labService.getLabRequestById(id);
        return ResponseEntity.ok(mapToResponse(labRequest));
    }

    @PatchMapping("/lab-requests/{id}/status")
    @PreAuthorize("hasAnyAuthority('CMP_LAB_ENTRY')")
    public ResponseEntity<LabRequestResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam LabRequestStatus status) {
        LabRequest labRequest = labService.updateStatus(id, status);
        return ResponseEntity.ok(mapToResponse(labRequest));
    }

    @PostMapping("/lab-requests/{id}/results")
    @PreAuthorize("hasAnyAuthority('CMP_LAB_ENTRY')")
    public ResponseEntity<LabRequestResponse> addLabResults(
            @PathVariable Long id,
            @Valid @RequestBody List<LabResultRequest> resultRequests) {
        LabRequest labRequest = labService.addLabResults(id, resultRequests);
        return ResponseEntity.ok(mapToResponse(labRequest));
    }

    @GetMapping("/lab-requests/{id}/pdf")
    @PreAuthorize("hasAnyAuthority('CMP_LAB_READ', 'CMP_LAB_ENTRY', 'CMP_CONSULTATION_READ')")
    public ResponseEntity<byte[]> getLabReportPdf(@PathVariable Long id) {
        try {
            byte[] pdf = reportService.generateLabReportPdf(id);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "lab_report_" + id + ".pdf");
            return new ResponseEntity<>(pdf, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- Mapper Helper ---

    private LabRequestResponse mapToResponse(LabRequest request) {
        List<LabResultResponse> results = request.getLabResults().stream()
                .map(this::mapResultToResponse)
                .collect(Collectors.toList());

        return LabRequestResponse.builder()
                .id(request.getId())
                .encounterId(request.getEncounter().getId())
                .patientId(request.getPatient().getId())
                .patientName(request.getPatient().getFirstName() + " " + request.getPatient().getLastName())
                .labTestId(request.getLabTest() != null ? request.getLabTest().getId() : null)
                .testName(request.getTestName())
                .testCode(request.getLabTest() != null ? request.getLabTest().getCode() : null)
                .referenceRange(request.getLabTest() != null ? request.getLabTest().getReferenceRange() : null)
                .status(request.getStatus().name())
                .technicianNotes(request.getTechnicianNotes())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .results(results)
                .parameters(mapParameters(request.getLabTest()))
                .build();
    }

    private List<com.hms.HospitalManagementSystem.dto.response.LabTestParameterDto> mapParameters(
            LabTestCatalog labTest) {
        if (labTest == null)
            return null;

        if (labTest.getParameters() != null && !labTest.getParameters().isEmpty()) {
            return labTest.getParameters().stream()
                    .map(p -> com.hms.HospitalManagementSystem.dto.response.LabTestParameterDto.builder()
                            .id(p.getId())
                            .parameterName(p.getParameterName())
                            .unit(p.getUnit())
                            .referenceRange(p.getReferenceRange())
                            .build())
                    .collect(Collectors.toList());
        } else {
            // Fallback for singular tests that don't have explicit parameters
            return List.of(com.hms.HospitalManagementSystem.dto.response.LabTestParameterDto.builder()
                    .id(labTest.getId() * -1) // Use negative ID to avoid collision if necessary, but DTO doesn't
                                              // strictly care
                    .parameterName(labTest.getName())
                    .unit("")
                    .referenceRange(labTest.getReferenceRange())
                    .build());
        }
    }

    private LabResultResponse mapResultToResponse(LabResult result) {
        return LabResultResponse.builder()
                .id(result.getId())
                .parameterName(result.getParameterName())
                .resultValue(result.getResultValue())
                .unit(result.getUnit())
                .referenceRange(result.getReferenceRange())
                .isAbnormal(result.getAbnormal())
                .createdAt(result.getCreatedAt())
                .build();
    }
}
