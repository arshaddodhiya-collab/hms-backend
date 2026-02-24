package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.entity.Prescription;
import com.hms.HospitalManagementSystem.entity.PrescriptionItem;

import java.util.List;

public interface PrescriptionService {
    Prescription savePrescription(Long encounterId, String note, List<PrescriptionItem> itemsData, Long userId);

    Prescription getPrescriptionByEncounterId(Long encounterId);
}
