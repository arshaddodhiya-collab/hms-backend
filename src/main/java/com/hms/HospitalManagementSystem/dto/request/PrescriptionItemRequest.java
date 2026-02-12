package com.hms.HospitalManagementSystem.dto.request;

import lombok.Data;

@Data
public class PrescriptionItemRequest {
    private String medicineName;
    private String dosage;
    private String frequency;
    private String duration;
}
