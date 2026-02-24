package com.hms.HospitalManagementSystem.service;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;
import java.util.function.Function;

public interface JwtService {
    String extractUsername(String token);

    Long extractUserId(String token);

    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);

    String generateToken(UserDetails userDetails);

    String generateToken(Map<String, Object> extraClaims, UserDetails userDetails);

    String generateTokenWithUserId(UserDetails userDetails, Long userId);

    boolean isTokenValid(String token, UserDetails userDetails);

    boolean isTokenValid(String token, UserDetails userDetails, Long userId);
}
