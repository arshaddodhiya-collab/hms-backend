package com.hms.HospitalManagementSystem.dto;

import com.hms.HospitalManagementSystem.entity.MedicalHistory;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistoryDto {
    private Long id;
    private String conditionName;
    private LocalDate diagnosedDate;
    private MedicalHistory.Status status;
}
