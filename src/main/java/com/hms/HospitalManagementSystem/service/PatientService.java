package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.request.PatientRegisterRequest;
import com.hms.HospitalManagementSystem.dto.request.PatientUpdateRequest;
import com.hms.HospitalManagementSystem.dto.response.PatientDetailsResponse;
import com.hms.HospitalManagementSystem.dto.response.PatientResponse;
import com.hms.HospitalManagementSystem.entity.Patient;
import com.hms.HospitalManagementSystem.exception.ConflictException;
import com.hms.HospitalManagementSystem.exception.ResourceNotFoundException;
import com.hms.HospitalManagementSystem.mapper.PatientMapper;
import com.hms.HospitalManagementSystem.repository.PatientRepository;
import com.hms.HospitalManagementSystem.specification.PatientSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Transactional
    public PatientResponse registerPatient(PatientRegisterRequest request) {
        log.info("Registering new patient: {} {}", request.getFirstName(), request.getLastName());

        // Check for duplicates
        if (patientRepository.existsByContact(request.getContact())) {
            throw new ConflictException("Patient with this contact number already exists.");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()
                && patientRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Patient with this email already exists.");
        }

        // Further duplicate check based on Name + DOB + Contact logic if needed
        // The implementation plan mentioned Name + DOB + Contact
        // But unique constraints are usually stricter. We can add specific logic here.
        patientRepository.findPotentialDuplicate(
                request.getFirstName(),
                request.getLastName(),
                request.getDob(),
                request.getContact()).ifPresent(p -> {
                    throw new ConflictException("Duplicate patient record found.");
                });

        Patient patient = patientMapper.toEntity(request);
        Patient savedPatient = patientRepository.save(patient);
        return patientMapper.toResponse(savedPatient);
    }

    @Transactional(readOnly = true)
    public Page<PatientResponse> searchPatients(String query, Pageable pageable) {
        Specification<Patient> spec = PatientSpecification.search(query);
        return patientRepository.findAll(spec, pageable).map(patientMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PatientDetailsResponse getPatientDetails(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + id));
        return patientMapper.toDetailsResponse(patient);
    }

    @Transactional
    public PatientResponse updatePatient(Long id, PatientUpdateRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + id));

        // Optimistic locking check is handled by @Version and Hibernate,
        // but if we want manual version check we could do it here.
        if (request.getVersion() != null && patient.getVersion() != null
                && !request.getVersion().equals(patient.getVersion())) {
            throw new ConflictException(
                    "Patient record has been modified by another transaction. Please refresh and try again.");
        }

        patientMapper.updateEntityFromRequest(request, patient);
        Patient updatedPatient = patientRepository.save(patient);
        return patientMapper.toResponse(updatedPatient);
    }

    @Transactional
    public void deletePatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + id));
        patient.setDeleted(true);
        patient.setActive(false);
        patientRepository.save(patient);
    }
}
