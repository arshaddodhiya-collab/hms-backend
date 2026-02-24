package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.entity.Vitals;

public interface VitalsService {
        Vitals saveVitals(Long encounterId, Vitals vitalsData, Long userId);

        Vitals getVitalsByEncounterId(Long encounterId);
}
