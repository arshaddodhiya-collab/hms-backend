package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.AppointmentRequest;
import com.hms.HospitalManagementSystem.entity.Appointment;
import com.hms.HospitalManagementSystem.entity.Patient;
import com.hms.HospitalManagementSystem.entity.User;
import com.hms.HospitalManagementSystem.enums.AppointmentStatus;
import com.hms.HospitalManagementSystem.enums.AppointmentType;
import com.hms.HospitalManagementSystem.repository.AppointmentRepository;
import com.hms.HospitalManagementSystem.repository.PatientRepository;
import com.hms.HospitalManagementSystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private User doctor;
    private Patient patient;
    private AppointmentRequest request;

    @BeforeEach
    void setUp() {
        doctor = new User();
        doctor.setId(1L);
        doctor.setFullName("Dr. Smith");

        patient = new Patient();
        patient.setId(100L);
        patient.setFirstName("John");
        patient.setLastName("Doe");

        request = new AppointmentRequest();
        request.setDoctorId(1L);
        request.setPatientId(100L);
        request.setStartDateTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        request.setEndDateTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(30));
        request.setType("CONSULTATION");
        request.setReason("Checkup");
    }

    @Test
    void bookAppointment_Success() {
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(userRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsOverlappingAppointment(any(), any(), any())).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = appointmentService.bookAppointment(request);

        assertNotNull(result);
        assertEquals(AppointmentStatus.SCHEDULED, result.getStatus());
        assertEquals(AppointmentType.CONSULTATION, result.getType());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void bookAppointment_Overlap() {
        when(patientRepository.findById(100L)).thenReturn(Optional.of(patient));
        when(userRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsOverlappingAppointment(any(), any(), any())).thenReturn(true);

        assertThrows(com.hms.HospitalManagementSystem.exception.ConflictException.class,
                () -> appointmentService.bookAppointment(request));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }
}
