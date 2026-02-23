package com.hms.HospitalManagementSystem.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private ResponseEntity<Object> buildErrorResponse(Exception ex, WebRequest request, HttpStatus status,
            String errorType, String message) {
        log.error("Exception handled: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", errorType);
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        return buildErrorResponse(ex, request, HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflictException(ConflictException ex, WebRequest request) {
        return buildErrorResponse(ex, request, HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolationException(DataIntegrityViolationException ex,
            WebRequest request) {
        return buildErrorResponse(ex, request, HttpStatus.CONFLICT, "Database Conflict",
                "A database conflict occurred.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Error");
        body.put("message", "Validation failed for one or more fields");
        body.put("details", errors);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ BadCredentialsException.class })
    public ResponseEntity<Object> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        return buildErrorResponse(ex, request, HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid username or password");
    }

    @ExceptionHandler({ AccessDeniedException.class })
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        return buildErrorResponse(ex, request, HttpStatus.FORBIDDEN, "Forbidden",
                "You do not have permission to access this resource");
    }

    @ExceptionHandler({ ExpiredJwtException.class, JwtException.class })
    public ResponseEntity<Object> handleJwtException(JwtException ex, WebRequest request) {
        return buildErrorResponse(ex, request, HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid or expired token");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        return buildErrorResponse(ex, request, HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unhandled exception at {}: {}", request.getDescription(false), ex.getMessage(), ex);
        return buildErrorResponse(ex, request, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please contact support.");
    }
}
