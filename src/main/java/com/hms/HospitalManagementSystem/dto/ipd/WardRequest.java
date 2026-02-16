package com.hms.HospitalManagementSystem.dto.ipd;

import com.hms.HospitalManagementSystem.enums.WardType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WardRequest {
    @NotBlank(message = "Ward name is required")
    private String name;

    @NotNull(message = "Ward type is required")
    private WardType type;

    @Min(value = 1, message = "Capacity must be at least 1")
    private int capacity;

    private boolean isActive = true;
}
