package com.hms.HospitalManagementSystem.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

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
    private List<RoundResponse> rounds;
    private List<VitalsResponse> vitalsHistory;

    // Audit fields
    private String createdBy;
    private String lastModifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
