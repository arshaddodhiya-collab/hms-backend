package com.hms.HospitalManagementSystem.service.impl;

import com.hms.HospitalManagementSystem.dto.ipd.WardRequest;
import com.hms.HospitalManagementSystem.dto.ipd.WardResponse;
import com.hms.HospitalManagementSystem.entity.Ward;
import com.hms.HospitalManagementSystem.exception.ResourceNotFoundException;
import com.hms.HospitalManagementSystem.mapper.IpdMapper;
import com.hms.HospitalManagementSystem.repository.WardRepository;
import com.hms.HospitalManagementSystem.service.WardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WardServiceImpl implements WardService {

    private final WardRepository wardRepository;
    private final IpdMapper ipdMapper;

    @Override
    @Transactional
    public WardResponse createWard(WardRequest request) {
        if (wardRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Ward with name " + request.getName() + " already exists");
        }
        Ward ward = ipdMapper.toWard(request);
        ward = wardRepository.save(ward);
        return ipdMapper.toWardResponse(ward);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WardResponse> getAllWards() {
        return ipdMapper.toWardResponseList(wardRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public WardResponse getWardById(Long id) {
        Ward ward = wardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with ID: " + id));
        return ipdMapper.toWardResponse(ward);
    }

    @Override
    @Transactional
    public WardResponse updateWard(Long id, WardRequest request) {
        Ward ward = wardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with ID: " + id));

        ward.setName(request.getName());
        ward.setType(request.getType());
        ward.setCapacity(request.getCapacity());
        ward.setActive(request.isActive());

        ward = wardRepository.save(ward);
        return ipdMapper.toWardResponse(ward);
    }

    @Override
    @Transactional
    public void deleteWard(Long id) {
        if (!wardRepository.existsById(id)) {
            throw new ResourceNotFoundException("Ward not found with ID: " + id);
        }
        // TODO: Check for active beds/patients before deleting?
        wardRepository.deleteById(id);
    }
}
