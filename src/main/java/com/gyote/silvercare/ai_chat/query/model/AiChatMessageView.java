package com.gyote.silvercare.ai_chat.query.model;

import com.gyote.silvercare.ai_chat.domain.ChatRole;
import com.gyote.silvercare.ai_chat.domain.entity.ChatMessage;

import java.time.Instant;
import java.util.UUID;

/** API 계층에 의존하지 않는 AI 채팅 메시지 조회 결과다. */
public record AiChatMessageView(
        UUID messageId,
        ChatRole role,
        String content,
        Instant createdAt,
        UUID resultDocumentId
) {

    /** 채팅 메시지 엔티티를 조회 결과로 변환한다. */
    public static AiChatMessageView from(ChatMessage message) {
        return new AiChatMessageView(
                message.getId(), message.getRole(), message.getContent(),
                message.getCreatedAt(), message.getResultDocumentId()
        );
    }
}
