package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.response.DepartmentDto;

import java.util.List;

public interface DepartmentService {
    org.springframework.data.domain.Slice<DepartmentDto> getAllDepartments(
            org.springframework.data.domain.Pageable pageable);

    DepartmentDto getDepartmentById(Long id);

    DepartmentDto createDepartment(DepartmentDto dto);

    DepartmentDto updateDepartment(Long id, DepartmentDto dto);

    void deleteDepartment(Long id);
}
