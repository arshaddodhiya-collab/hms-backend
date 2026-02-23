package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.entity.Round;
import com.hms.HospitalManagementSystem.entity.Vitals;
import com.hms.HospitalManagementSystem.dto.ipd.RoundRequest;
import com.hms.HospitalManagementSystem.entity.Appointment;
import com.hms.HospitalManagementSystem.entity.Encounter;
import com.hms.HospitalManagementSystem.entity.User;

import com.hms.HospitalManagementSystem.enums.AppointmentStatus;
import com.hms.HospitalManagementSystem.enums.EncounterStatus;
import com.hms.HospitalManagementSystem.enums.PrescriptionStatus;
import com.hms.HospitalManagementSystem.exception.ConflictException;
import com.hms.HospitalManagementSystem.exception.ResourceNotFoundException;
import com.hms.HospitalManagementSystem.repository.AppointmentRepository;
import com.hms.HospitalManagementSystem.repository.EncounterRepository;
import com.hms.HospitalManagementSystem.repository.UserRepository;
import com.hms.HospitalManagementSystem.repository.RoundRepository;
import com.hms.HospitalManagementSystem.repository.VitalsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EncounterService {

    private final EncounterRepository encounterRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final RoundRepository roundRepository;
    private final VitalsRepository vitalsRepository;

    @Transactional
    public Encounter startEncounter(Long appointmentId, Long patientId, Long doctorId) {
        // 1. Check if encounter already exists
        return encounterRepository.findByAppointmentId(appointmentId)
                .orElseGet(() -> createEncounter(appointmentId, patientId, doctorId));
    }

    private Encounter createEncounter(Long appointmentId, Long patientId, Long doctorId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new ConflictException("Cannot start encounter for cancelled or no-show appointment");
        }

        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new ConflictException("Patient ID does not match appointment");
        }

        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new ConflictException("Doctor ID does not match appointment");
        }

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        Encounter encounter = Encounter.builder()
                .appointment(appointment)
                .patient(appointment.getPatient())
                .doctor(doctor)
                .status(EncounterStatus.TRIAGE)
                .startedAt(LocalDateTime.now())
                .build();

        if (appointment.getStatus() == AppointmentStatus.SCHEDULED) {
            appointment.setStatus(AppointmentStatus.CHECKED_IN);
            appointmentRepository.save(appointment);
        }

        return encounterRepository.save(encounter);
    }

    @Transactional
    public Encounter updateClinicalNotes(Long id, String chiefComplaint, String diagnosis, String notes,
            Long currentUserId) {
        Encounter encounter = encounterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found"));

        if (encounter.getStatus() == EncounterStatus.COMPLETED) {
            throw new ConflictException("Cannot update clinical notes for completed encounter");
        }

        // Verify doctor ownership
        if (!encounter.getDoctor().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only modify your own encounters");
        }

        encounter.setChiefComplaint(chiefComplaint);
        encounter.setDiagnosis(diagnosis);
        encounter.setNotes(notes);

        if (encounter.getStatus() == EncounterStatus.TRIAGE) {
            encounter.setStatus(EncounterStatus.IN_PROGRESS);
            // Update appointment status as well if present
            if (encounter.getAppointment() != null
                    && encounter.getAppointment().getStatus() != AppointmentStatus.IN_PROGRESS) {
                encounter.getAppointment().setStatus(AppointmentStatus.IN_PROGRESS);
                appointmentRepository.save(encounter.getAppointment());
            }
        }

        return encounterRepository.save(encounter);
    }

    @Transactional
    public Encounter completeEncounter(Long id, Long currentUserId) {
        Encounter encounter = encounterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found"));

        if (encounter.getStatus() != EncounterStatus.IN_PROGRESS) {
            // Allow completion from TRIAGE if doctor skipped notes? Usually logical flow is
            // TRIAGE -> IN_PROGRESS -> COMPLETED.
            // But let's stick to strict flow or allow if diagnosis is present. Only check
            // existing status.
            if (encounter.getStatus() == EncounterStatus.COMPLETED) {
                throw new ConflictException("Encounter is already completed");
            }
        }

        if (!encounter.getDoctor().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only complete your own encounters");
        }

        if (encounter.getDiagnosis() == null || encounter.getDiagnosis().trim().isEmpty()) {
            throw new ConflictException("Diagnosis is required to complete encounter");
        }

        encounter.setStatus(EncounterStatus.COMPLETED);
        encounter.setCompletedAt(LocalDateTime.now());

        // Update appointment if present
        if (encounter.getAppointment() != null) {
            encounter.getAppointment().setStatus(AppointmentStatus.COMPLETED);
            appointmentRepository.save(encounter.getAppointment());
        }

        // Issue prescriptions
        encounter.getPrescriptions().forEach(p -> {
            p.setStatus(PrescriptionStatus.ISSUED);
            p.setIssuedAt(LocalDateTime.now());
        });

        return encounterRepository.save(encounter);
    }

    public Encounter getEncounterById(Long id) {
        return encounterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found"));
    }

    public Encounter getEncounterByAppointmentId(Long appointmentId) {
        return encounterRepository.findByAppointmentId(appointmentId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Encounter not found for appointment " + appointmentId));
    }

    public List<Encounter> getTriageQueue() {
        return encounterRepository.findByStatus(EncounterStatus.TRIAGE);
    }

    public List<Encounter> getDoctorQueue(Long doctorId) {
        // Return encounters in both TRIAGE and IN_PROGRESS status
        // This follows HMIS standards where encounters appear in consultation queue
        // immediately after triage is completed (vitals recorded)
        return encounterRepository.findByDoctorIdAndStatusIn(
                doctorId,
                Arrays.asList(EncounterStatus.TRIAGE, EncounterStatus.IN_PROGRESS));
    }

    public List<Encounter> getOpdDoctorQueue(Long doctorId) {
        return encounterRepository.findByDoctorIdAndAppointmentIsNotNullAndStatusIn(
                doctorId,
                Arrays.asList(EncounterStatus.TRIAGE, EncounterStatus.IN_PROGRESS));
    }

    public List<Encounter> getPatientEncounters(Long patientId) {
        return encounterRepository.findByPatientId(patientId);
    }

    // IPD Rounds
    @Transactional
    public Round addRound(RoundRequest request, Long doctorId) {
        // Find Admission first to get Encounter
        // Or Request should have encounterId?
        // Plan says RoundRequest has admissionId.
        // We need to find the active encounter for this admission.
        // Assuming 1 active IPD encounter per admission.
        // But wait, Encounter table has admission_id.

        Encounter encounter = encounterRepository.findByAdmissionId(request.getAdmissionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active encounter not found for admission " + request.getAdmissionId()));

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        Round round = new Round();
        round.setEncounter(encounter);
        round.setDoctor(doctor);
        round.setNotes(request.getNotes());

        // Save Round
        round = roundRepository.save(round);

        // Helper to save Vitals if present
        if (request.getSystolic() != null || request.getTemperature() != null) {
            Vitals vitals = Vitals.builder()
                    .encounter(encounter)
                    .temperature(request.getTemperature())
                    .systolic(request.getSystolic())
                    .diastolic(request.getDiastolic())
                    .pulse(request.getPulse())
                    .spo2(request.getSpo2())
                    .recordedBy(doctor)
                    .recordedAt(LocalDateTime.now())
                    .build();
            vitalsRepository.save(vitals);
        }

        return round;
    }

    public List<Encounter> getIpdDoctorQueue(Long doctorId) {
        // IPD Queue: Active Encounters (Admitted) assigned to doctor or all?
        // Typically IPD patients are assigned to a doctor.
        // Encounter Status: IN_PROGRESS (since they are admitted)
        // And Admission is not null
        return encounterRepository.findByDoctorIdAndAdmissionIsNotNullAndStatusIn(
                doctorId,
                Arrays.asList(EncounterStatus.IN_PROGRESS));
    }
}
