package com.hms.HospitalManagementSystem.dto.request;

import lombok.Data;
import java.time.Instant;

@Data
public class AppointmentRequest {
    private Long patientId;
    private Long doctorId;
    private Instant startDateTime;
    private Instant endDateTime;
    private String type; // CONSULTATION, FOLLOW_UP, etc.
    private String reason;
    private String status;
}
