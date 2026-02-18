package com.hms.HospitalManagementSystem.dto;

import com.hms.HospitalManagementSystem.entity.MessageType;
import com.hms.HospitalManagementSystem.entity.SenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private Long patientId;
    private String content;
    private SenderType sender;
    private MessageType type;
    private LocalDateTime timestamp;
}
