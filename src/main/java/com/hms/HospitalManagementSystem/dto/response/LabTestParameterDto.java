package com.hms.HospitalManagementSystem.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LabTestParameterDto {
    private Long id;
    private String parameterName;
    private String unit;
    private String referenceRange;
}
