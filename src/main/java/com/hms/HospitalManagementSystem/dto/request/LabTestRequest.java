package com.hms.HospitalManagementSystem.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LabTestRequest {
    @NotBlank(message = "Test name is required")
    private String name;

    @NotBlank(message = "Test code is required")
    private String code;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    private BigDecimal price;

    private String referenceRange;
    private boolean active = true;
}
