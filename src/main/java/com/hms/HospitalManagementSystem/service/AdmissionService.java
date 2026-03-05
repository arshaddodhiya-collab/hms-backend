package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.ipd.AdmissionRequest;
import com.hms.HospitalManagementSystem.dto.ipd.AdmissionResponse;
import com.hms.HospitalManagementSystem.dto.ipd.DischargeRequest;
import com.hms.HospitalManagementSystem.dto.ipd.TransferRequest;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface AdmissionService {
    AdmissionResponse admitPatient(AdmissionRequest request);

    AdmissionResponse dischargePatient(Long admissionId, DischargeRequest request);

    AdmissionResponse transferPatient(Long admissionId, TransferRequest request);

    Slice<AdmissionResponse> getActiveAdmissions(
            org.springframework.data.domain.Pageable pageable);

    AdmissionResponse getAdmissionById(Long id);
}
