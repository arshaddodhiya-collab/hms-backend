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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

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
    public ResponseEntity<Slice<AppointmentResponse>> getUpcomingAppointmentsForDoctor(
            @PathVariable Long doctorId,
            @PageableDefault(sort = "startDateTime", direction = Sort.Direction.ASC) Pageable pageable) {
        Slice<Appointment> appointments = appointmentService.getUpcomingAppointmentsForDoctor(doctorId, pageable);
        return ResponseEntity.ok(appointments.map(this::mapToResponse));
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyAuthority('CMP_APPOINTMENT_LIST', 'ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<Slice<AppointmentResponse>> getDoctorAppointments(
            @PathVariable Long doctorId,
            @RequestParam String date,
            @PageableDefault(sort = "startDateTime", direction = Sort.Direction.ASC) Pageable pageable) {

        LocalDate localDate = LocalDate.parse(date);
        LocalDateTime start = localDate.atStartOfDay();
        LocalDateTime end = localDate.atTime(23, 59, 59);

        Slice<Appointment> appointments = appointmentService.getDoctorAppointments(doctorId, start, end, pageable);
        return ResponseEntity.ok(appointments.map(this::mapToResponse));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyAuthority('CMP_APPOINTMENT_LIST', 'ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_ADMIN', 'ROLE_PATIENT')")
    public ResponseEntity<Slice<AppointmentResponse>> getPatientAppointments(
            @PathVariable Long patientId,
            @RequestParam(required = false) String status,
            @PageableDefault(sort = "startDateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        Slice<Appointment> appointments;
        if (status != null) {
            appointments = appointmentService.getPatientAppointmentsByStatus(
                    patientId,
                    com.hms.HospitalManagementSystem.enums.AppointmentStatus.valueOf(status),
                    pageable);
        } else {
            appointments = appointmentService.getPatientAppointments(patientId, pageable);
        }
        return ResponseEntity.ok(appointments.map(this::mapToResponse));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyAuthority('CMP_APPOINTMENT_LIST', 'ROLE_RECEPTION', 'ROLE_NURSE', 'ROLE_DOCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<Slice<AppointmentResponse>> getTodayAppointments(
            @PageableDefault(sort = "startDateTime", direction = Sort.Direction.ASC) Pageable pageable) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);

        Slice<Appointment> appointments = appointmentService.getAppointmentsByDate(start, end, pageable);
        return ResponseEntity.ok(appointments.map(this::mapToResponse));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('CMP_APPOINTMENT_LIST', 'ROLE_ADMIN')")
    public ResponseEntity<Slice<AppointmentResponse>> getAllAppointments(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        // Find all appointments
        Slice<Appointment> appointments = appointmentService.getAllAppointments(pageable);
        return ResponseEntity.ok(appointments.map(this::mapToResponse));
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
