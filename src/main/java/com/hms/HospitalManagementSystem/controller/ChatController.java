package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.ChatMessageRequest;
import com.hms.HospitalManagementSystem.dto.ChatMessageResponse;
import com.hms.HospitalManagementSystem.entity.User;
import com.hms.HospitalManagementSystem.service.ChatService;
import com.hms.HospitalManagementSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    @PostMapping("/send")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ChatMessageResponse> sendMessage(@RequestBody ChatMessageRequest request) {
        User patient = getAuthenticatedPatient();
        ChatMessageResponse response = chatService.sendMessage(patient, request.getContent());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory() {
        User patient = getAuthenticatedPatient();
        List<ChatMessageResponse> history = chatService.getChatHistory(patient);
        return ResponseEntity.ok(history);
    }

    private User getAuthenticatedPatient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userService.getUserByUsername(username); // Assuming userService has this method. Check if not.
    }
}
