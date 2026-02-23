package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.request.LoginRequest;
import com.hms.HospitalManagementSystem.dto.request.RefreshTokenRequest;
import com.hms.HospitalManagementSystem.dto.request.RegisterRequest;
import com.hms.HospitalManagementSystem.dto.response.AuthResponse;
import com.hms.HospitalManagementSystem.entity.*;
import com.hms.HospitalManagementSystem.exception.ConflictException;
import com.hms.HospitalManagementSystem.exception.ResourceNotFoundException;
import com.hms.HospitalManagementSystem.repository.*;
import com.hms.HospitalManagementSystem.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${app.security.secure-cookie}")
    private boolean secureCookie;

    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ConflictException("Username is already taken!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());

        // Handle Department
        if (request.getDepartment() != null && !request.getDepartment().isEmpty()) {
            Department dept = departmentRepository.findByName(request.getDepartment())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Department not found: " + request.getDepartment()));
            user.setDepartment(dept);
        }

        Set<Role> roles = new HashSet<>();
        // Use provided role or default to RECEPTION
        String roleInput = (request.getRole() != null && !request.getRole().isEmpty())
                ? request.getRole()
                : "RECEPTION";

        // Normalize Role: "Front Desk" -> "RECEPTION", "Doctor" -> "DOCTOR"
        String roleName = normalizeRoleName(roleInput);

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
        roles.add(role);
        user.setRoles(roles);

        userRepository.save(user);

        return login(new LoginRequest() {
            {
                setUsername(request.getUsername());
                setPassword(request.getPassword());
            }
        }, response);
    }

    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        String accessToken = jwtService.generateTokenWithUserId(userDetails, user.getId());
        String refreshToken = createRefreshToken(user).getToken();

        addTokenCookie(response, "accessToken", accessToken, jwtExpiration / 1000);
        addTokenCookie(response, "refreshToken", refreshToken, refreshExpiration / 1000);

        String roleName = user.getRoles().isEmpty() ? "" : user.getRoles().iterator().next().getName();
        Set<String> permissions = new HashSet<>();
        user.getRoles().forEach(role -> role.getPermissions().forEach(p -> permissions.add(p.getCode())));

        return new AuthResponse(user.getId(), user.getUsername(), roleName, permissions);
    }

    public AuthResponse refreshToken(RefreshTokenRequest requestBody, HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = null;

        // 1. Try getting from body
        if (requestBody != null && requestBody.getRefreshToken() != null) {
            refreshToken = requestBody.getRefreshToken();
        }

        // 2. If not in body, try from cookie
        if (refreshToken == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            throw new BadCredentialsException("Refresh token is missing!");
        }

        return refreshTokenRepository.findByToken(refreshToken)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
                    String accessToken = jwtService.generateTokenWithUserId(userDetails, user.getId());

                    addTokenCookie(response, "accessToken", accessToken, jwtExpiration / 1000);

                    String roleName = user.getRoles().isEmpty() ? "" : user.getRoles().iterator().next().getName();
                    Set<String> permissions = new HashSet<>();
                    user.getRoles().forEach(role -> role.getPermissions().forEach(p -> permissions.add(p.getCode())));

                    return new AuthResponse(user.getId(), user.getUsername(), roleName, permissions);
                })
                .orElseThrow(() -> new BadCredentialsException("Refresh token is not in database!"));
    }

    public void logout(HttpServletResponse response) {
        // Clear cookies
        addTokenCookie(response, "accessToken", null, 0);
        addTokenCookie(response, "refreshToken", null, 0);
    }

    // Kept for signature compatibility if needed, but getCurrentUser usually just
    // needs user info
    public AuthResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String roleName = user.getRoles().isEmpty() ? "" : user.getRoles().iterator().next().getName();
        Set<String> permissions = new HashSet<>();
        user.getRoles().forEach(role -> role.getPermissions().forEach(p -> permissions.add(p.getCode())));

        return new AuthResponse(user.getId(), user.getUsername(), roleName, permissions);
    }

    private void addTokenCookie(HttpServletResponse response, String name, String value,
            long maxAge) {

        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(new RefreshToken());

        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpiration));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(java.time.Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new BadCredentialsException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    private String normalizeRoleName(String input) {
        String normalized = input.trim().toUpperCase();
        switch (normalized) {
            case "ADMINISTRATOR":
            case "ADMIN":
                return "ADMIN";
            case "DOCTOR":
            case "MEDICAL DOCTOR":
                return "DOCTOR";
            case "NURSE":
                return "NURSE";
            case "LAB TECHNICIAN":
            case "LAB":
            case "LAB_TECH":
                return "LAB_TECH";
            case "FRONT DESK":
            case "RECEPTIONIST":
            case "RECEPTION":
                return "RECEPTION";
            default:
                // Fallback: Replace spaces with underscores and return uppercase
                return normalized.replace(" ", "_");
        }
    }
}
