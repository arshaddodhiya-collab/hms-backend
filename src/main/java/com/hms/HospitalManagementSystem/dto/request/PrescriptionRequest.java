package com.hms.HospitalManagementSystem.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PrescriptionRequest {
    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;

    @NotEmpty(message = "At least one prescription item is required")
    @Valid
    private List<PrescriptionItemRequest> items;
}
