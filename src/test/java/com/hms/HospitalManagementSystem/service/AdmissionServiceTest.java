package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.ipd.AdmissionRequest;
import com.hms.HospitalManagementSystem.dto.ipd.AdmissionResponse;
import com.hms.HospitalManagementSystem.entity.Admission;
import com.hms.HospitalManagementSystem.entity.Bed;
import com.hms.HospitalManagementSystem.entity.Patient;
import com.hms.HospitalManagementSystem.entity.User;
// import com.hms.HospitalManagementSystem.enums.AdmissionStatus;
import com.hms.HospitalManagementSystem.enums.BedType;
import com.hms.HospitalManagementSystem.exception.BusinessRuleViolationException;
import com.hms.HospitalManagementSystem.mapper.IpdMapper;
import com.hms.HospitalManagementSystem.repository.AdmissionRepository;
import com.hms.HospitalManagementSystem.repository.BedRepository;
import com.hms.HospitalManagementSystem.repository.EncounterRepository;
import com.hms.HospitalManagementSystem.repository.PatientRepository;
import com.hms.HospitalManagementSystem.repository.UserRepository;
// import com.hms.HospitalManagementSystem.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceTest {

    @Mock
    private AdmissionRepository admissionRepository;
    @Mock
    private BedRepository bedRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EncounterRepository encounterRepository;
    @Mock
    private IpdMapper ipdMapper;
    @Mock
    private AppointmentService appointmentService;

    @InjectMocks
    private AdmissionService admissionService;

    private Patient patient;
    private User doctor;
    private Bed bed;
    private AdmissionRequest request;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(1L);

        doctor = new User();
        doctor.setId(2L);

        bed = new Bed();
        bed.setId(3L);
        bed.setNumber("101");
        bed.setType(BedType.GENERAL);
        bed.setActive(true);
        bed.setOccupied(false);

        request = new AdmissionRequest();
        request.setPatientId(1L);
        request.setDoctorId(2L);
        request.setBedId(3L);
        request.setDiagnosis("Flu");
    }

    @Test
    void admitPatient_Success() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(userRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(bedRepository.findByIdWithLock(3L)).thenReturn(Optional.of(bed)); // Mocking the lock
        when(admissionRepository.findActiveByPatientId(1L)).thenReturn(Optional.empty());
        when(admissionRepository.save(any(Admission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bedRepository.save(any(Bed.class))).thenReturn(bed);
        when(ipdMapper.toAdmissionResponse(any(Admission.class))).thenReturn(new AdmissionResponse());

        AdmissionResponse response = admissionService.admitPatient(request);

        assertNotNull(response);
        assertTrue(bed.isOccupied());
        verify(bedRepository).save(bed);
        verify(admissionRepository).save(any(Admission.class));
    }

    @Test
    void admitPatient_BedOccupied_ThrowsException() {
        bed.setOccupied(true);

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(userRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(bedRepository.findByIdWithLock(3L)).thenReturn(Optional.of(bed));
        when(admissionRepository.findActiveByPatientId(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleViolationException.class, () -> admissionService.admitPatient(request));
        verify(admissionRepository, never()).save(any(Admission.class));
    }

    @Test
    void admitPatient_PatientAlreadyAdmitted_ThrowsException() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(admissionRepository.findActiveByPatientId(1L)).thenReturn(Optional.of(new Admission()));

        assertThrows(BusinessRuleViolationException.class, () -> admissionService.admitPatient(request));
    }
}
