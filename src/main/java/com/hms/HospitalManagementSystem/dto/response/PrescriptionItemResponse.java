package com.hms.HospitalManagementSystem.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PrescriptionItemResponse {
    private Long id;
    private String medicineName;
    private String dosage;
    private String frequency;
    private String duration;
}
