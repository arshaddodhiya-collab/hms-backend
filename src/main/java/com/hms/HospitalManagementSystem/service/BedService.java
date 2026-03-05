package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.ipd.BedRequest;
import com.hms.HospitalManagementSystem.dto.ipd.BedResponse;
import com.hms.HospitalManagementSystem.enums.BedType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface BedService {
    BedResponse createBed(BedRequest request);

    Slice<BedResponse> getAvailableBeds(Long wardId, BedType type,
            org.springframework.data.domain.Pageable pageable);

    Slice<BedResponse> getAllBeds(org.springframework.data.domain.Pageable pageable);

    BedResponse updateBedStatus(Long id, boolean isActive);
}
