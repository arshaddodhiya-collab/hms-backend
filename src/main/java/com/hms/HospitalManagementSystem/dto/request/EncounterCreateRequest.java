package com.hms.HospitalManagementSystem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EncounterCreateRequest {
    private Long appointmentId; // Optional - encounter can be created without an appointment

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;
}
