package com.hms.HospitalManagementSystem.dto.request;

import lombok.Data;

@Data
public class EncounterCreateRequest {
    private Long appointmentId;
    private Long patientId;
    private Long doctorId;
}
