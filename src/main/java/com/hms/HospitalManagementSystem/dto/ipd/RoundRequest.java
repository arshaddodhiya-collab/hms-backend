package com.hms.HospitalManagementSystem.dto.ipd;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class RoundRequest {
    private Long admissionId;
    private Long doctorId;
    private String notes; // Optional: initial notes

    // Vitals
    private BigDecimal temperature;
    private Integer systolic;
    private Integer diastolic;
    private Integer pulse;
    private Integer spo2;
}
