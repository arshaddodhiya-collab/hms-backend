package com.hms.HospitalManagementSystem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LabResultResponse {
    private Long id;
    private String parameterName;
    private String resultValue;
    private String unit;
    private String referenceRange;
    private Boolean isAbnormal;
    private LocalDateTime createdAt;
}
