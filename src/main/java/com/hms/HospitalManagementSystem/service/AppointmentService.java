package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.request.AppointmentRequest;
import com.hms.HospitalManagementSystem.entity.Appointment;
import com.hms.HospitalManagementSystem.enums.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.List;

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

    List<Appointment> getDoctorAppointments(Long doctorId, LocalDateTime start, LocalDateTime end);

    List<Appointment> getAllAppointments();

    List<Appointment> getAppointmentsByDate(LocalDateTime start, LocalDateTime end);

    Appointment getAppointmentById(Long id);

    List<Appointment> getPatientAppointments(Long patientId);

    List<Appointment> getPatientAppointmentsByStatus(Long patientId, AppointmentStatus status);

    void completeActiveAppointmentsForPatient(Long patientId);

    List<Appointment> getUpcomingAppointmentsForDoctor(Long doctorId);
}
