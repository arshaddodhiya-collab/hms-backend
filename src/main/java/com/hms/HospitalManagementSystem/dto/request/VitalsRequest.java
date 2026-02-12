package com.hms.HospitalManagementSystem.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VitalsRequest {
    private BigDecimal temperature;
    private Integer systolic;
    private Integer diastolic;
    private Integer pulse;
    private Integer spo2;
    private BigDecimal weight;
    private BigDecimal height;
}
