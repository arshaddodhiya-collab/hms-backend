package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.entity.Appointment;
import com.hms.HospitalManagementSystem.entity.Encounter;
import com.hms.HospitalManagementSystem.entity.Patient;
import com.hms.HospitalManagementSystem.entity.Prescription;
import com.hms.HospitalManagementSystem.entity.User;
import com.hms.HospitalManagementSystem.enums.AppointmentStatus;
import com.hms.HospitalManagementSystem.enums.EncounterStatus;
import com.hms.HospitalManagementSystem.enums.PrescriptionStatus;
import com.hms.HospitalManagementSystem.repository.AppointmentRepository;
import com.hms.HospitalManagementSystem.repository.EncounterRepository;
import com.hms.HospitalManagementSystem.repository.UserRepository; // Not used directly in startEncounter but needed for mocks if service uses it
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncounterServiceTest {

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EncounterService encounterService;

    private User doctor;
    private Patient patient;
    private Appointment appointment;
    private Encounter encounter;

    @BeforeEach
    void setUp() {
        doctor = new User();
        doctor.setId(1L);
        doctor.setFullName("Dr. Smith");

        patient = new Patient();
        patient.setId(100L);
        patient.setFirstName("John Doe");

        appointment = new Appointment();
        appointment.setId(10L);
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        encounter = new Encounter();
        encounter.setId(50L);
        encounter.setAppointment(appointment);
        encounter.setPatient(patient);
        encounter.setDoctor(doctor);
        encounter.setStatus(EncounterStatus.IN_PROGRESS);
        encounter.setDiagnosis("Flu");

        // Add a prescription
        Prescription prescription = new Prescription();
        prescription.setId(1L);
        prescription.setStatus(PrescriptionStatus.DRAFT);
        prescription.setEncounter(encounter);

        List<Prescription> prescriptions = new ArrayList<>();
        prescriptions.add(prescription);
        encounter.setPrescriptions(prescriptions);
    }

    @Test
    void startEncounter_CreatesNew_WhenNotExists() {
        when(encounterRepository.findByAppointmentId(10L)).thenReturn(Optional.empty());
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(invocation -> {
            Encounter saved = invocation.getArgument(0);
            saved.setId(50L);
            saved.setStatus(EncounterStatus.IN_PROGRESS); // Service logic might set to TRIAGE initially
            return saved;
        });

        Encounter result = encounterService.startEncounter(10L, 100L, 1L);

        assertNotNull(result);
        assertEquals(50L, result.getId());
        verify(encounterRepository).save(any(Encounter.class));
    }

    @Test
    void completeEncounter_Success() {
        when(encounterRepository.findById(50L)).thenReturn(Optional.of(encounter));
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock appointment save
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

        Encounter result = encounterService.completeEncounter(50L, 1L); // 1L is doctor ID

        assertNotNull(result);
        assertEquals(EncounterStatus.COMPLETED, result.getStatus());
        assertEquals(AppointmentStatus.COMPLETED, result.getAppointment().getStatus());
        assertEquals(PrescriptionStatus.ISSUED, result.getPrescriptions().get(0).getStatus());
        assertNotNull(result.getPrescriptions().get(0).getIssuedAt());

        verify(encounterRepository).save(encounter);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void completeEncounter_Fails_WhenNotOwner() {
        when(encounterRepository.findById(50L)).thenReturn(Optional.of(encounter));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> encounterService.completeEncounter(50L, 2L)); // 2L is different user
    }
}
