package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.request.ChargeCatalogRequest;
import com.hms.HospitalManagementSystem.dto.response.ChargeCatalogResponse;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface ChargeCatalogService {
    ChargeCatalogResponse createCharge(ChargeCatalogRequest request);

    Slice<ChargeCatalogResponse> getAllCharges(
            org.springframework.data.domain.Pageable pageable);

    ChargeCatalogResponse getChargeById(Long id);

    ChargeCatalogResponse updateCharge(Long id, ChargeCatalogRequest request);
}
