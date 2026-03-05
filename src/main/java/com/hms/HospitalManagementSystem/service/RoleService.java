package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.response.PermissionDto;
import com.hms.HospitalManagementSystem.dto.response.RoleDto;

import java.util.List;
import java.util.Set;

public interface RoleService {
    org.springframework.data.domain.Slice<RoleDto> getAllRoles(org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Slice<PermissionDto> getAllPermissions(
            org.springframework.data.domain.Pageable pageable);

    RoleDto updateRolePermissions(Long roleId, Set<Long> permissionIds);
}
