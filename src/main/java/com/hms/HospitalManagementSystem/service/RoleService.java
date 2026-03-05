package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.response.PermissionDto;
import com.hms.HospitalManagementSystem.dto.response.RoleDto;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Set;

public interface RoleService {
    Slice<RoleDto> getAllRoles(org.springframework.data.domain.Pageable pageable);

    Slice<PermissionDto> getAllPermissions(
            org.springframework.data.domain.Pageable pageable);

    RoleDto updateRolePermissions(Long roleId, Set<Long> permissionIds);
}
