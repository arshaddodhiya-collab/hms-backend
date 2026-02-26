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

    List<EncounterResponse> getTriageQueue();

    List<EncounterResponse> getDoctorQueue(Long doctorId);

    List<EncounterResponse> getOpdDoctorQueue(Long doctorId);

    List<EncounterResponse> getPatientEncounters(Long patientId);

    RoundResponse addRound(RoundRequest request, Long doctorId);

    List<EncounterResponse> getIpdDoctorQueue(Long doctorId);
}
