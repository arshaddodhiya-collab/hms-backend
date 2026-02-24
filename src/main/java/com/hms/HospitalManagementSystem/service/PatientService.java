package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.request.PatientRegisterRequest;
import com.hms.HospitalManagementSystem.dto.request.PatientUpdateRequest;
import com.hms.HospitalManagementSystem.projection.PatientDetailsProjection;
import com.hms.HospitalManagementSystem.projection.PatientProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PatientService {
    PatientProjection registerPatient(PatientRegisterRequest request);

    Page<PatientProjection> searchPatients(String query, Pageable pageable);

    PatientDetailsProjection getPatientDetails(Long id);

    PatientProjection updatePatient(Long id, PatientUpdateRequest request);

    void deletePatient(Long id);
}
