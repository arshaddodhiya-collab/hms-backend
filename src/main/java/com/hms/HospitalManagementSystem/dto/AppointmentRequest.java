package com.hms.HospitalManagementSystem.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AppointmentRequest {
    private Long patientId;
    private Long doctorId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String type; // CONSULTATION, FOLLOW_UP, etc.
    private String reason;
}
