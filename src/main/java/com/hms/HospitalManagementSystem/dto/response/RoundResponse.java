package com.hms.HospitalManagementSystem.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RoundResponse {
    private Long id;
    private Long encounterId;
    private Long doctorId;
    private String doctorName;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Audit fields
    private String createdBy;
    private String lastModifiedBy;
}
