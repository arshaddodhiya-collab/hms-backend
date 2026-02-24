package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.response.PermissionDto;
import com.hms.HospitalManagementSystem.dto.response.RoleDto;

import java.util.List;
import java.util.Set;

public interface RoleService {
    List<RoleDto> getAllRoles();

    List<PermissionDto> getAllPermissions();

    RoleDto updateRolePermissions(Long roleId, Set<Long> permissionIds);
}
