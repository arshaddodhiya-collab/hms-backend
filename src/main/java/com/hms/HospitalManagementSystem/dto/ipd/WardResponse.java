package com.hms.HospitalManagementSystem.dto.ipd;

import com.hms.HospitalManagementSystem.enums.WardType;
import lombok.Data;

@Data
public class WardResponse {
    private Long id;
    private String name;
    private WardType type;
    private int capacity;
    private boolean isActive;

    // Audit fields
    private String createdBy;
    private String lastModifiedBy;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
