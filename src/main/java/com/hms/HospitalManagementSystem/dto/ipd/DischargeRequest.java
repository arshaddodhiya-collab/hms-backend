package com.hms.HospitalManagementSystem.dto.ipd;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DischargeRequest {
    @NotBlank(message = "Discharge summary is required")
    private String dischargeSummary;

    private String diagnosis;

    private String advice;

    private String dischargeDate; // Optional: ISO 8601 string or leave as String to parse in service
}
