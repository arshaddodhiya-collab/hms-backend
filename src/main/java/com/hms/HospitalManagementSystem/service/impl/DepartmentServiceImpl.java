package com.hms.HospitalManagementSystem.service.impl;

import com.hms.HospitalManagementSystem.dto.response.DepartmentDto;
import com.hms.HospitalManagementSystem.entity.Department;
import com.hms.HospitalManagementSystem.entity.User;
import com.hms.HospitalManagementSystem.repository.DepartmentRepository;
import com.hms.HospitalManagementSystem.repository.UserRepository;
import com.hms.HospitalManagementSystem.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Override
    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentDto getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with ID: " + id));
        return mapToDto(dept);
    }

    @Override
    public DepartmentDto createDepartment(DepartmentDto dto) {
        if (departmentRepository.existsByName(dto.getName())) {
            throw new com.hms.HospitalManagementSystem.exception.ConflictException(
                    "Department with name '" + dto.getName() + "' already exists.");
        }

        Department dept = new Department();
        dept.setName(dto.getName());
        dept.setDescription(dto.getDescription());

        if (dto.getHeadOfDepartmentId() != null) {
            User hod = userRepository.findById(dto.getHeadOfDepartmentId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + dto.getHeadOfDepartmentId()));
            dept.setHeadOfDepartment(hod);
        }

        Department saved = departmentRepository.save(dept);
        return mapToDto(saved);
    }

    @Override
    public DepartmentDto updateDepartment(Long id, DepartmentDto dto) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with ID: " + id));

        // Check name uniqueness if changed
        if (!dept.getName().equals(dto.getName()) && departmentRepository.existsByName(dto.getName())) {
            throw new com.hms.HospitalManagementSystem.exception.ConflictException(
                    "Department with name '" + dto.getName() + "' already exists.");
        }

        dept.setName(dto.getName());
        dept.setDescription(dto.getDescription());
        dept.setActive(dto.isActive());

        if (dto.getHeadOfDepartmentId() != null) {
            User hod = userRepository.findById(dto.getHeadOfDepartmentId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + dto.getHeadOfDepartmentId()));
            dept.setHeadOfDepartment(hod);
        } else {
            dept.setHeadOfDepartment(null);
        }

        return mapToDto(dept);
    }

    @Override
    public void deleteDepartment(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with ID: " + id));

        if (!dept.getUsers().isEmpty()) {
            throw new com.hms.HospitalManagementSystem.exception.ConflictException(
                    "Cannot delete department with active staff members.");
        }

        departmentRepository.delete(dept);
    }

    private DepartmentDto mapToDto(Department dept) {
        return DepartmentDto.builder()
                .id(dept.getId())
                .name(dept.getName())
                .description(dept.getDescription())
                .headOfDepartmentName(
                        dept.getHeadOfDepartment() != null ? dept.getHeadOfDepartment().getFullName() : null)
                .headOfDepartmentId(dept.getHeadOfDepartment() != null ? dept.getHeadOfDepartment().getId() : null)
                .staffCount(dept.getUsers().size())
                .active(dept.isActive())
                .build();
    }
}
