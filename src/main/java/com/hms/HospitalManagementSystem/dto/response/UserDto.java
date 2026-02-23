package com.hms.HospitalManagementSystem.dto.response;

import lombok.*;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String fullName;
    private String departmentName;
    private Long departmentId;
    private Set<String> roles;
    private boolean active;
    private String password; // Optional for updates, required for create (handled in service)
}
