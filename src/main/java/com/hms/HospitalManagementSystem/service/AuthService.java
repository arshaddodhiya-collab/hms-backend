package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.request.LoginRequest;
import com.hms.HospitalManagementSystem.dto.request.RefreshTokenRequest;
import com.hms.HospitalManagementSystem.dto.request.RegisterRequest;
import com.hms.HospitalManagementSystem.dto.response.AuthResponse;
import com.hms.HospitalManagementSystem.entity.*;
import com.hms.HospitalManagementSystem.repository.*;
import com.hms.HospitalManagementSystem.security.CustomUserDetailsService;
import org.springframework.security.authentication.AuthenticationManager;
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
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    public AuthService(UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username is already taken!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setDepartment(request.getDepartment());

        Set<Role> roles = new HashSet<>();
        // Use provided role or default to RECEPTION
        String roleInput = (request.getRole() != null && !request.getRole().isEmpty())
                ? request.getRole()
                : "RECEPTION";

        // Normalize Role: "Front Desk" -> "RECEPTION", "Doctor" -> "DOCTOR"
        String roleName = normalizeRoleName(roleInput);

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        roles.add(role);
        user.setRoles(roles);

        userRepository.save(user);

        return login(new LoginRequest() {
            {
                setUsername(request.getUsername());
                setPassword(request.getPassword());
            }
        });
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = createRefreshToken(user).getToken();

        String roleName = user.getRoles().isEmpty() ? "" : user.getRoles().iterator().next().getName();
        Set<String> permissions = new HashSet<>();
        user.getRoles().forEach(role -> role.getPermissions().forEach(p -> permissions.add(p.getCode())));

        return new AuthResponse(accessToken, refreshToken, user.getUsername(), roleName, permissions);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenRepository.findByToken(requestRefreshToken)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
                    String accessToken = jwtService.generateToken(userDetails);

                    String roleName = user.getRoles().isEmpty() ? "" : user.getRoles().iterator().next().getName();
                    Set<String> permissions = new HashSet<>();
                    user.getRoles().forEach(role -> role.getPermissions().forEach(p -> permissions.add(p.getCode())));

                    return new AuthResponse(accessToken, requestRefreshToken, user.getUsername(), roleName,
                            permissions);
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(refreshTokenRepository::delete);
    }

    // Since we don't have a direct way to get the current user entity from context
    // easily without loading it,
    // and usually the controller handles extracting the principal.
    // But we can implement a method that takes the username (extracted from token
    // in controller)
    public AuthResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String roleName = user.getRoles().isEmpty() ? "" : user.getRoles().iterator().next().getName();
        Set<String> permissions = new HashSet<>();
        user.getRoles().forEach(role -> role.getPermissions().forEach(p -> permissions.add(p.getCode())));

        // We don't return tokens for /me endpoint usually, or return null/empty
        return new AuthResponse(null, null, user.getUsername(), roleName, permissions);
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(new RefreshToken());

        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(604800000)); // 7 days
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
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
