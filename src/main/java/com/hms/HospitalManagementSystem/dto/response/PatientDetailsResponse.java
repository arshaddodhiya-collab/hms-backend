package com.hms.HospitalManagementSystem.dto.response;

import com.hms.HospitalManagementSystem.dto.response.MedicalHistoryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PatientDetailsResponse extends PatientResponse {
    private String allergies;
    private List<MedicalHistoryDto> medicalHistory;
}
