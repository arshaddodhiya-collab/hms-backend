package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final com.hms.HospitalManagementSystem.service.UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('CMP_ADMIN_USER_READ')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/doctors")
    @PreAuthorize("hasAnyAuthority('CMP_ADMIN_USER_READ', 'CMP_APPOINTMENT_CREATE', 'CMP_APPOINTMENT_READ')")
    public ResponseEntity<List<UserDto>> getDoctors() {
        return ResponseEntity.ok(userService.getUsersByRole("DOCTOR"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CMP_ADMIN_USER_READ')")
    public ResponseEntity<UserDto> getUserById(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @org.springframework.web.bind.annotation.PostMapping
    @PreAuthorize("hasAuthority('CMP_ADMIN_USER_WRITE')")
    public ResponseEntity<UserDto> createUser(@org.springframework.web.bind.annotation.RequestBody UserDto userDto) {
        return ResponseEntity.ok(userService.createUser(userDto));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CMP_ADMIN_USER_WRITE')")
    public ResponseEntity<UserDto> updateUser(@org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody UserDto userDto) {
        return ResponseEntity.ok(userService.updateUser(id, userDto));
    }
}
