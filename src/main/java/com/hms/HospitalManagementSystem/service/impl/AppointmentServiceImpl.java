package com.hms.HospitalManagementSystem.service.impl;

import com.hms.HospitalManagementSystem.dto.request.AppointmentRequest;
import com.hms.HospitalManagementSystem.entity.Appointment;
import com.hms.HospitalManagementSystem.enums.AppointmentStatus;
import com.hms.HospitalManagementSystem.enums.AppointmentType;
import com.hms.HospitalManagementSystem.repository.AppointmentRepository;
import com.hms.HospitalManagementSystem.repository.EncounterRepository;
import com.hms.HospitalManagementSystem.repository.PatientRepository;
import com.hms.HospitalManagementSystem.repository.UserRepository;
import com.hms.HospitalManagementSystem.exception.ResourceNotFoundException;
import com.hms.HospitalManagementSystem.exception.ConflictException;
import com.hms.HospitalManagementSystem.exception.ValidationException;
import com.hms.HospitalManagementSystem.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final EncounterRepository encounterRepository;

    @Override
    @Transactional
    public Appointment bookAppointment(AppointmentRequest request) {
        // 0. Validate Input
        Instant now = Instant.now();
        if (request.getStartDateTime().isBefore(now)) {
            throw new ValidationException("Appointment time must be in the future");
        }

        if (request.getEndDateTime().isBefore(request.getStartDateTime())) {
            throw new ValidationException("End time must be after start time");
        }

        long durationMinutes = java.time.Duration.between(
                request.getStartDateTime(),
                request.getEndDateTime()).toMinutes();

        if (durationMinutes < 15 || durationMinutes > 120) {
            throw new ValidationException(
                    "Appointment duration must be between 15 and 120 minutes");
        }

        // Convert Instant → LocalDateTime using system zone for JPA
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime startLdt = LocalDateTime.ofInstant(request.getStartDateTime(), zone);
        LocalDateTime endLdt = LocalDateTime.ofInstant(request.getEndDateTime(), zone);

        // 1. Validate Doctor and Patient
        var patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        var doctor = userRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        // 2. Validate Overlaps
        boolean hasOverlap = appointmentRepository.existsOverlappingAppointment(
                request.getDoctorId(),
                startLdt,
                endLdt);

        if (hasOverlap) {
            throw new ConflictException("Doctor is not available at this time");
        }

        // 3. Create Appointment
        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .startDateTime(startLdt)
                .endDateTime(endLdt)
                .type(AppointmentType.valueOf(request.getType()))
                .status(AppointmentStatus.SCHEDULED)
                .reason(request.getReason())
                .build();

        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public Appointment updateAppointment(Long id, AppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // Validate Doctor if changed
        if (!appointment.getDoctor().getId().equals(request.getDoctorId())) {
            // Check for encounter (stubbed)
            if (appointment.hasEncounter()) {
                throw new ConflictException("Cannot change doctor after encounter has been created");
            }

            var doctor = userRepository.findById(request.getDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
            appointment.setDoctor(doctor);
        }

        // Prevent time change if encounter is active (stubbed)
        if (appointment.hasActiveEncounter()) {
            if (!appointment.getStartDateTime().equals(request.getStartDateTime()) ||
                    !appointment.getEndDateTime().equals(request.getEndDateTime())) {
                throw new ConflictException("Cannot change appointment time while encounter is active");
            }
        }

        // Convert Instant → LocalDateTime using system zone for JPA
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime startLdt = LocalDateTime.ofInstant(request.getStartDateTime(), zone);
        LocalDateTime endLdt = LocalDateTime.ofInstant(request.getEndDateTime(), zone);

        appointment.setStartDateTime(startLdt);
        appointment.setEndDateTime(endLdt);
        appointment.setType(AppointmentType.valueOf(request.getType()));
        appointment.setReason(request.getReason());

        if (request.getStatus() != null) {
            try {
                appointment.setStatus(AppointmentStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid status or throw error
            }
        }

        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public Appointment cancelAppointment(Long id, String reason) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.hasActiveEncounter()) {
            throw new ConflictException(
                    "Cannot cancel appointment with active encounter. Complete or close the encounter first.");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ConflictException("Cannot cancel a completed appointment");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(reason);
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public Appointment checkIn(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ConflictException("Only SCHEDULED appointments can be checked in");
        }

        appointment.setStatus(AppointmentStatus.CHECKED_IN);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Create Encounter for Triage
        com.hms.HospitalManagementSystem.entity.Encounter encounter = com.hms.HospitalManagementSystem.entity.Encounter
                .builder()
                .appointment(savedAppointment)
                .patient(savedAppointment.getPatient())
                .doctor(savedAppointment.getDoctor())
                .status(com.hms.HospitalManagementSystem.enums.EncounterStatus.TRIAGE)
                .startedAt(LocalDateTime.now())
                .build();

        encounterRepository.save(encounter);

        return savedAppointment;
    }

    @Override
    @Transactional
    public Appointment startConsultation(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            throw new ConflictException("Only CHECKED_IN appointments can start consultation");
        }

        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public Appointment completeAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new ConflictException("Only IN_PROGRESS appointments can be completed");
        }

        // Verify encounter is completed (stubbed)
        if (appointment.hasActiveEncounter()) { // logic slightly different in stub, basically checks if encounter is
                                                // NOT completed
            throw new ConflictException("Cannot complete appointment before encounter is completed");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public Appointment markNoShow(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ConflictException("Cannot mark completed appointment as no-show");
        }

        appointment.setStatus(AppointmentStatus.NO_SHOW);
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public void softDeleteAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (appointment.hasActiveEncounter()) {
            throw new ConflictException("Cannot delete appointment with active encounter");
        }

        appointment.setDeleted(true);
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public Appointment restoreAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        appointment.setDeleted(false);
        return appointmentRepository.save(appointment);
    }

    @Override
    public List<Appointment> getDoctorAppointments(Long doctorId, LocalDateTime start, LocalDateTime end) {
        // TODO: Validate current user is doctor or admin
        return appointmentRepository.findByDoctorIdAndStartDateTimeBetweenAndDeletedFalse(doctorId, start, end);
    }

    @Override
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    @Override
    public List<Appointment> getAppointmentsByDate(LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findByStartDateTimeBetweenAndDeletedFalse(start, end);
    }

    @Override
    public Appointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    @Override
    public List<Appointment> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    @Override
    public List<Appointment> getPatientAppointmentsByStatus(Long patientId, AppointmentStatus status) {
        return appointmentRepository.findByPatientIdAndStatus(patientId, status);
    }

    @Override
    @Transactional
    public void completeActiveAppointmentsForPatient(Long patientId) {
        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);
        LocalDateTime now = LocalDateTime.now();

        appointments.stream()
                .filter(a -> !a.isDeleted())
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED ||
                        a.getStatus() == AppointmentStatus.CHECKED_IN ||
                        a.getStatus() == AppointmentStatus.IN_PROGRESS)
                .filter(a -> a.getStartDateTime().isBefore(now)
                        || a.getStartDateTime().toLocalDate().equals(now.toLocalDate()))
                .forEach(a -> {
                    a.setStatus(AppointmentStatus.COMPLETED);
                    appointmentRepository.save(a);
                });
    }

    @Override
    public List<Appointment> getUpcomingAppointmentsForDoctor(Long doctorId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);

        // For dashboard: We want future appointments OR active appointments (even if
        // time passed but not completed)
        return appointmentRepository
                .findByDoctorIdAndStartDateTimeBetweenAndDeletedFalse(doctorId, now.toLocalDate().atStartOfDay(),
                        endOfDay)
                .stream()
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED ||
                        a.getStatus() == AppointmentStatus.CHECKED_IN ||
                        a.getStatus() == AppointmentStatus.IN_PROGRESS)
                .sorted((a1, a2) -> a1.getStartDateTime().compareTo(a2.getStartDateTime()))
                .collect(java.util.stream.Collectors.toList());
    }
}
