package com.hms.HospitalManagementSystem.service.impl;

import com.hms.HospitalManagementSystem.entity.Encounter;
import com.hms.HospitalManagementSystem.entity.Vitals;
import com.hms.HospitalManagementSystem.entity.User;
import com.hms.HospitalManagementSystem.repository.EncounterRepository;
import com.hms.HospitalManagementSystem.repository.UserRepository;
import com.hms.HospitalManagementSystem.repository.VitalsRepository;
import com.hms.HospitalManagementSystem.exception.ResourceNotFoundException;
import com.hms.HospitalManagementSystem.service.VitalsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VitalsServiceImpl implements VitalsService {

    private final VitalsRepository vitalsRepository;
    private final EncounterRepository encounterRepository;
    private final UserRepository userRepository;
    // private final EncounterService encounterService; // To create encounter if
    // needed

    @Override
    @Transactional
    public Vitals saveVitals(Long encounterId, Vitals vitalsData, Long userId) {
        // vitalsData acts as DTO here for simplicity, or I can extract fields.
        // Logic:
        // 1. Get or create encounter (if appointmentId providing... wait, the analysis
        // says POST /encounters/{encounterId}/vitals. So encounter must exist or be
        // created via appointment?)
        // Analysis 4.2.1: "Records vitals for an encounter (creates encounter if
        // doesn't exist)". But the endpoint is /encounters/{encounterId}/vitals.
        // If encounterId is passed, it implies encounter exists.
        // But the business logic says: "If encounter doesn't exist for appointment,
        // create it".
        // This implies the input might be Appointment ID?
        // Let's look at the API design again: POST
        // /api/v1/encounters/{encounterId}/vitals.
        // This implies encounterId IS known.
        // OR the endpoint might be /appointments/{appointmentId}/vitals?
        // No, it says /encounters/{encounterId}/vitals.
        // Maybe the "create if not exist" is for when the NURSE enters vitals from
        // Triage queue (which has appointmentId).
        // If the UI calls this, it probably has an encounter ID if the encounter was
        // created.
        // If not, maybe it calls a different endpoint?
        // Let's assume encounterId is valid.

        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if vitals already exist
        Vitals vitals = vitalsRepository.findByEncounterId(encounterId)
                .orElse(new Vitals());

        vitals.setEncounter(encounter);
        vitals.setRecordedBy(user);
        vitals.setTemperature(vitalsData.getTemperature());
        vitals.setSystolic(vitalsData.getSystolic());
        vitals.setDiastolic(vitalsData.getDiastolic());
        vitals.setPulse(vitalsData.getPulse());
        vitals.setSpo2(vitalsData.getSpo2());
        vitals.setWeight(vitalsData.getWeight());
        vitals.setHeight(vitalsData.getHeight());

        // Calculate BMI
        if (vitals.getWeight() != null && vitals.getHeight() != null
                && vitals.getHeight().compareTo(BigDecimal.ZERO) > 0) {
            // formula: weight (kg) / [height (m)]^2
            BigDecimal heightInMeters = vitals.getHeight().divide(BigDecimal.valueOf(100), 2,
                    java.math.RoundingMode.HALF_UP);
            vitals.setBmi(vitals.getWeight().divide(heightInMeters.pow(2), 2,
                    java.math.RoundingMode.HALF_UP));
        }

        vitals.setRecordedAt(LocalDateTime.now());

        return vitalsRepository.save(vitals);
    }

    @Override
    public Vitals getVitalsByEncounterId(Long encounterId) {
        return vitalsRepository.findByEncounterId(encounterId)
                .orElseThrow(() -> new ResourceNotFoundException("Vitals not found for encounter"));
    }
}
