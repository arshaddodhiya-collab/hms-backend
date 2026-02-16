package com.hms.HospitalManagementSystem.dto.ipd;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferRequest {
    @NotNull(message = "New Bed ID is required")
    private Long newBedId;
}
