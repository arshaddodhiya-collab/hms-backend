package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.PermissionDto;
import com.hms.HospitalManagementSystem.dto.RoleDto;
import com.hms.HospitalManagementSystem.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('CMP_ADMIN_USER_READ')")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('CMP_ADMIN_USER_READ')")
    public ResponseEntity<List<PermissionDto>> getAllPermissions() {
        return ResponseEntity.ok(roleService.getAllPermissions());
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('CMP_ADMIN_USER_WRITE')")
    public ResponseEntity<RoleDto> updateRolePermissions(
            @PathVariable Long id,
            @RequestBody Set<Long> permissionIds) {
        return ResponseEntity.ok(roleService.updateRolePermissions(id, permissionIds));
    }
}
