package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.ipd.AdmissionRequest;
import com.hms.HospitalManagementSystem.dto.ipd.AdmissionResponse;
import com.hms.HospitalManagementSystem.dto.ipd.DischargeRequest;
import com.hms.HospitalManagementSystem.dto.ipd.TransferRequest;
import com.hms.HospitalManagementSystem.entity.Admission;
import com.hms.HospitalManagementSystem.entity.Bed;
import com.hms.HospitalManagementSystem.entity.Patient;
import com.hms.HospitalManagementSystem.entity.User;
import com.hms.HospitalManagementSystem.enums.AdmissionStatus;
import com.hms.HospitalManagementSystem.exception.BusinessRuleViolationException;
import com.hms.HospitalManagementSystem.exception.ResourceNotFoundException;
import com.hms.HospitalManagementSystem.mapper.IpdMapper;
import com.hms.HospitalManagementSystem.repository.AdmissionRepository;
import com.hms.HospitalManagementSystem.repository.BedRepository;
import com.hms.HospitalManagementSystem.repository.PatientRepository;
import com.hms.HospitalManagementSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdmissionService {

    private final AdmissionRepository admissionRepository;
    private final BedRepository bedRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final BillingService billingService;
    private final IpdMapper ipdMapper;

    @Transactional
    public AdmissionResponse admitPatient(AdmissionRequest request) {
        // 1. Validate Patient
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Patient not found with ID: " + request.getPatientId()));

        // Check if patient is already admitted
        if (admissionRepository.findActiveByPatientId(patient.getId()).isPresent()) {
            throw new BusinessRuleViolationException("Patient is already admitted.");
        }

        // 2. Validate Doctor
        User doctor = userRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + request.getDoctorId()));

        // 3. Lock Bed and Validate Availability
        Bed bed = bedRepository.findByIdWithLock(request.getBedId())
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found with ID: " + request.getBedId()));

        if (bed.isOccupied()) {
            throw new BusinessRuleViolationException("Bed " + bed.getNumber() + " is already occupied.");
        }

        if (!bed.isActive()) {
            throw new BusinessRuleViolationException("Bed " + bed.getNumber() + " is not active / under maintenance.");
        }

        // 4. Update Bed Status
        bed.setOccupied(true);
        bedRepository.save(bed);

        // 5. Create Admission
        Admission admission = new Admission();
        admission.setPatient(patient);
        admission.setDoctor(doctor);
        admission.setBed(bed);
        admission.setDiagnosis(request.getDiagnosis());
        admission.setAdmissionDate(LocalDateTime.now());
        admission.setStatus(AdmissionStatus.ADMITTED);

        admission = admissionRepository.save(admission);

        return ipdMapper.toAdmissionResponse(admission);
    }

    @Transactional
    public AdmissionResponse dischargePatient(Long admissionId, DischargeRequest request) {
        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found with ID: " + admissionId));

        if (admission.getStatus() == AdmissionStatus.DISCHARGED) {
            throw new BusinessRuleViolationException("Patient is already discharged.");
        }

        // 1. Update Admission
        if (request.getDischargeDate() != null && !request.getDischargeDate().isEmpty()) {
            try {
                admission.setDischargeDate(LocalDateTime.parse(request.getDischargeDate()));
            } catch (Exception e) {
                admission.setDischargeDate(LocalDateTime.now());
            }
        } else {
            admission.setDischargeDate(LocalDateTime.now());
        }

        admission.setStatus(AdmissionStatus.DISCHARGED);
        admission.setDischargeSummary(request.getDischargeSummary());

        if (request.getDiagnosis() != null) {
            admission.setDiagnosis(request.getDiagnosis());
        }

        if (request.getAdvice() != null) {
            admission.setAdvice(request.getAdvice());
        }

        // 2. Release Bed
        Bed bed = admission.getBed();
        bed.setOccupied(false);
        bedRepository.save(bed);

        Admission savedAdmission = admissionRepository.save(admission);

        // 3. Trigger Billing
        billingService.generateBill(savedAdmission);

        return ipdMapper.toAdmissionResponse(savedAdmission);
    }

    @Transactional
    public AdmissionResponse transferPatient(Long admissionId, TransferRequest request) {
        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found with ID: " + admissionId));

        if (admission.getStatus() != AdmissionStatus.ADMITTED) {
            throw new BusinessRuleViolationException("Cannot transfer a discharged patient.");
        }

        Bed oldBed = admission.getBed();

        // 1. Lock New Bed
        Bed newBed = bedRepository.findByIdWithLock(request.getNewBedId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Target Bed not found with ID: " + request.getNewBedId()));

        if (newBed.getId().equals(oldBed.getId())) {
            throw new BusinessRuleViolationException("Patient is already in this bed.");
        }

        if (newBed.isOccupied()) {
            throw new BusinessRuleViolationException("Target Bed " + newBed.getNumber() + " is already occupied.");
        }

        if (!newBed.isActive()) {
            throw new BusinessRuleViolationException("Target Bed " + newBed.getNumber() + " is not active.");
        }

        // 2. Release Old Bed
        oldBed.setOccupied(false);
        bedRepository.save(oldBed);

        // 3. Occupy New Bed
        newBed.setOccupied(true);
        bedRepository.save(newBed);

        // 4. Update Admission
        admission.setBed(newBed);
        Admission savedAdmission = admissionRepository.save(admission);

        return ipdMapper.toAdmissionResponse(savedAdmission);
    }

    @Transactional(readOnly = true)
    public List<AdmissionResponse> getActiveAdmissions() {
        return ipdMapper.toAdmissionResponseList(admissionRepository.findByStatus(AdmissionStatus.ADMITTED));
    }

    @Transactional(readOnly = true)
    public AdmissionResponse getAdmissionById(Long id) {
        Admission admission = admissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found with ID: " + id));
        return ipdMapper.toAdmissionResponse(admission);
    }
}
