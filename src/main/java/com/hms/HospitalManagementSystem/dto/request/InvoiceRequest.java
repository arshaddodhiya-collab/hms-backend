package com.hms.HospitalManagementSystem.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    private Long admissionId; // Optional

    private List<Long> chargeIds; // IDs of unbilled charges to include

    private String status; // Optional, defaults to ISSUED if null

    @NotEmpty(message = "Items are required if creating manual invoice")
    private List<InvoiceItemRequest> items; // For manual items
}
