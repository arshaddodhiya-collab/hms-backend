package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.ipd.BedRequest;
import com.hms.HospitalManagementSystem.dto.ipd.BedResponse;
import com.hms.HospitalManagementSystem.enums.BedType;

import java.util.List;

public interface BedService {
    BedResponse createBed(BedRequest request);

    List<BedResponse> getAvailableBeds(Long wardId, BedType type);

    List<BedResponse> getAllBeds();

    BedResponse updateBedStatus(Long id, boolean isActive);
}
