package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.ipd.WardRequest;
import com.hms.HospitalManagementSystem.dto.ipd.WardResponse;

import java.util.List;

public interface WardService {
    WardResponse createWard(WardRequest request);

    List<WardResponse> getAllWards();

    WardResponse getWardById(Long id);

    WardResponse updateWard(Long id, WardRequest request);

    void deleteWard(Long id);
}
