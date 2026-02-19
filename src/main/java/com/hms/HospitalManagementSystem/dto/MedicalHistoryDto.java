package com.hms.HospitalManagementSystem.dto;

import com.hms.HospitalManagementSystem.entity.MedicalHistory;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistoryDto {
    private Long id;
    @NotBlank(message = "Condition is required")
    private String condition;

    @NotNull(message = "Diagnosed date is required")
    @PastOrPresent(message = "Diagnosed date cannot be in the future")
    private LocalDate diagnosedDate;

    @NotNull(message = "Status is required")
    private MedicalHistory.Status status;
}
