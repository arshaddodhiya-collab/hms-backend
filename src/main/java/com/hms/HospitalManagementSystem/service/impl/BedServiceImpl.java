package com.hms.HospitalManagementSystem.service.impl;

import com.hms.HospitalManagementSystem.dto.ipd.BedRequest;
import com.hms.HospitalManagementSystem.dto.ipd.BedResponse;
import com.hms.HospitalManagementSystem.entity.Bed;
import com.hms.HospitalManagementSystem.entity.Ward;
import com.hms.HospitalManagementSystem.enums.BedType;
import com.hms.HospitalManagementSystem.exception.ResourceNotFoundException;
import com.hms.HospitalManagementSystem.mapper.IpdMapper;
import com.hms.HospitalManagementSystem.repository.BedRepository;
import com.hms.HospitalManagementSystem.repository.WardRepository;
import com.hms.HospitalManagementSystem.service.BedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BedServiceImpl implements BedService {

    private final BedRepository bedRepository;
    private final WardRepository wardRepository;
    private final IpdMapper ipdMapper;

    @Override
    @Transactional
    public BedResponse createBed(BedRequest request) {
        Ward ward = wardRepository.findById(request.getWardId())
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with ID: " + request.getWardId()));

        // Check capacity? Or just let them add beds?
        // Check for duplicate bed number in ward via DB constraint or check here

        Bed bed = ipdMapper.toBed(request);
        bed.setWard(ward);
        bed = bedRepository.save(bed);
        return ipdMapper.toBedResponse(bed);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Slice<BedResponse> getAvailableBeds(Long wardId, BedType type,
            org.springframework.data.domain.Pageable pageable) {
        if (type != null) {
            return bedRepository.findAvailableBedsByWardAndType(wardId, type, pageable)
                    .map(ipdMapper::toBedResponse);
        } else {
            return bedRepository.findAvailableBedsByWard(wardId, pageable)
                    .map(ipdMapper::toBedResponse);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Slice<BedResponse> getAllBeds(
            org.springframework.data.domain.Pageable pageable) {
        return bedRepository.findAllBy(pageable)
                .map(ipdMapper::toBedResponse);
    }

    @Override
    @Transactional
    public BedResponse updateBedStatus(Long id, boolean isActive) {
        Bed bed = bedRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found with ID: " + id));
        bed.setActive(isActive);
        bed = bedRepository.save(bed);
        return ipdMapper.toBedResponse(bed);
    }
}
