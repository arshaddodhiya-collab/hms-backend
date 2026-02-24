package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.ipd.RoundRequest;
import com.hms.HospitalManagementSystem.entity.Encounter;
import com.hms.HospitalManagementSystem.entity.Round;

import java.util.List;

public interface EncounterService {
    Encounter startEncounter(Long appointmentId, Long patientId, Long doctorId);

    Encounter updateClinicalNotes(Long id, String chiefComplaint, String diagnosis, String notes, Long currentUserId);

    Encounter completeEncounter(Long id, Long currentUserId);

    Encounter getEncounterById(Long id);

    Encounter getEncounterByAppointmentId(Long appointmentId);

    List<Encounter> getTriageQueue();

    List<Encounter> getDoctorQueue(Long doctorId);

    List<Encounter> getOpdDoctorQueue(Long doctorId);

    List<Encounter> getPatientEncounters(Long patientId);

    Round addRound(RoundRequest request, Long doctorId);

    List<Encounter> getIpdDoctorQueue(Long doctorId);
}
