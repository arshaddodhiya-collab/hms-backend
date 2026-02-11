package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.AppointmentRequest;
import com.hms.HospitalManagementSystem.entity.Appointment;
import com.hms.HospitalManagementSystem.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping("/book")
    public ResponseEntity<Appointment> bookAppointment(@RequestBody AppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.bookAppointment(request));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Appointment> cancelAppointment(@PathVariable Long id, @RequestBody String reason) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, reason));
    }

    @PutMapping("/{id}/check-in")
    public ResponseEntity<Appointment> checkInAppointment(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.checkIn(id));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<Appointment>> getDoctorAppointments(
            @PathVariable Long doctorId,
            @RequestParam String date) { // Expecting ISO date string yyyy-MM-dd

        // Parse date to start of day and end of day
        // Ideally frontend sends range, but if sending single date:
        java.time.LocalDate localDate = java.time.LocalDate.parse(date);
        LocalDateTime start = localDate.atStartOfDay();
        LocalDateTime end = localDate.atTime(23, 59, 59);

        return ResponseEntity.ok(appointmentService.getDoctorAppointments(doctorId, start, end));
    }
}
