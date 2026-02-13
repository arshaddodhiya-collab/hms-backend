package com.hms.HospitalManagementSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LabResultRequest {
    @NotBlank(message = "Parameter name is required")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\(\\)\\-\\.]+$", message = "Parameter must only contain letters, numbers, spaces, and brackets.")
    private String parameterName;

    @NotBlank(message = "Result value is required")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\.\\-\\+\\<\\>\\/\\%]+$", message = "Value contains invalid characters.")
    private String resultValue;

    @Pattern(regexp = "^[a-zA-Z0-9\\s\\/\\%\\^\\(\\)]*$", message = "Unit contains invalid characters.")
    private String unit;

    @Pattern(regexp = "^[a-zA-Z0-9\\s\\.\\-\\+\\<\\>]*$", message = "Reference range format is invalid.")
    private String referenceRange;

    private Boolean isAbnormal;
}
