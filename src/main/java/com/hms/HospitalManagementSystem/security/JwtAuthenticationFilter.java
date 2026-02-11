package com.hms.HospitalManagementSystem.security;

import com.hms.HospitalManagementSystem.entity.User;
import com.hms.HospitalManagementSystem.repository.UserRepository;
import com.hms.HospitalManagementSystem.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService,
            UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extract Token from Cookie
        String token = null;
        String username = null;
        Long userId = null;

        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null) {
            try {
                username = jwtService.extractUsername(token);
                userId = jwtService.extractUserId(token);
            } catch (IllegalArgumentException e) {
                logger.error("Unable to get JWT Token");
            } catch (ExpiredJwtException e) {
                logger.error("JWT Token has expired");
            } catch (MalformedJwtException e) {
                logger.error("Invalid JWT Token");
            }
        }

        // 2. Validate Token & Set Context
        if (username != null && userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Verify user exists with matching username and ID
            User user = userRepository.findByUsername(username).orElse(null);

            if (user != null && user.getId().equals(userId)) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // Validate token with both username and userId
                if (jwtService.isTokenValid(token, userDetails, userId)) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 3. Set Authentication in Security Context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    logger.warn("Token validation failed for user: {} with userId: {}", username, userId);
                }
            } else {
                logger.warn("User ID mismatch or user not found. Username: {}, Token userId: {}", username, userId);
            }
        }

        filterChain.doFilter(request, response);
    }
}
