package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByPatientIdOrderByTimestampAsc(Long patientId);
}
