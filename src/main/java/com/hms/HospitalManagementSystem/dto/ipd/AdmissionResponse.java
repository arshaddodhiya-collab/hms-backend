package com.hms.HospitalManagementSystem.dto.ipd;

import com.hms.HospitalManagementSystem.dto.ipd.BedResponse;
import com.hms.HospitalManagementSystem.enums.AdmissionStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdmissionResponse {
    private Long id;
    private LocalDateTime admissionDate;
    private LocalDateTime dischargeDate;
    private AdmissionStatus status;
    private String diagnosis;
    private String dischargeSummary;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private BedResponse bed;
}
