package com.hms.HospitalManagementSystem.dto.ipd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundRequest {
    private Long admissionId; // Identify the encounter via admission
    private String notes;

    // Vitals
    private BigDecimal temperature;
    private Integer systolic;
    private Integer diastolic;
    private Integer pulse;
    private Integer spo2;
}
