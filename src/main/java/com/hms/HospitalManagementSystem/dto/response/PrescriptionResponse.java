package com.hms.HospitalManagementSystem.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PrescriptionResponse {
    private Long id;
    private Long encounterId;
    private String note;
    private String status;
    private LocalDateTime issuedAt;
    private List<PrescriptionItemResponse> items;

    // Audit fields
    private String createdBy;
    private String lastModifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
