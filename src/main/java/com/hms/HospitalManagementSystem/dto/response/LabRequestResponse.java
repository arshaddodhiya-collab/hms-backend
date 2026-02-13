package com.hms.HospitalManagementSystem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class LabRequestResponse {
    private Long id;
    private Long encounterId;
    private Long patientId;
    private String patientName;
    private Long labTestId;
    private String testName;
    private String testCode;
    private String status;
    private String technicianNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<LabResultResponse> results;
    private List<LabTestParameterDto> parameters;
    private String referenceRange;
}
