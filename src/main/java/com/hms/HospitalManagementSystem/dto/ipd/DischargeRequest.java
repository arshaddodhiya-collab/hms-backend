package com.hms.HospitalManagementSystem.dto.ipd;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DischargeRequest {
    @NotBlank(message = "Discharge summary is required")
    private String dischargeSummary;
}
