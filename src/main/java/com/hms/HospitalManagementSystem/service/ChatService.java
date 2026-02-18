package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.ChatMessageResponse;
import com.hms.HospitalManagementSystem.entity.User;

import java.util.List;

public interface ChatService {
    ChatMessageResponse sendMessage(User patient, String content);

    List<ChatMessageResponse> getChatHistory(User patient);
}
