package com.gyote.silvercare.ai_chat.query.model;

import com.gyote.silvercare.ai_chat.domain.entity.ChatSession;

import java.time.Instant;
import java.util.UUID;

/** API 계층에 의존하지 않는 AI 채팅방 조회 결과다. */
public record AiChatSessionView(UUID sessionId, UUID patientId, String title, Instant createdAt) {

    /** 채팅방 엔티티를 조회 결과로 변환한다. */
    public static AiChatSessionView from(ChatSession session) {
        return new AiChatSessionView(
                session.getId(), session.getPatientId(), session.getTitle(), session.getCreatedAt()
        );
    }
}
