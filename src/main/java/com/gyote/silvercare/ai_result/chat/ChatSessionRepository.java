package com.gyote.silvercare.ai_result.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    // 현재 사용자가 생성한 채팅방을 식별자로 조회한다.
    Optional<ChatSession> findByIdAndStartedByUserId(UUID id, UUID startedByUserId);

    // 질문 대상별 채팅방을 최근 생성 순으로 조회한다.
    List<ChatSession> findByPatientIdAndStartedByUserIdOrderByCreatedAtDesc(UUID patientId, UUID startedByUserId);
}
