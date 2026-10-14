package com.gyote.silvercare.ai_result.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    Optional<ChatSession> findByIdAndStartedByUserId(UUID id, UUID startedByUserId);
    List<ChatSession> findByPatientIdAndStartedByUserIdOrderByCreatedAtDesc(UUID patientId, UUID startedByUserId);
}
