package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.entity.Encounter;
import com.hms.HospitalManagementSystem.entity.Prescription;
import com.hms.HospitalManagementSystem.entity.PrescriptionItem;
import com.hms.HospitalManagementSystem.entity.User;
import com.hms.HospitalManagementSystem.enums.EncounterStatus;
import com.hms.HospitalManagementSystem.enums.PrescriptionStatus;
import com.hms.HospitalManagementSystem.exception.ConflictException;
import com.hms.HospitalManagementSystem.exception.ResourceNotFoundException;
import com.hms.HospitalManagementSystem.repository.EncounterRepository;
import com.hms.HospitalManagementSystem.repository.PrescriptionRepository;
import com.hms.HospitalManagementSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final EncounterRepository encounterRepository;
    private final UserRepository userRepository;

    @Transactional
    public Prescription savePrescription(Long encounterId, String note, List<PrescriptionItem> itemsData, Long userId) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found"));

        if (encounter.getStatus() == EncounterStatus.COMPLETED) {
            throw new ConflictException("Cannot add prescription to completed encounter");
        }

        if (!encounter.getDoctor().getId().equals(userId)) {
            throw new AccessDeniedException("You can only prescribe for your own encounters");
        }

        // Check if prescription already exists for this encounter (assuming one
        // prescription per encounter as per analysis UI flow, or create new one?)
        // Analysis 4.3.1: "Creates or updates prescription". "Replace all items with
        // new items".
        // So we should find existing DRAFT prescription or create new.

        List<Prescription> existingPrescriptions = prescriptionRepository.findByEncounterId(encounterId);
        Prescription prescription;

        if (!existingPrescriptions.isEmpty()) {
            // Assume the first one is the active draft or the only one.
            // If multiple, logic might need adjustment. For now take the first DRAFT one.
            prescription = existingPrescriptions.stream()
                    .filter(p -> p.getStatus() == PrescriptionStatus.DRAFT)
                    .findFirst()
                    .orElse(new Prescription());

            if (prescription.getId() == null) {
                // No draft found, create new
                prescription.setEncounter(encounter);
                prescription.setStatus(PrescriptionStatus.DRAFT);
            }
        } else {
            prescription = new Prescription();
            prescription.setEncounter(encounter);
            prescription.setStatus(PrescriptionStatus.DRAFT);
        }

        prescription.setNote(note);

        // Handle items
        // Since we want to replace all items, we can clear the list and add new ones.
        // But we need to map itemsData to new PrescriptionItem entities associated with
        // 'prescription'.
        // Or if itemsData are already entities (from controller), we need to
        // re-associate or copy.
        // I'll assume itemsData contains the data we want to save.

        if (prescription.getItems() != null) {
            prescription.getItems().clear();
        } else {
            // initializing list handled by builder default or new ArrayList in entity?
            // Entity has `new ArrayList<>()` initialization.
        }

        if (itemsData != null) {
            for (PrescriptionItem itemData : itemsData) {
                PrescriptionItem item = PrescriptionItem.builder()
                        .medicineName(itemData.getMedicineName())
                        .dosage(itemData.getDosage())
                        .frequency(itemData.getFrequency())
                        .duration(itemData.getDuration())
                        .prescription(prescription)
                        .build();
                prescription.getItems().add(item);
            }
        }

        return prescriptionRepository.save(prescription);
    }

    public Prescription getPrescriptionByEncounterId(Long encounterId) {
        return prescriptionRepository.findByEncounterId(encounterId).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found for encounter"));
    }
}
