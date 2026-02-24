package com.hms.HospitalManagementSystem.service.impl;

import com.hms.HospitalManagementSystem.dto.response.PermissionDto;
import com.hms.HospitalManagementSystem.dto.response.RoleDto;
import com.hms.HospitalManagementSystem.entity.Permission;
import com.hms.HospitalManagementSystem.entity.Role;
import com.hms.HospitalManagementSystem.exception.ResourceNotFoundException;
import com.hms.HospitalManagementSystem.repository.PermissionRepository;
import com.hms.HospitalManagementSystem.repository.RoleRepository;
import com.hms.HospitalManagementSystem.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapRoleToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::mapPermissionToDto)
                .collect(Collectors.toList());
    }

    @Override
    public RoleDto updateRolePermissions(Long roleId, Set<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + roleId));

        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        role.setPermissions(new HashSet<>(permissions));

        return mapRoleToDto(roleRepository.save(role));
    }

    private RoleDto mapRoleToDto(Role role) {
        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(role.getPermissions().stream()
                        .map(this::mapPermissionToDto)
                        .collect(Collectors.toSet()))
                .build();
    }

    private PermissionDto mapPermissionToDto(Permission permission) {
        return PermissionDto.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .module(permission.getModule())
                .build();
    }
}
