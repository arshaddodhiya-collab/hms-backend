package com.hms.HospitalManagementSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LabResultRequest {
    @NotBlank(message = "Parameter name is required")
    private String parameterName;

    @NotBlank(message = "Result value is required")
    private String resultValue;

    private String unit;
    private String referenceRange;
    private Boolean isAbnormal;
}
