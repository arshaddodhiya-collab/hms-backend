package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.request.AppointmentRequest;
import com.hms.HospitalManagementSystem.dto.response.AppointmentResponse;
import com.hms.HospitalManagementSystem.entity.Appointment;
import com.hms.HospitalManagementSystem.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping("/book")
    @PreAuthorize("hasAnyAuthority('CMP_APPOINTMENT_CREATE', 'ROLE_RECEPTION', 'ROLE_ADMIN', 'ROLE_PATIENT')")
    public ResponseEntity<AppointmentResponse> bookAppointment(@RequestBody AppointmentRequest request) {
        Appointment appointment = appointmentService.bookAppointment(request);
        return ResponseEntity.ok(mapToResponse(appointment));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CMP_APPOINTMENT_EDIT', 'ROLE_RECEPTION', 'ROLE_ADMIN')")
    public ResponseEntity<AppointmentResponse> updateAppointment(@PathVariable Long id,
            @RequestBody AppointmentRequest request) {
        Appointment appointment = appointmentService.updateAppointment(id, request);
        return ResponseEntity.ok(mapToResponse(appointment));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('CMP_APPOINTMENT_EDIT', 'ROLE_RECEPTION', 'ROLE_ADMIN', 'ROLE_PATIENT')")
    public ResponseEntity<AppointmentResponse> cancelAppointment(@PathVariable Long id, @RequestBody String reason) {
        Appointment appointment = appointmentService.cancelAppointment(id, reason);
        return ResponseEntity.ok(mapToResponse(appointment));
    }

    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAnyAuthority('CMP_APPOINTMENT_EDIT', 'ROLE_NURSE', 'ROLE_RECEPTION', 'ROLE_ADMIN')")
    public ResponseEntity<AppointmentResponse> checkInAppointment(@PathVariable Long id) {
        Appointment appointment = appointmentService.checkIn(id);
        return ResponseEntity.ok(mapToResponse(appointment));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAnyAuthority('CMP_CONSULTATION_WRITE', 'ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<AppointmentResponse> startConsultation(@PathVariable Long id) {
        Appointment appointment = appointmentService.startConsultation(id);
        return ResponseEntity.ok(mapToResponse(appointment));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyAuthority('CMP_CONSULTATION_WRITE', 'ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<AppointmentResponse> completeAppointment(@PathVariable Long id) {
        Appointment appointment = appointmentService.completeAppointment(id);
        return ResponseEntity.ok(mapToResponse(appointment));
    }

    @PutMapping("/{id}/no-show")
    @PreAuthorize("hasAnyAuthority('CMP_APPOINTMENT_EDIT', 'ROLE_RECEPTION', 'ROLE_ADMIN')")
    public ResponseEntity<AppointmentResponse> markNoShow(@PathVariable Long id) {
        Appointment appointment = appointmentService.markNoShow(id);
        return ResponseEntity.ok(mapToResponse(appointment));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ACT_DELETE', 'ROLE_ADMIN')")
    public ResponseEntity<Void> deleteAppointment(@PathVariable Long id) {
        appointmentService.softDeleteAppointment(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasAnyAuthority('ACT_DELETE', 'ROLE_ADMIN')")
    public ResponseEntity<AppointmentResponse> restoreAppointment(@PathVariable Long id) {
        Appointment appointment = appointmentService.restoreAppointment(id);
        return ResponseEntity.ok(mapToResponse(appointment));
    }

    @GetMapping("/doctor/{doctorId}/upcoming")
    @PreAuthorize("hasAnyAuthority('CMP_APPOINTMENT_LIST', 'ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<List<AppointmentResponse>> getUpcomingAppointmentsForDoctor(@PathVariable Long doctorId) {
        List<Appointment> appointments = appointmentService.getUpcomingAppointmentsForDoctor(doctorId);
        return ResponseEntity.ok(appointments.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyAuthority('CMP_APPOINTMENT_LIST', 'ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<List<AppointmentResponse>> getDoctorAppointments(
            @PathVariable Long doctorId,
            @RequestParam String date) {

        LocalDate localDate = LocalDate.parse(date);
        LocalDateTime start = localDate.atStartOfDay();
        LocalDateTime end = localDate.atTime(23, 59, 59);

        List<Appointment> appointments = appointmentService.getDoctorAppointments(doctorId, start, end);
        return ResponseEntity.ok(appointments.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyAuthority('CMP_APPOINTMENT_LIST', 'ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_ADMIN', 'ROLE_PATIENT')")
    public ResponseEntity<List<AppointmentResponse>> getPatientAppointments(
            @PathVariable Long patientId,
            @RequestParam(required = false) String status) {
        List<Appointment> appointments;
        if (status != null) {
            // Assuming AppointmentStatus is imported
            // TODO: Handle invalid status string gracefully
            appointments = appointmentService.getPatientAppointmentsByStatus(
                    patientId,
                    com.hms.HospitalManagementSystem.enums.AppointmentStatus.valueOf(status));
        } else {
            appointments = appointmentService.getPatientAppointments(patientId);
        }
        return ResponseEntity.ok(appointments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyAuthority('CMP_APPOINTMENT_LIST', 'ROLE_RECEPTION', 'ROLE_NURSE', 'ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<List<AppointmentResponse>> getTodayAppointments() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);

        List<Appointment> appointments = appointmentService.getAppointmentsByDate(start, end);
        return ResponseEntity.ok(appointments.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('CMP_APPOINTMENT_LIST', 'ROLE_ADMIN')")
    public ResponseEntity<List<AppointmentResponse>> getAllAppointments() {
        // Find all appointments
        List<Appointment> appointments = appointmentService.getAllAppointments();
        return ResponseEntity.ok(appointments.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CMP_APPOINTMENT_VIEW', 'ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_RECEPTION', 'ROLE_ADMIN', 'ROLE_PATIENT')")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable Long id) {
        Appointment appointment = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(mapToResponse(appointment));
    }

    private AppointmentResponse mapToResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .patientId(appointment.getPatient().getId())
                .patientName(appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName())
                .doctorId(appointment.getDoctor().getId())
                .doctorName(appointment.getDoctor().getFullName())
                .startDateTime(appointment.getStartDateTime())
                .endDateTime(appointment.getEndDateTime())
                .status(appointment.getStatus().name())
                .type(appointment.getType().name())
                .reason(appointment.getReason())
                .notes(appointment.getNotes())
                .cancelReason(appointment.getCancelReason())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .hasEncounter(appointment.hasEncounter())
                // .encounterStatus(appointment.hasEncounter() ?
                // appointment.getEncounter().getStatus().name() : null) // Stubbed
                .build();
    }
}
