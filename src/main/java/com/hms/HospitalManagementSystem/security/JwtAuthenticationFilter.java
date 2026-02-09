package com.hms.HospitalManagementSystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // TODO: Implement JWT extraction and validation logic here
        // String token = extractToken(request);
        // if (token != null && validate(token)) {
        // Authentication auth = getAuthentication(token);
        // SecurityContextHolder.getContext().setAuthentication(auth);
        // }

        filterChain.doFilter(request, response);
    }
}
