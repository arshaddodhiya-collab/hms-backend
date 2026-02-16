package com.hms.HospitalManagementSystem.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class EncounterResponse {
    private Long id;
    private Long appointmentId;
    private Long admissionId;
    private Long patientId;
    private Long doctorId;
    private String patientName;
    private String patientGender;
    private String patientDob;
    private String doctorName;
    private String status;
    private String chiefComplaint;
    private String diagnosis;
    private String notes;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private VitalsResponse vitals;
}
