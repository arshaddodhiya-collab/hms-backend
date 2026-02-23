package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.response.DepartmentDto;
import com.hms.HospitalManagementSystem.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_READ')")
    public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_READ')")
    public ResponseEntity<DepartmentDto> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_WRITE')")
    public ResponseEntity<DepartmentDto> createDepartment(@RequestBody DepartmentDto departmentDto) {
        return ResponseEntity.ok(departmentService.createDepartment(departmentDto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_WRITE')")
    public ResponseEntity<DepartmentDto> updateDepartment(@PathVariable Long id,
            @RequestBody DepartmentDto departmentDto) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, departmentDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CMP_ADMIN_DEPT_DELETE')")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
