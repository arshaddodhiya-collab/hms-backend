package com.hms.HospitalManagementSystem.projection;

import com.hms.HospitalManagementSystem.entity.MedicalHistory;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;

public interface MedicalHistoryProjection {
    Long getId();

    @Value("#{target.conditionName}")
    String getCondition();

    LocalDate getDiagnosedDate();

    MedicalHistory.Status getStatus();
}
