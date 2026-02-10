package com.hms.HospitalManagementSystem.dto.request;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String username;
    private String password;
    private String fullName;
    private String department;
    private String role; // Optional, defaults to RECEPTION if null
}
