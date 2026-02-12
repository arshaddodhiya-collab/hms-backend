package com.hms.HospitalManagementSystem.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class VitalsResponse {
    private Long id;
    private Long encounterId;
    private BigDecimal temperature;
    private Integer systolic;
    private Integer diastolic;
    private Integer pulse;
    private Integer spo2;
    private BigDecimal weight;
    private BigDecimal height;
    private BigDecimal bmi;
    private LocalDateTime recordedAt;
    private String recordedBy;
}
