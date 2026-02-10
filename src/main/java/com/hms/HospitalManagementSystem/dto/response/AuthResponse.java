package com.hms.HospitalManagementSystem.dto.response;

import java.util.Set;

public class AuthResponse {
    private String username;
    private String role;
    private Set<String> permissions;

    public AuthResponse(String username, String role, Set<String> permissions) {
        this.username = username;
        this.role = role;
        this.permissions = permissions;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
