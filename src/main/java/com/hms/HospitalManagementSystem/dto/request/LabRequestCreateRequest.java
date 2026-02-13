package com.hms.HospitalManagementSystem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LabRequestCreateRequest {
    @NotNull(message = "Encounter ID is required")
    private Long encounterId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Lab Test ID is required")
    private Long labTestId;

    private String notes;
}
