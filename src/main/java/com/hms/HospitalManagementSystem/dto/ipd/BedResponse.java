package com.hms.HospitalManagementSystem.dto.ipd;

import com.hms.HospitalManagementSystem.enums.BedType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BedResponse {
    private Long id;
    private String number;
    private BedType type;
    @JsonProperty("isOccupied")
    private boolean isOccupied;
    private boolean isActive;
    private WardResponse ward;

    // Audit fields
    private String createdBy;
    private String lastModifiedBy;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
