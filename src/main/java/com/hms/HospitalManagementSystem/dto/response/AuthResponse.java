package com.hms.HospitalManagementSystem.dto.response;

import lombok.*;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private Long id;
    private String username;
    private String role;
    private Set<String> permissions;
}
