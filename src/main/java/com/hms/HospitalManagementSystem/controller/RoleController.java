package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.response.PermissionDto;
import com.hms.HospitalManagementSystem.dto.response.RoleDto;
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
    public ResponseEntity<org.springframework.data.domain.Slice<RoleDto>> getAllRoles(
            @org.springframework.data.web.PageableDefault(sort = "name", direction = org.springframework.data.domain.Sort.Direction.ASC) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(roleService.getAllRoles(pageable));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('CMP_ADMIN_USER_READ')")
    public ResponseEntity<org.springframework.data.domain.Slice<PermissionDto>> getAllPermissions(
            @org.springframework.data.web.PageableDefault(sort = "module", direction = org.springframework.data.domain.Sort.Direction.ASC) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(roleService.getAllPermissions(pageable));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('CMP_ADMIN_USER_WRITE')")
    public ResponseEntity<RoleDto> updateRolePermissions(
            @PathVariable Long id,
            @RequestBody Set<Long> permissionIds) {
        return ResponseEntity.ok(roleService.updateRolePermissions(id, permissionIds));
    }
}
