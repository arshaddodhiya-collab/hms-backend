package com.hms.HospitalManagementSystem.dto.ipd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdmissionRequest {
    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    @NotNull(message = "Bed ID is required")
    private Long bedId;

    @NotBlank(message = "Diagnosis is required")
    private String diagnosis;
}
