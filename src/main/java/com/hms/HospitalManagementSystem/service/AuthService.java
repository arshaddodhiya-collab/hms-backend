package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.request.LoginRequest;
import com.hms.HospitalManagementSystem.dto.request.RefreshTokenRequest;
import com.hms.HospitalManagementSystem.dto.request.RegisterRequest;
import com.hms.HospitalManagementSystem.dto.response.AuthResponse;
import com.hms.HospitalManagementSystem.entity.RefreshToken;
import com.hms.HospitalManagementSystem.entity.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request, HttpServletResponse response);

    AuthResponse login(LoginRequest request, HttpServletResponse response);

    AuthResponse refreshToken(RefreshTokenRequest requestBody, HttpServletRequest request,
            HttpServletResponse response);

    void logout(HttpServletResponse response);

    AuthResponse getCurrentUser(String username);

    RefreshToken createRefreshToken(User user);

    RefreshToken verifyExpiration(RefreshToken token);
}
