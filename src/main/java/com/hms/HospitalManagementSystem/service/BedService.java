package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.ipd.BedRequest;
import com.hms.HospitalManagementSystem.dto.ipd.BedResponse;
import com.hms.HospitalManagementSystem.enums.BedType;

import java.util.List;

public interface BedService {
    BedResponse createBed(BedRequest request);

    org.springframework.data.domain.Slice<BedResponse> getAvailableBeds(Long wardId, BedType type,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Slice<BedResponse> getAllBeds(org.springframework.data.domain.Pageable pageable);

    BedResponse updateBedStatus(Long id, boolean isActive);
}
