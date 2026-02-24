package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.request.PatientRegisterRequest;
import com.hms.HospitalManagementSystem.dto.request.PatientUpdateRequest;
import com.hms.HospitalManagementSystem.dto.response.PatientDetailsResponse;
import com.hms.HospitalManagementSystem.dto.response.PatientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PatientService {
    PatientResponse registerPatient(PatientRegisterRequest request);

    Page<PatientResponse> searchPatients(String query, Pageable pageable);

    PatientDetailsResponse getPatientDetails(Long id);

    PatientResponse updatePatient(Long id, PatientUpdateRequest request);

    void deletePatient(Long id);
}
