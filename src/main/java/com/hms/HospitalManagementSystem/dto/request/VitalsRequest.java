package com.hms.HospitalManagementSystem.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VitalsRequest {
    @DecimalMin(value = "30.0", message = "Temperature must be at least 30°C")
    @DecimalMax(value = "45.0", message = "Temperature must not exceed 45°C")
    private BigDecimal temperature;

    @Min(value = 50, message = "Systolic pressure must be at least 50 mmHg")
    @Max(value = 300, message = "Systolic pressure must not exceed 300 mmHg")
    private Integer systolic;

    @Min(value = 20, message = "Diastolic pressure must be at least 20 mmHg")
    @Max(value = 200, message = "Diastolic pressure must not exceed 200 mmHg")
    private Integer diastolic;

    @Min(value = 20, message = "Pulse must be at least 20 bpm")
    @Max(value = 300, message = "Pulse must not exceed 300 bpm")
    private Integer pulse;

    @Min(value = 0, message = "SpO2 must be at least 0%")
    @Max(value = 100, message = "SpO2 must not exceed 100%")
    private Integer spo2;

    @DecimalMin(value = "0.5", message = "Weight must be at least 0.5 kg")
    @DecimalMax(value = "500.0", message = "Weight must not exceed 500 kg")
    private BigDecimal weight;

    @DecimalMin(value = "30.0", message = "Height must be at least 30 cm")
    @DecimalMax(value = "300.0", message = "Height must not exceed 300 cm")
    private BigDecimal height;
}
