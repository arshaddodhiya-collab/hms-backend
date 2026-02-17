package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.entity.Appointment;
import com.hms.HospitalManagementSystem.entity.Encounter;
import com.hms.HospitalManagementSystem.entity.User;
import com.hms.HospitalManagementSystem.enums.AdmissionStatus;
import com.hms.HospitalManagementSystem.enums.AppointmentStatus;
import com.hms.HospitalManagementSystem.enums.EncounterStatus;
import com.hms.HospitalManagementSystem.enums.PrescriptionStatus;
import com.hms.HospitalManagementSystem.exception.ConflictException;
import com.hms.HospitalManagementSystem.exception.ResourceNotFoundException;
import com.hms.HospitalManagementSystem.repository.AppointmentRepository;
import com.hms.HospitalManagementSystem.repository.EncounterRepository;
import com.hms.HospitalManagementSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// import com.hms.HospitalManagementSystem.enums.PrescriptionStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EncounterService {

    private final EncounterRepository encounterRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    // private final com.hms.HospitalManagementSystem.repository.AdmissionRepository
    // admissionRepository; // Unused now
    private final com.hms.HospitalManagementSystem.repository.RoundRepository roundRepository;
    private final com.hms.HospitalManagementSystem.repository.VitalsRepository vitalsRepository;

    @Transactional
    public Encounter startEncounter(Long appointmentId, Long patientId, Long doctorId) {
        // 1. Check if encounter already exists
        return encounterRepository.findByAppointmentId(appointmentId)
                .orElseGet(() -> createEncounter(appointmentId, patientId, doctorId));
    }

    @Transactional
    public Encounter createIpdEncounter(com.hms.HospitalManagementSystem.entity.Admission admission) {
        // Create an encounter linked to the admission
        // This encounter stays active for the duration of the admission
        Encounter encounter = Encounter.builder()
                .admission(admission)
                .patient(admission.getPatient())
                .doctor(admission.getDoctor())
                .status(EncounterStatus.IN_PROGRESS) // Start as In Progress
                .startedAt(LocalDateTime.now())
                .build();

        return encounterRepository.save(encounter);
    }

    @Transactional
    public void addRound(com.hms.HospitalManagementSystem.dto.ipd.RoundRequest request) {
        // 1. Find the active IPD encounter for this admission
        // We assume there's one active encounter per admission.
        // We can find by Admission ID and Status != COMPLETED?
        // Or just find by Active Admission linked encounter.
        // For now, let's look up the encounter by Admission ID.
        // But EncounterRepository doesn't have a direct method for this yet
        // effectively.
        // We really want the encounter linked to this admission.
        // Let's add findByAdmissionId to repository or use stream if necessary (but
        // better repo method).
        // Since we don't have findByAdmissionId yet, we can try to find by patient and
        // admission active?
        // Wait, Encounter has 'admission' field.

        Encounter encounter = encounterRepository.findByAdmissionId(request.getAdmissionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active IPD Encounter not found for Admission ID: " + request.getAdmissionId()));

        // 2. Validate Doctor
        User doctor = userRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        // 3. Create Round Entity
        com.hms.HospitalManagementSystem.entity.Round round = com.hms.HospitalManagementSystem.entity.Round.builder()
                .encounter(encounter)
                .doctor(doctor)
                .notes(request.getNotes())
                .build();

        roundRepository.save(round);

        // 4. Create Vitals Entity (if vitals are present)
        // Check if any vital sign is provided
        if (request.getTemperature() != null || request.getSystolic() != null || request.getPulse() != null) {
            com.hms.HospitalManagementSystem.entity.Vitals vitals = com.hms.HospitalManagementSystem.entity.Vitals
                    .builder()
                    .encounter(encounter)
                    .recordedBy(doctor)
                    .recordedAt(LocalDateTime.now())
                    .temperature(request.getTemperature())
                    .systolic(request.getSystolic())
                    .diastolic(request.getDiastolic())
                    .pulse(request.getPulse())
                    .spo2(request.getSpo2())
                    .build();

            vitalsRepository.save(vitals);
        }
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

    public List<Encounter> getIpdDoctorQueue(Long doctorId) {
        return encounterRepository.findByDoctorIdAndAdmissionIsNotNullAndStatusIn(
                doctorId,
                Arrays.asList(EncounterStatus.TRIAGE, EncounterStatus.IN_PROGRESS));
    }

    public List<Encounter> getPatientEncounters(Long patientId) {
        return encounterRepository.findByPatientId(patientId);
    }
}
