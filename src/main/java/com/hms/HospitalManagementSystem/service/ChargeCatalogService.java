package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.request.ChargeCatalogRequest;
import com.hms.HospitalManagementSystem.dto.response.ChargeCatalogResponse;
import com.hms.HospitalManagementSystem.entity.ChargeCatalog;
import com.hms.HospitalManagementSystem.exception.ResourceNotFoundException;
import com.hms.HospitalManagementSystem.mapper.ChargeCatalogMapper;
import com.hms.HospitalManagementSystem.repository.ChargeCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChargeCatalogService {

    private final ChargeCatalogRepository chargeCatalogRepository;
    private final ChargeCatalogMapper chargeCatalogMapper;

    @Transactional
    public ChargeCatalogResponse createCharge(ChargeCatalogRequest request) {
        log.info("Creating charge catalog item: {}", request.getCode());
        if (chargeCatalogRepository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("Charge code with code " + request.getCode() + " already exists");
        }
        ChargeCatalog chargeCatalog = chargeCatalogMapper.toEntity(request);
        ChargeCatalog savedParams = chargeCatalogRepository.save(chargeCatalog);
        return chargeCatalogMapper.toResponse(savedParams);
    }

    @Transactional(readOnly = true)
    public List<ChargeCatalogResponse> getAllCharges() {
        return chargeCatalogRepository.findAll().stream()
                .map(chargeCatalogMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChargeCatalogResponse getChargeById(Long id) {
        ChargeCatalog chargeCatalog = chargeCatalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charge not found with id: " + id));
        return chargeCatalogMapper.toResponse(chargeCatalog);
    }

    @Transactional
    public ChargeCatalogResponse updateCharge(Long id, ChargeCatalogRequest request) {
        log.info("Updating charge catalog item: {}", id);
        ChargeCatalog existingCharge = chargeCatalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charge not found with id: " + id));

        // Update fields
        existingCharge.setName(request.getName());
        existingCharge.setDescription(request.getDescription());
        existingCharge.setServiceType(request.getServiceType());
        existingCharge.setStandardPrice(request.getStandardPrice());
        existingCharge.setActive(request.isActive());

        // Note: Code should typically not be changeable, or handle conflict if changed

        ChargeCatalog savedCharge = chargeCatalogRepository.save(existingCharge);
        return chargeCatalogMapper.toResponse(savedCharge);
    }
}
