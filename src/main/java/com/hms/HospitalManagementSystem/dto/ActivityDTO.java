package com.hms.HospitalManagementSystem.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ActivityDTO {
    private Long id;
    private String type; // e.g., "APPOINTMENT", "LAB", "ADMISSION", "INVOICE"
    private String description;
    private LocalDateTime timestamp;
    private String status;
    private String userRoleRequiringAccess; // e.g., "DOCTOR", "ADMIN", "NURSE"
}
