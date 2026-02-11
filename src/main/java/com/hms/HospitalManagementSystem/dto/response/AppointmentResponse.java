package com.hms.HospitalManagementSystem.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AppointmentResponse {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String status;
    private String type;
    private String reason;
}
