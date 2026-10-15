package com.gyote.silvercare.ai_result.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    // 채팅방의 메시지를 생성 시각 오름차순으로 조회한다.
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
