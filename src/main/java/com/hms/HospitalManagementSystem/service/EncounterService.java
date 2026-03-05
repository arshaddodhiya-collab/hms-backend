package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.ipd.RoundRequest;
import com.hms.HospitalManagementSystem.dto.response.EncounterResponse;
import com.hms.HospitalManagementSystem.dto.response.RoundResponse;

import java.util.List;

public interface EncounterService {
    EncounterResponse startEncounter(Long appointmentId, Long patientId, Long doctorId);

    EncounterResponse updateClinicalNotes(Long id, String chiefComplaint, String diagnosis, String notes,
            Long currentUserId);

    EncounterResponse completeEncounter(Long id, Long currentUserId);

    EncounterResponse getEncounterById(Long id);

    EncounterResponse getEncounterByAppointmentId(Long appointmentId);

    org.springframework.data.domain.Slice<EncounterResponse> getTriageQueue(
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Slice<EncounterResponse> getDoctorQueue(Long doctorId,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Slice<EncounterResponse> getOpdDoctorQueue(Long doctorId,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Slice<EncounterResponse> getPatientEncounters(Long patientId,
            org.springframework.data.domain.Pageable pageable);

    RoundResponse addRound(RoundRequest request, Long doctorId);

    org.springframework.data.domain.Slice<EncounterResponse> getIpdDoctorQueue(Long doctorId,
            org.springframework.data.domain.Pageable pageable);
}
