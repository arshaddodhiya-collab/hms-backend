package com.hms.HospitalManagementSystem.projection;

import java.util.List;

public interface PatientDetailsProjection extends PatientProjection {
    List<MedicalHistoryProjection> getMedicalHistory();
}
