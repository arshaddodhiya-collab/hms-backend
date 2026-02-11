package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.AppointmentRequest;
import com.hms.HospitalManagementSystem.entity.Appointment;
import com.hms.HospitalManagementSystem.enums.AppointmentStatus;
import com.hms.HospitalManagementSystem.enums.AppointmentType;
import com.hms.HospitalManagementSystem.repository.AppointmentRepository;
import com.hms.HospitalManagementSystem.repository.PatientRepository;
import com.hms.HospitalManagementSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    @Transactional
    public Appointment bookAppointment(AppointmentRequest request) {
        // 1. Validate Doctor and Patient
        var patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        var doctor = userRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        // 2. Validate Overlaps
        boolean hasOverlap = appointmentRepository.existsOverlappingAppointment(
                request.getDoctorId(),
                request.getStartDateTime(),
                request.getEndDateTime());

        if (hasOverlap) {
            throw new RuntimeException("Doctor is not available at this time");
        }

        // 3. Create Appointment
        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .type(AppointmentType.valueOf(request.getType()))
                .status(AppointmentStatus.SCHEDULED)
                .reason(request.getReason())
                .build();

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment cancelAppointment(Long id, String reason) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel a completed appointment");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(reason);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment checkIn(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new RuntimeException("Only SCHEDULED appointments can be checked in");
        }

        appointment.setStatus(AppointmentStatus.CHECKED_IN);
        return appointmentRepository.save(appointment);
    }

    public List<Appointment> getDoctorAppointments(Long doctorId, java.time.LocalDateTime start,
            java.time.LocalDateTime end) {
        return appointmentRepository.findByDoctorIdAndStartDateTimeBetween(doctorId, start, end);
    }
}
