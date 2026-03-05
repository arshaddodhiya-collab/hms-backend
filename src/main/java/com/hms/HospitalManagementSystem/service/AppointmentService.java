package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.request.AppointmentRequest;
import com.hms.HospitalManagementSystem.entity.Appointment;
import com.hms.HospitalManagementSystem.enums.AppointmentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;

public interface AppointmentService {
    Appointment bookAppointment(AppointmentRequest request);

    Appointment updateAppointment(Long id, AppointmentRequest request);

    Appointment cancelAppointment(Long id, String reason);

    Appointment checkIn(Long id);

    Appointment startConsultation(Long id);

    Appointment completeAppointment(Long id);

    Appointment markNoShow(Long id);

    void softDeleteAppointment(Long id);

    Appointment restoreAppointment(Long id);

    Slice<Appointment> getDoctorAppointments(Long doctorId, LocalDateTime start,
                                             LocalDateTime end, org.springframework.data.domain.Pageable pageable);

    Slice<Appointment> getAllAppointments(
            org.springframework.data.domain.Pageable pageable);

    Slice<Appointment> getAppointmentsByDate(LocalDateTime start, LocalDateTime end,
            org.springframework.data.domain.Pageable pageable);

    Appointment getAppointmentById(Long id);

    Slice<Appointment> getPatientAppointments(Long patientId,
            org.springframework.data.domain.Pageable pageable);

    Slice<Appointment> getPatientAppointmentsByStatus(Long patientId,
            AppointmentStatus status, org.springframework.data.domain.Pageable pageable);

    void completeActiveAppointmentsForPatient(Long patientId);

    Slice<Appointment> getUpcomingAppointmentsForDoctor(Long doctorId,
            org.springframework.data.domain.Pageable pageable);
}
