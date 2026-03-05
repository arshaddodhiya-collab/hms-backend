package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.ipd.RoundRequest;
import com.hms.HospitalManagementSystem.dto.response.EncounterResponse;
import com.hms.HospitalManagementSystem.dto.response.RoundResponse;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface EncounterService {
    EncounterResponse startEncounter(Long appointmentId, Long patientId, Long doctorId);

    EncounterResponse updateClinicalNotes(Long id, String chiefComplaint, String diagnosis, String notes,
            Long currentUserId);

    EncounterResponse completeEncounter(Long id, Long currentUserId);

    EncounterResponse getEncounterById(Long id);

    EncounterResponse getEncounterByAppointmentId(Long appointmentId);

    Slice<EncounterResponse> getTriageQueue(
            org.springframework.data.domain.Pageable pageable);

    Slice<EncounterResponse> getDoctorQueue(Long doctorId,
            org.springframework.data.domain.Pageable pageable);

    Slice<EncounterResponse> getOpdDoctorQueue(Long doctorId,
            org.springframework.data.domain.Pageable pageable);

    Slice<EncounterResponse> getPatientEncounters(Long patientId,
            org.springframework.data.domain.Pageable pageable);

    RoundResponse addRound(RoundRequest request, Long doctorId);

    Slice<EncounterResponse> getIpdDoctorQueue(Long doctorId,
            org.springframework.data.domain.Pageable pageable);
}
