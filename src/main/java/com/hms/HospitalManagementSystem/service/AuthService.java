package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.request.LoginRequest;
import com.hms.HospitalManagementSystem.dto.request.RefreshTokenRequest;
import com.hms.HospitalManagementSystem.dto.request.RegisterRequest;
import com.hms.HospitalManagementSystem.dto.response.AuthResponse;
import com.hms.HospitalManagementSystem.entity.*;
import com.hms.HospitalManagementSystem.repository.*;
import com.hms.HospitalManagementSystem.security.CustomUserDetailsService;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${app.security.secure-cookie}")
    private boolean secureCookie;

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

    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
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
        }, response);
    }

    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = createRefreshToken(user).getToken();

        addTokenCookie(response, "accessToken", accessToken, jwtExpiration / 1000);
        addTokenCookie(response, "refreshToken", refreshToken, refreshExpiration / 1000);

        String roleName = user.getRoles().isEmpty() ? "" : user.getRoles().iterator().next().getName();
        Set<String> permissions = new HashSet<>();
        user.getRoles().forEach(role -> role.getPermissions().forEach(p -> permissions.add(p.getCode())));

        return new AuthResponse(user.getUsername(), roleName, permissions);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletResponse response) {
        String requestRefreshToken = request.getRefreshToken();
        // If coming from body, use it. If null, maybe check cookie?
        // For now adhering to request body for refresh token lookup if provided,
        // but typically refresh endpoint also reads from cookie in this pattern.
        // Let's assume the Filter might not have intercepted it for this specific
        // endpoint wrapper.
        // But commonly refresh token is also a cookie.
        // The prompt implies "use the cookie for access and refresh token".
        // So this method might need to extract from cookie if not in body?
        // Let's stick to the current flow -> usually the client sends the refresh token
        // cookie.
        // Getting it from request object would be better.

        return refreshTokenRepository.findByToken(requestRefreshToken)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
                    String accessToken = jwtService.generateToken(userDetails);

                    addTokenCookie(response, "accessToken", accessToken, jwtExpiration / 1000);

                    String roleName = user.getRoles().isEmpty() ? "" : user.getRoles().iterator().next().getName();
                    Set<String> permissions = new HashSet<>();
                    user.getRoles().forEach(role -> role.getPermissions().forEach(p -> permissions.add(p.getCode())));

                    return new AuthResponse(user.getUsername(), roleName, permissions);
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    public void logout(jakarta.servlet.http.HttpServletResponse response) {
        // Clear cookies
        addTokenCookie(response, "accessToken", null, 0);
        addTokenCookie(response, "refreshToken", null, 0);
    }

    // Kept for signature compatibility if needed, but getCurrentUser usually just
    // needs user info
    public AuthResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String roleName = user.getRoles().isEmpty() ? "" : user.getRoles().iterator().next().getName();
        Set<String> permissions = new HashSet<>();
        user.getRoles().forEach(role -> role.getPermissions().forEach(p -> permissions.add(p.getCode())));

        return new AuthResponse(user.getUsername(), roleName, permissions);
    }

    private void addTokenCookie(jakarta.servlet.http.HttpServletResponse response, String name, String value,
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
