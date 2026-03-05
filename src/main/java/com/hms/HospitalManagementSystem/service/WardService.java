package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.ipd.WardRequest;
import com.hms.HospitalManagementSystem.dto.ipd.WardResponse;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface WardService {
    WardResponse createWard(WardRequest request);

    Slice<WardResponse> getAllWards(org.springframework.data.domain.Pageable pageable);

    WardResponse getWardById(Long id);

    WardResponse updateWard(Long id, WardRequest request);

    void deleteWard(Long id);
}
