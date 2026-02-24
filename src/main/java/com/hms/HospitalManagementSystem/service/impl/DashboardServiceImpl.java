package com.hms.HospitalManagementSystem.service.impl;

import com.hms.HospitalManagementSystem.dto.response.ActivityDTO;
import com.hms.HospitalManagementSystem.dto.response.DashboardDTO;
import com.hms.HospitalManagementSystem.entity.*;
import com.hms.HospitalManagementSystem.enums.AppointmentStatus;
import com.hms.HospitalManagementSystem.enums.LabRequestStatus;
import com.hms.HospitalManagementSystem.repository.*;
import com.hms.HospitalManagementSystem.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final LabRequestRepository labRequestRepository;
    private final InvoiceRepository invoiceRepository;
    private final AdmissionRepository admissionRepository;

    @Override
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "dashboardStats")
    public DashboardDTO getStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        long totalPatients = patientRepository.count();
        long expectedAppts = appointmentRepository.countByStartDateTimeBetweenAndDeletedFalse(startOfDay, endOfDay);
        long pendingLabs = labRequestRepository.countByStatus(LabRequestStatus.ORDERED);

        BigDecimal todaysRevenue = invoiceRepository.sumNetAmountByIssueDateBetween(startOfDay, endOfDay);
        if (todaysRevenue == null) {
            todaysRevenue = BigDecimal.ZERO;
        }

        // Critical patients: For now, maybe just count admitted patients?
        // Or patients with recently abnormal vitals (if we had that logic ready).
        // Let's count active admissions as a proxy for "in-care" or critical logic for
        // now.
        // Or if we specifically want "Critical Care" stats as per UI, we might need a
        // status on Admission/Patient.
        // For this iteration, let's just count total Active Admissions.
        long criticalPatients = admissionRepository.count(); // TODO: filter by status ADMITTED if available, but
                                                             // repository might not have it exposed yet.

        return DashboardDTO.builder()
                .totalPatients(totalPatients)
                .expectedAppointmentsToday(expectedAppts)
                .pendingLabRequests(pendingLabs)
                .todaysRevenue(todaysRevenue)
                .criticalPatientsCount(criticalPatients)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "dashboardActivity")
    public List<ActivityDTO> getRecentActivity() {
        List<ActivityDTO> activities = new ArrayList<>();

        // 1. Recent Appointments
        List<Appointment> recentAppts = appointmentRepository.findTop5ByDeletedFalseOrderByCreatedAtDesc();
        activities.addAll(recentAppts.stream().map(this::mapToActivity).toList());

        // 2. Recent Labs
        List<LabRequest> recentLabs = labRequestRepository.findTop5ByOrderByCreatedAtDesc();
        activities.addAll(recentLabs.stream().map(this::mapToActivity).toList());

        // 3. New Patients
        List<Patient> recentPatients = patientRepository.findTop5ByDeletedFalseOrderByCreatedAtDesc();
        activities.addAll(recentPatients.stream().map(this::mapToActivity).toList());

        // Sort by timestamp desc and take top 10
        return activities.stream()
                .sorted(Comparator.comparing(ActivityDTO::getTimestamp).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    private ActivityDTO mapToActivity(Appointment appt) {
        return ActivityDTO.builder()
                .id(appt.getId())
                .type("APPOINTMENT")
                .description("Appointment for " + appt.getPatient().getFirstName() + " with Dr. "
                        + appt.getDoctor().getFullName())
                .timestamp(appt.getCreatedAt()) // Or createdAt if startDateTime is future
                .status(appt.getStatus().name())
                .userRoleRequiringAccess("DOCTOR")
                .build();
    }

    private ActivityDTO mapToActivity(LabRequest lab) {
        return ActivityDTO.builder()
                .id(lab.getId())
                .type("LAB")
                .description("Lab Request: "
                        + (lab.getLabTest() != null ? lab.getLabTest().getName() : lab.getTestName()))
                .timestamp(lab.getCreatedAt())
                .status(lab.getStatus().name())
                .userRoleRequiringAccess("NURSE")
                .build();
    }

    private ActivityDTO mapToActivity(Patient patient) {
        return ActivityDTO.builder()
                .id(patient.getId())
                .type("PATIENT")
                .description("New Patient Registered: " + patient.getFirstName() + " " + patient.getLastName())
                .timestamp(patient.getCreatedAt())
                .status("ACTIVE")
                .userRoleRequiringAccess("RECEPTIONIST")
                .build();
    }
}
