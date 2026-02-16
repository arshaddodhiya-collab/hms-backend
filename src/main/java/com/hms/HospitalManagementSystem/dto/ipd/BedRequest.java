package com.hms.HospitalManagementSystem.dto.ipd;

import com.hms.HospitalManagementSystem.enums.BedType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BedRequest {
    @NotBlank(message = "Bed number is required")
    private String number;

    @NotNull(message = "Bed type is required")
    private BedType type;

    @NotNull(message = "Ward ID is required")
    private Long wardId;

    private boolean isActive = true;
}
